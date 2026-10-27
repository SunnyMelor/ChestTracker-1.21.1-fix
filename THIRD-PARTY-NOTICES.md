# 第三方版权与许可声明

本仓库为 Chest Tracker 的修改版。所有原始作品版权归其各自作者所有。

---

## Chest Tracker

- **作者**：JackFred2
- **仓库**：https://github.com/JackFred2/ChestTracker
- **下载**：https://modrinth.com/mod/chest-tracker
- **许可**：**LGPL-3.0-only**

上游仓库 `LICENSE` 文件为 GNU Lesser General Public License v3.0 全文。`fabric.mod.json` 中
`"license": "LGPL-3.0-only"`。Modrinth 项目页面许可字段亦为 `LGPL-3.0-only`。

本仓库的 `LICENSE` 文件**与上游完全一致，未做任何改动**。

### 关于本仓库的修改

本仓库对 Chest Tracker 的修改仅限于以下文件：

| 文件 | 修改内容 |
| --- | --- |
| `src/client/java/red/jackf/chesttracker/impl/providers/InteractionTrackerImpl.java` | 只记录带方块实体的方块；新增 `markConsumed()` / `clearConsumed()` |
| `src/client/java/red/jackf/chesttracker/impl/ChestTracker.java` | 界面打开时固定方块来源，关闭后清除 |
| `src/client/java/red/jackf/chesttracker/api/providers/defaults/DefaultProvider.java` | `onScreenOpen` 固定来源；`onBlockPlaced` 修正根位置 |
| `src/client/java/red/jackf/chesttracker/impl/DefaultChestTrackerPlugin.java` | 优先用固定的来源；槽位数校验；修正根位置与 `otherPositions` |
| `src/client/java/red/jackf/chesttracker/impl/util/ContainerSizes.java` | **新增文件**：原版容器槽位数与校验方法 |
| `src/client/java/red/jackf/chesttracker/impl/util/Misc.java` | 新增 `rootPosition()` 方法 |
| `src/client/java/red/jackf/chesttracker/impl/memory/MemoryKeyImpl.java` | `add()` / `remove()` 清理失效链路与兄弟记忆 |
| `src/client/java/red/jackf/chesttracker/impl/compat/servers/hypixel/HypixelProvider.java` | Hypixel 箱子修正根位置与来源固定 |
| `src/client/java/red/jackf/chesttracker/impl/compat/mods/ExpandedStorageIntegration.java` | 改为反射加载（原依赖已下线） |
| `src/client/resources/fabric.mod.json` | `contributors` 追加本分支说明；新增 `chesttracker:fork` 说明字段 |
| `build.gradle.kts` | 注释掉无法解析的 `expanded-storage` 依赖；`grgit` 容错查找 |
| `README.md` | 重写为说明本分支与原项目 |
| `THIRD-PARTY-NOTICES.md` | **新增文件**：第三方版权与许可声明 |
| `docs/` | **新增目录**：根因分析、逐文件修改说明、构建说明、发布说明 |
| `.github/workflows/publish.yml` | **已删除**：防止误推到原作者的 Maven 仓库 |

**未修改**的内容：模组 id、名称、所有贴图与资源、`authors` 字段（保持 `JackFred` 原样）、
`darkmode/`、`changelogs/`、`images/` 目录。

### LGPL-3.0 义务说明

依据 LGPL-3.0，分发本修改版时：

1. **保留原版权声明** — 原作者署名未被移除或改写；
2. **标明修改** — 见上表与 [docs/changes.md](./docs/changes.md)；
3. **以同一许可授权** — 本修改版同样以 LGPL-3.0-only 发布；
4. **提供对应源码** — 本仓库即为完整源码。

本仓库**不是** Chest Tracker 的官方发行版，与原作者无从属或背书关系。
若你是原作者并希望移除本仓库，请提 Issue，将立即处理。

---

## Where Is It

- **作者**：JackFred2
- **仓库**：https://github.com/JackFred2/WhereIsIt
- **下载**：https://modrinth.com/mod/where-is-it
- **许可**：**LGPL-3.0-only**

由上游通过 Jar-in-Jar 内嵌，本仓库**未做任何修改**。

---

## JackFredLib

- **作者**：JackFred2
- **仓库**：https://github.com/JackFred2/JackFredLib
- **许可**：**LGPL-3.0-only**

由上游通过 Jar-in-Jar 内嵌，本仓库**未做任何修改**。

---

## Searchables

- **作者**：Jaredlll08
- **仓库**：https://github.com/jaredlll08/searchables
- **许可**：**MIT**

由上游通过 Jar-in-Jar 内嵌（`bundle_searchables=true` 时），本仓库**未做任何修改**。

---

## Minecraft / Fabric

- Minecraft © Mojang Studios / Microsoft
- Fabric Loader / Fabric API © FabricMC

本仓库**不包含**上述任何专有资源；开发时仅通过 Loom 引用其公开构件。
