# ChestTracker-1.21.1-fix

**Chest Tracker 记忆归属错误的修复版（Minecraft 1.21.1 / Fabric）**

> ⚠️ 本仓库为**非官方修改版**，与原作者无从属或背书关系。
> 上游 `Chest Tracker` 采用 **LGPL-3.0-only**，版权归原作者 **JackFred2** 所有；本仓库按其许可条款发布修改版，
> 保留原版权声明、标明修改内容，并以同一许可授权。详情见 [THIRD-PARTY-NOTICES.md](./THIRD-PARTY-NOTICES.md)。

---

## 1. 上游项目

| 项目 | 上游仓库 | 作者 | 许可 |
| --- | --- | --- | --- |
| Chest Tracker | https://github.com/JackFred2/ChestTracker | JackFred2 | LGPL-3.0-only |
| Where Is It（内嵌） | https://github.com/JackFred2/WhereIsIt | JackFred2 | LGPL-3.0-only |
| JackFredLib（内嵌） | https://github.com/JackFred2/JackFredLib | JackFred2 | LGPL-3.0-only |

本修复基于：

- `JackFred2/ChestTracker` 分支 **`1.21`**，版本 `2.6.7+1.21.1`

下载页：https://modrinth.com/mod/chest-tracker

---

## 2. 修复内容

### 2.1 问题现象

1. 搜索某个物品时，高亮的箱子**实际并不包含该物品**；
2. 相同内容经常同时出现在**相邻的两个方块**上；
3. 部分记忆甚至指向 `minecraft:air`（空的方块位置）；
4. 越常开箱、搜索结果越不可信。

### 2.2 实测证据

解析真实存档的记忆库与服务器区域文件，逐条比对记忆内容与方块实体实际内容：

| 指标 | 数值 |
| --- | --- |
| 记忆总数（`minecraft:overworld`） | 40 |
| 与方块实体内容**不符** | **33**（82.5%） |
| 内容完全相同但坐标相邻的重复对 | 5 |
| 指向 `minecraft:air` 的记忆 | 1 |

典型重复对（内容完全一致，坐标仅差 1 格）：

| 位置 A | 位置 B |
| --- | --- |
| `-1266, 64, 47` | `-1266, 65, 47` |
| `-1264, 63, 53` | `-1264, 63, 54` |

### 2.3 根因

Chest Tracker 在**容器界面关闭时**写入记忆，而旧代码此时才去读取「最后交互的方块」。三个缺陷叠加：

#### 缺陷一：交互来源是全局可变状态（主因）

`InteractionTrackerImpl` 把**每一次**主手 `UseBlockCallback` 都写进同一个字段，且只在界面关闭时清除。
只要界面打开期间发生任何方块交互，来源就被覆盖——包括：

- Chest Tracker 自身的 `containerindexer` 调用
- 快速堆叠类模组（stack-to-nearby-chests）
- 漏斗 / 潜影盒预览类模组
- 工作台、铁砧等原版工作站交互

于是界面内容被记到最后点击的那个方块上，而非界面真正的来源方块。

#### 缺陷二：多格容器根位置取错

`ConnectedBlocksGrabber.getConnected()` 返回的 `HashSet` 在转 `List` 时**按 `BlockPos::asLong` 排序**，
因此 `connected.get(0)` 取到的是数值较小的那一半，**不是玩家点击的那一半**。
双箱于是在两半之间反复建键，产生重复记忆。

#### 缺陷三：删除时未清理残链

`MemoryKeyImpl.remove()`：

- 未删除 `connected` 映射中指向该位置的条目；
- 未删除 `otherPositions` 包含该位置的兄弟记忆。

残留的脏数据不会被搜索过滤掉，于是持续被命中。

### 2.4 修复方式

| 文件 | 修改 |
| --- | --- |
| `impl/providers/InteractionTrackerImpl.java` | 只记录**带方块实体**的方块；新增 `markConsumed()` / `clearConsumed()`，来源不能存活超过它所属的界面 |
| `impl/ChestTracker.java` | 界面打开时**固定**方块来源；界面关闭后清除，不再遗留 |
| `api/providers/defaults/DefaultProvider.java` | `onScreenOpen` 固定来源；`onBlockPlaced` 使用修正后的根位置 |
| `impl/DefaultChestTrackerPlugin.java` | 优先使用界面打开时固定的来源；用容器槽位数校验界面是否属于该方块；使用修正后的根位置与过滤后的 `otherPositions` |
| `impl/util/ContainerSizes.java`（新增） | 原版容器槽位数与 `isPlausible()` 校验。**未知（模组）方块一律放行**，保证模组兼容性 |
| `impl/memory/MemoryKeyImpl.java` | `add()` / `remove()` 清理失效的 `connected` 链路与指向被删位置的兄弟记忆 |
| `impl/util/Misc.java` | 新增 `rootPosition(pos, connected)` |
| `impl/compat/servers/hypixel/HypixelProvider.java` | Hypixel Skyblock 箱子同样修正根位置与来源固定 |

