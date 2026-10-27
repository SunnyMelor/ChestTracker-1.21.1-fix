# 根因分析：搜索出的箱子位置与实际内容不符

## 摘要

| 项 | 内容 |
| --- | --- |
| 现象 | 搜索物品时高亮的箱子并不包含该物品；相同内容出现在相邻两个方块上；部分记忆指向 `minecraft:air` |
| 涉及版本 | Chest Tracker `2.6.7+1.21.1` |
| 平台 | Minecraft 1.21.1 / Fabric |
| 根因 | 记忆在写入时就被归属到**错误的方块**（交互来源是全局可变状态 + 多格容器根位置取错 + 删除时未清理残链） |
| 性质 | 写入路径缺陷，非渲染或搜索算法问题 |

---

## 1. 证据链

### 1.1 实测数据

解析真实存档中的记忆库与服务器区域文件，逐条比对「记忆内容」与「该坐标方块实体的实际内容」。

比对方法：对每个记忆位置，从对应的 `.mca` 区域文件中读取该区块的方块实体 `Items` 列表，与记忆中
`items` 的 id/count 集合计算 Jaccard 相似度，`≤ 0.85` 视为不匹配。

| 指标 | 数值 |
| --- | --- |
| 记忆总数（`minecraft:overworld`） | 40 |
| 与方块实体内容**不符** | **33**（82.5%） |
| 内容完全相同但坐标相邻的重复对 | 5 |
| 指向 `minecraft:air` 的记忆 | 1 |

重复对示例（内容逐格一致，坐标仅差 1 格）：

| 位置 A | 位置 B |
| --- | --- |
| `-1266, 64, 47` | `-1266, 65, 47` |
| `-1264, 63, 53` | `-1264, 63, 54` |

「相同内容 + 相邻坐标」是**同一个 GUI 被存到多个方块**的典型特征。

### 1.2 搜索路径

```
ChestTrackerWhereIsItPlugin
  → MemoryBankImpl.doSearch
    → MemoryKeyImpl.doSearch
       过滤条件：distToCenterSqr(searchRange) + SearchRequest.check(memory.items())
```

`otherPositions` **不参与搜索**。因此一旦记忆的 key（主坐标）记错，搜索结果就必然指向错误的方块——
这与用户现象完全一致。

### 1.3 写入路径（修复前）

```
ScreenEvents.BEFORE_INIT  → provider.onScreenOpen()          // 此时未记录来源
ScreenEvents.remove       → provider.onScreenClose()
                            → DefaultChestTrackerPlugin.defaultMemoryCreator()
                                → InteractionTracker.INSTANCE.getLastBlockSource()   // ← 关闭时才读
                                → connectedBlocks.get(0)                             // ← 根位置取错
```

---

## 2. 三个叠加缺陷

### 缺陷一：交互来源是全局可变状态（主因）

`InteractionTrackerImpl` 持有单个 `lastSource` 字段：

```java
UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
    if (hand == InteractionHand.MAIN_HAND) {
        INSTANCE.setLastBlockSource(new CachedClientBlockSource(level, hitResult.getBlockPos()));
    }
    return InteractionResult.PASS;
});
```

该字段**只在界面关闭时清除**，且清除条件还受 `ScreenBlacklist` 影响。于是在界面打开期间：

| 场景 | 后果 |
| --- | --- |
| Chest Tracker 自身 `containerindexer` 的 `UseBlockCallback` | 来源被自身覆盖 |
| stack-to-nearby-chests / 快速堆叠类模组 | 来源被邻居箱子覆盖 |
| 漏斗 / 潜影盒预览类模组 | 来源被预览方块覆盖 |
| 点击工作台、铁砧等原版工作站 | 来源变成工作站（无方块实体） |
| 服务端主动下发 GUI（无方块交互） | 沿用上一次的陈旧来源 |

界面关闭时读到的是**最后一次点击的方块**，而非 GUI 真正的来源。

### 缺陷二：多格容器根位置取错

`ConnectedBlocksGrabber.getConnected()` 内部：

```java
Set<BlockPos> positions = new HashSet<>();
// ... 收集连接方块（双箱的另一半等）
return positions.stream().sorted(Comparator.comparingLong(BlockPos::asLong)).toList();
```

返回列表**按 `BlockPos::asLong` 数值排序**。双箱的另一半可能数值更小，于是 `connected.get(0)`
取到的是**坐标数值较小的那一半**，而不是玩家点击的那一半。

