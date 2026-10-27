# 逐文件修改说明

本文档列出本仓库相对上游 `JackFred2/ChestTracker` `2.6.7+1.21.1` 的全部改动。
按「功能修复」与「构建配置」两类分组。

---

## 一、功能修复

### 1.1 `impl/providers/InteractionTrackerImpl.java`

**问题**：`lastSource` 是全局可变状态，只在界面关闭时清除，界面打开期间任何方块交互都会覆盖它。

**修改**：

- `UseBlockCallback` 仅在 `clientLevel.getBlockState(pos).hasBlockEntity()` 时记录来源，
  否则调用 `INSTANCE.clear()`。工作台、铁砧等不会成为来源。
- 新增字段 `pendingClearPos`，记录「被哪个界面消费过」。
- 新增 `markConsumed()`：在界面打开、来源已被固定到该界面后调用。
- 新增 `clearConsumed()`：在界面关闭后调用，只有当前来源仍是被消费的那个时才清除，
  避免误删「重命名界面往返」时需要保留的来源。
- `clear()` 同时重置 `lastSource` 与 `pendingClearPos`。

### 1.2 `impl/ChestTracker.java`

**问题**：界面生命周期没有把来源绑定到界面。

**修改**：

- `ScreenEvents.BEFORE_INIT` 中，`provider.onScreenOpen(openContext)` 之后调用
  `InteractionTrackerImpl.INSTANCE.markConsumed()`。
- `ScreenEvents.remove` 中，把原来的无条件 `clear()` 换成 `clearConsumed()`，且仍在
  `!shouldSkipProviderForNextGuiClose` 分支内，保证重命名往返不受影响。

### 1.3 `api/providers/defaults/DefaultProvider.java`

**修改**：

- `onScreenOpen`：拿到 `lastBlockSource` 后**同时**设置 `memoryLocation` 与 `blockSource`
  （`context.setBlockSource(cbs)`），把来源固定到本次界面。
- `onBlockPlaced`：`connected.get(0)` → `Misc.rootPosition(pos, connected)`，
  `otherPositions` 过滤掉根位置。

### 1.4 `impl/DefaultChestTrackerPlugin.java`

**问题**：`defaultMemoryCreator` 在关闭时读 `InteractionTracker.getLastBlockSource()`，且读了两次。

**修改**：

- 改为 `context.getBlockSource().or(() -> InteractionTracker.INSTANCE.getLastBlockSource())`，
  优先使用打开界面时固定的来源。
- 新增 `ContainerSizes.isPlausible(cbs.get().blockState(), context.getContainerSlotCount())` 校验，
  不通过则 `ResultHolder.pass()`（不写记忆）。
- 根位置改用 `Misc.rootPosition(...)`，`otherPositions` 过滤掉根位置。
- `enderChestMemoryCreator` 同样改为优先读 `context.getBlockSource()`。

### 1.5 `impl/util/ContainerSizes.java`（新增）

原版容器槽位数常量与 `isPlausible(BlockState, int)` 校验方法。

设计取舍：

- 未知方块（模组容器）**一律返回 `true`**，不破坏模组兼容性；
- `ChestBlock` 同时接受 27 与 54，因为双箱另一半可能未加载。

### 1.6 `impl/util/Misc.java`

新增：

```java
public static BlockPos rootPosition(BlockPos pos, List<BlockPos> connected) {
    return connected.contains(pos) ? pos : connected.get(0);
}
```

### 1.7 `impl/memory/MemoryKeyImpl.java`

**`add(BlockPos, Memory)`**：

- 在写入新记忆前，先移除目标位置在 `connected` 中的陈旧映射，并连带删除它指向的孤立根记忆，
  防止「已有记忆仍链向此位置」形成重复。

**`remove(BlockPos)`**：

- 新增 `this.connected.remove(rootPosition)`；
- 新增遍历 `List.copyOf(this.memories.entrySet())`，删除任何 `otherPositions` 包含 `position`
  的兄弟记忆，并清理其 `connected` 条目。

### 1.8 `impl/compat/servers/hypixel/HypixelProvider.java`

- `onScreenOpen`：同样调用 `context.setBlockSource(cbs)`。
- `onScreenClose`：优先读 `context.getBlockSource()`。
- Skyblock 箱子：`connected.get(0)` → `Misc.rootPosition(...)`，`otherPositions` 过滤根位置。

### 1.9 `impl/compat/mods/ExpandedStorageIntegration.java`

**问题**：直接 `import compasses.expandedstorage.*`，形成编译期硬依赖；该 Modrinth 项目已下线，
依赖无法解析，导致**干净检出无法构建**。

**修改**：改为反射加载。

- `Class.forName("compasses.expandedstorage.api.ExpandedStorageAccessors")` 后
  `getMethod("getAttachedChestDirection", BlockState.class)`；
- `Class.forName("compasses.expandedstorage.impl.client.gui.AbstractScreen")` 用于
  `GetCustomName` 的 `instanceof` 判断；
- 反射失败时静默降级（`Optional.empty()` / `ResultHolder.pass()`），不影响主流程。

功能等价：装了 Expanded Storage 依然生效，没装也能正常编译运行。

### 1.10 `src/client/resources/fabric.mod.json`

- `contributors` 追加 `"SunnyMelor - 1.21.1 fork: container memory mis-attribution fixes"`。
- 新增 `custom["chesttracker:fork"]` 说明字段，标注来源与许可。
- **未改动**：`authors`（保持 `JackFred`）、`license`（`LGPL-3.0-only`）、`contact`（指向原仓库）、
  `id`、`name`、`entrypoints`、`depends`。

---

## 二、构建配置

### 2.1 `build.gradle.kts`

1. **`grgit` 容错查找**

   ```kotlin
   // 修改前
   val grgit: Grgit? = project.grgit

   // 修改后
   val grgit: Grgit? = project.extensions.findByType(Grgit::class.java)
   ```

   原因：grgit 插件仅在项目位于 git 仓库内时才注册扩展，从源码 zip 解压后构建会报
   `Unresolved reference: grgit`。

2. **注释 `expanded-storage` 依赖**

   ```kotlin
   //modCompileOnly("maven.modrinth:expanded-storage:${properties["expandedstorage_version"]}")
   ```

   原因：该 Modrinth 项目已下线（`/v2/project/expanded-storage` 返回 404），maven 构件
   无法下载，构建在配置阶段即失败。集成代码已改为反射（见 1.9）。

### 2.2 `.github/workflows/publish.yml`

**已删除**。

原因：该工作流会向原作者的 JackFred Maven 仓库、Modrinth 与 CurseForge 发布，
在本分支中属于误用。构建验证由 `build.yml` 承担（未修改）。

### 2.3 `README.md` / `THIRD-PARTY-NOTICES.md` / `docs/`

新增或重写，用于说明本分支来源、修改内容与许可义务。

---

## 三、未修改的内容

| 类别 | 说明 |
| --- | --- |
| 模组元数据 | `id`、`name`、`authors`、`license`、`contact`、`entrypoints`、`depends` |
| 资源文件 | 所有贴图、语言文件、`mixins.json`、`accesswidener` |
| 附属目录 | `darkmode/`、`changelogs/`、`images/` |
| 内嵌依赖 | Where Is It、JackFredLib、Searchables（Jar-in-Jar，未改动） |
| 其他工作流 | `build.yml` |
