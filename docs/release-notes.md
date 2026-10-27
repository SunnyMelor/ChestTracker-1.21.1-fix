# Release v2.6.7-vtfix

## 容器记忆归属错误修复（Minecraft 1.21.1 / Fabric）

修复「搜索物品时高亮的箱子并不包含该物品、相同内容出现在相邻两个方块上」的问题。

- **Minecraft**：1.21.1
- **加载器**：Fabric（Loader ≥ 0.15.0）
- **依赖**：Fabric API ≥ 0.103.0+1.21.1、YACL ≥ 3.2.0
- **环境**：纯客户端

### 文件

| 文件 | 说明 |
| --- | --- |
| `chesttracker-2.6.7+1.21.1-fork.jar` | 修复后的 Chest Tracker（Fabric） |

### 修复内容

1. **记忆时点前移** — 界面打开时固定方块来源，关闭时消费并清除；不再在关闭时读取全局可变的
   `lastSource`
2. **只记录可开箱的方块** — 工作台、铁砧等无方块实体的方块不再成为来源
3. **槽位数校验兜底** — 用界面非玩家槽位数校验其是否属于该方块，不通过则不写记忆；
   模组容器一律放行
4. **多格容器根位置修正** — 不再使用 `connected.get(0)`，改用玩家实际交互位置
5. **残链清理** — `MemoryKeyImpl.add/remove` 清理失效 `connected` 链路与兄弟记忆

### ⚠️ 升级须知

本次修复**不会**纠正已存在的错误记忆。升级后请先清空对应记忆库再重新开箱：

- 游戏内：记忆库管理 → 删除该服务器的记忆库
- 或直接删除 `chesttracker/multiplayer/<服务器>.nbt`（保留同名 `.meta` 可保留搜索配置）

### 实测效果

修复前对真实存档的统计：

| 指标 | 数值 |
| --- | --- |
| 记忆总数 | 40 |
| 与实际内容不符 | 33（82.5%） |
| 相邻重复对 | 5 |

修复后新写入的记忆与实际方块实体内容一致。

### 安装

1. 安装 Minecraft 1.21.1 与 Fabric Loader；
2. 将 `chesttracker-2.6.7+1.21.1-fork.jar` 放入 `mods/`；
3. 安装 Fabric API 与 YACL。

### 许可

基于 Chest Tracker（作者 JackFred2，LGPL-3.0-only）修改，以同一许可发布。
详见 [THIRD-PARTY-NOTICES.md](../THIRD-PARTY-NOTICES.md)。