结果：同一个 GUI 在不同时刻被建键到双箱的不同半边，形成两个内容相同、坐标相邻的记忆。

### 缺陷三：删除时未清理残链

`MemoryKeyImpl` 维护 `memories`（主坐标 → 记忆）与 `connected`（附属坐标 → 主坐标）两张表。

修复前的 `remove(BlockPos)`：

```java
public boolean remove(BlockPos position) {
    BlockPos rootPosition = this.connected.getOrDefault(position, position);
    boolean success = this.memories.remove(rootPosition) != null;
    this.namedMemories.remove(rootPosition);
    while (this.connected.values().remove(rootPosition));
    // ← 未删除 this.connected.remove(rootPosition)
    // ← 未删除 otherPositions 包含 position 的兄弟记忆
    this.overrides.remove(position);
    return success;
}
```

两个后果：

1. `connected` 中指向被删位置的条目残留，后续 `get(pos)` 仍能命中一个不存在的记忆；
2. 把该位置当作 `otherPositions` 一员的兄弟记忆**存活下来**，继续被搜索返回。

---

## 3. 修复策略

核心原则：**记忆必须归属到界面真正打开的那个方块**，并且这个归属关系在界面打开瞬间就固定下来。

### 3.1 时点前移：打开时固定，关闭时消费

```
ScreenEvents.BEFORE_INIT  → provider.onScreenOpen()
                             └─ context.setBlockSource(lastBlockSource)   // ← 固定
                             → InteractionTrackerImpl.markConsumed()
ScreenEvents.remove       → provider.onScreenClose()
                             → defaultMemoryCreator() 用 context.getBlockSource()
                             → InteractionTrackerImpl.clearConsumed()      // ← 清除
```

`markConsumed()` / `clearConsumed()` 通过 `pendingClearPos` 记录「被哪个界面消费过」，
使得来源**不会存活超过它所属的界面**，同时不影响「重命名界面往返」这类需要保留来源的场景。

### 3.2 只记录可能开箱的方块

```java
if (clientLevel.getBlockState(pos).hasBlockEntity())
    INSTANCE.setLastBlockSource(new CachedClientBlockSource(clientLevel, pos));
else
    INSTANCE.clear();
```

工作台、铁砧等没有方块实体的方块不再成为来源，避免后续 GUI 被归到它们名下。

### 3.3 槽位数校验作为兜底

`ContainerSizes.isPlausible(state, actualSlots)` 比对「界面中非玩家槽位数」与「该方块应有的槽位数」：

| 方块 | 槽位数 |
| --- | --- |
| 箱子 | 27 或 54 |
| 木桶 / 潜影盒 | 27 |
| 漏斗 / 酿造台 | 5 |
| 发射器 / 合成器 | 9 |
| 熔炉类 | 3 |

**未知方块（模组容器）一律返回 `true`**，保证模组兼容性不被破坏。

箱子同时接受 27 与 54，因为双箱的另一半可能在未加载区块中，两者无法区分。

校验不通过时**不写入记忆**（安全降级），而不是写到错误的方块上。

### 3.4 根位置修正

```java
public static BlockPos rootPosition(BlockPos pos, List<BlockPos> connected) {
    return connected.contains(pos) ? pos : connected.get(0);
}
```

优先使用**玩家实际交互的位置**，仅在它不在连接集合中时才回退到排序后的首元素。

### 3.5 残链清理

`add()` 与 `remove()` 现在都会：

- 删除 `connected` 中指向被覆盖 / 被删除位置的所有条目；
- 删除 `otherPositions` 包含被删除位置的**兄弟记忆**（通过 `List.copyOf(...)` 快照迭代，避免
  `ConcurrentModificationException`）。

---

## 4. 影响范围

| 场景 | 修复前 | 修复后 |
| --- | --- | --- |
| 普通单箱 | 通常正确 | 正确 |
| 双箱 | 常在两半之间重复 | 固定为点击的那一半 |
| 界面打开期间操作其他方块 | 内容归到错的方块 | 归到真正的来源方块 |
| 服务端主动下发的 GUI | 沿用陈旧来源 | 校验不通过则不记录 |
| 模组容器 | 可能错位 | 校验放行，仍依赖来源正确性 |

> 已知局限：槽位数校验是**兜底**而非完整解。若服务端在完全没有方块交互的情况下打开一个模组容器，
> 且来源恰好看不出矛盾，仍可能误记。这类情况无法在客户端完全消除。