核心思路：**记忆必须归属到界面真正打开的那个方块**，而不是「最后一次点击的方块」。

### 2.5 ⚠️ 已存在的错误记忆不会自动修复

本次修改只阻止**新的**错误写入。已存成错误坐标的记忆仍然是错的。

处理方式：在游戏内清空对应记忆库，或删除游戏目录下
`chesttracker/<singleplayer|multiplayer>/<服务器>.nbt`（保留同名 `.meta` 可保留搜索配置），然后重新开箱收录。

---

## 3. 已验证

在隔离的 Fabric 客户端环境做对照验证：

| 项目 | 结果 |
| --- | --- |
| 干净检出 `./gradlew build` | ✅ 通过（无任何本地补丁） |
| GitHub Actions `build`（ubuntu-22.04, JDK 21） | ✅ 通过 |
| GitHub Actions `build`（windows-2022, JDK 21） | ✅ 通过 |

运行时环境：

- Minecraft **1.21.1**
- Fabric Loader **0.15.0+**
- Fabric API **0.103.0+1.21.1**
- YACL **3.2.0+**
- 内嵌 Where Is It **2.6.3+1.21.1**

---

## 4. 仓库结构

```
ChestTracker-1.21.1-fix/
├── src/                          # 修复后的源码（Fabric，client 为主）
├── docs/
│   ├── root-cause.md             # 根因分析（含实测数据与代码路径）
│   ├── changes.md                # 逐文件修改说明
│   └── BUILDING.md               # 构建说明与产物校验值
├── dist/                         # 构建产物（已随仓库提供）
│   └── chesttracker-2.6.7+1.21.1-fork.jar
├── darkmode/                     # 上游附带的暗色资源包（未修改）
├── changelogs/                   # 上游变更日志（未修改）
├── images/                       # 上游截图（未修改）
├── LICENSE                       # LGPL-3.0 全文（与上游一致，未改动）
├── THIRD-PARTY-NOTICES.md        # 第三方版权与许可声明
└── README.md
```

---

## 5. 构建方式

需要 **JDK 21**。

```bat
gradlew.bat build
```

产物：

```
build\libs\chesttracker-2.6.7+1.21.1.jar
```

> **构建配置说明**：本仓库相对上游做了以下**仅影响发布/构建流程**的调整，不改变任何模组运行逻辑：
> - `expanded-storage` 的编译期依赖改为**注释**，集成代码改用反射加载（该 Modrinth 项目已下线，原依赖无法解析）
> - `project.grgit` 改为容错查找，使得在非 git 目录（如源码 zip）下也能构建
> - 删除上游的 `publish.yml`，避免误推到原作者的 Maven 仓库
>
> 详见 [docs/BUILDING.md](./docs/BUILDING.md)。

---

## 6. 安装

1. 安装 Minecraft 1.21.1 与 Fabric Loader；
2. 将 `dist/chesttracker-2.6.7+1.21.1-fork.jar` 放入 `mods/`；
3. 必须安装 [Fabric API](https://modrinth.com/mod/fabric-api) 与 [YACL](https://modrinth.com/mod/yacl)；
4. 本模组为**纯客户端**模组，服务端无需安装；
5. 首次使用建议先清空旧记忆库（见 2.5）。

---

## 7. 使用方法

- 按 **Y** 搜索物品（使用 Where Is It 的按键绑定）
- 按 **`**（GRAVE）打开主界面，点击物品搜索当前维度内的位置
- 详细用法见[上游 Wiki](https://github.com/JackFred2/ChestTracker/wiki)

---

## 8. 关于本修复的来源

本修复由 **DeepSeek** 辅助完成（根因定位、代码修改、构建与验证）。

---

## 9. 许可证与免责

- **Chest Tracker** — **LGPL-3.0-only**，版权归 **JackFred2**。本仓库为其修改版，按同一许可发布：
  保留原版权声明、标明修改内容、以 LGPL-3.0 授权衍生作品、并提供完整源码。
- **Where Is It / JackFredLib / Searchables** — 由上游内嵌（Jar-in-Jar），本仓库未做修改。
- **Minecraft / Fabric** — 版权归 Mojang Studios 与 FabricMC，本仓库不包含任何专有资源。

完整声明与许可证全文见 [THIRD-PARTY-NOTICES.md](./THIRD-PARTY-NOTICES.md) 与 [LICENSE](./LICENSE)。

若你是上游作者并希望移除本仓库，请提 Issue，将立即处理。
