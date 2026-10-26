# Chest Tracker — 1.21.1 fork (bug fixes)

> ## ⚠️ This is an unofficial fork
>
> **Original mod:** [Chest Tracker](https://github.com/JackFred2/ChestTracker) by **[JackFred2](https://github.com/JackFred2)**
> **Original repository:** <https://github.com/JackFred2/ChestTracker>
> **Original license:** [LGPL-3.0-only](https://www.gnu.org/licenses/lgpl-3.0.html)
>
> All credit for Chest Tracker goes to the original author. This repository is a fork that contains **only the bug
> fixes described below** — no new features, no rebranding, no removal of attribution. The mod id, name, textures and
> codebase are unchanged apart from the fixes.
>
> This fork is **not affiliated with or endorsed by** the original author. Please do not report bugs from this fork
> upstream.

---

## 📌 What this fork changes

The upstream mod could report **the wrong chest location for an item**. Searching for an item would highlight a chest
that did not actually contain it, and the same contents would often appear at two adjacent blocks.

### Root cause

Chest Tracker records a memory when a container GUI **closes**, and it used to read the "last interacted block" at that
moment:

1. **Stale interaction source (primary cause).** `InteractionTrackerImpl` stored *every* main-hand `UseBlockCallback`
   in a single mutable field, and it was only cleared on screen close. Any block interaction while a container GUI was
   open — from Chest Tracker itself, or from other mods such as quick-stack / stack-to-nearby-chests / hopper previews —
   overwrote that field. The GUI's contents were then filed under whatever block was clicked last, not the block the
   GUI was actually opened from.
2. **Wrong root position for multi-block containers.** `ConnectedBlocksGrabber.getConnected()` returns a `HashSet`
   sorted by `BlockPos::asLong`, so `connected.get(0)` is the numerically lowest half of a double chest rather than the
   clicked half. Memories were re-keyed onto the adjacent block, producing duplicate entries.
3. **Leaked entries on removal.** `MemoryKeyImpl.remove()` did not clear the `connected` mapping, and did not remove
   sibling memories whose `otherPositions` contained the removed position. Stale duplicates therefore survived and kept
   being returned by search.

Measured against a real save: **33 of 40 stored memories did not match the actual block entity contents**, including 5
pairs of identical contents at adjacent positions and one memory pointing at `minecraft:air`.

### Fixes

| File | Change |
| --- | --- |
| `impl/providers/InteractionTrackerImpl.java` | Only record blocks that actually have a block entity; added `markConsumed()` / `clearConsumed()` so a source cannot outlive the GUI it was used for. |
| `impl/ChestTracker.java` | Pin the block source when a screen opens; clear it after the screen closes instead of leaving it around. |
| `api/providers/defaults/DefaultProvider.java` | `onScreenOpen` pins the block source; `onBlockPlaced` uses the corrected root position. |
| `impl/DefaultChestTrackerPlugin.java` | Prefer the source pinned at screen-open time; validate the GUI against the block via slot count; use the corrected root position and filtered `otherPositions`. |
| `impl/util/ContainerSizes.java` *(new)* | Vanilla container slot counts plus `isPlausible(state, actualSlots)`. Unknown (modded) blocks are always accepted, so mod compatibility is preserved. |
| `impl/memory/MemoryKeyImpl.java` | `add()` / `remove()` now purge stale `connected` links and sibling memories that reference a removed position. |
| `impl/util/Misc.java` | New `rootPosition(pos, connected)` helper. |
| `impl/compat/servers/hypixel/HypixelProvider.java` | Same root-position and pinned-source fixes for Hypixel Skyblock chests. |
| `impl/compat/mods/ExpandedStorageIntegration.java` | Now loads Expanded Storage reflectively, because that mod is no longer published to any Maven repository (see *Building* below). |

### ⚠️ Existing memories are not repaired

The fixes only prevent **new** memories from being written with the wrong position. Memories already stored with a
wrong position stay wrong. Clear the affected memory bank in-game (or delete the `.nbt` file under
`<game dir>/chesttracker/<singleplayer|multiplayer>/`) and re-open the containers to rebuild it.

---

## 🇨🇳 中文说明

**这是一个非官方分支。** 原模组 [Chest Tracker](https://github.com/JackFred2/ChestTracker) 由
[JackFred2](https://github.com/JackFred2) 开发，采用 **LGPL-3.0-only** 许可。模组本身的一切功劳归原作者所有，
本仓库只包含下述缺陷修复，未添加新功能、未改名、未移除任何署名，也与原作者无隶属或背书关系。

**修复的问题：** 搜索物品时经常指向错误的箱子位置，且相同内容会出现在相邻的两个方块上。

**根本原因：**

1. 记忆是在容器界面**关闭时**写入的，而旧代码此时才去读取「最后交互的方块」。这个字段是全局可变的，界面打开
   期间任何方块交互（包括快速堆叠、stack-to-nearby-chests、漏斗预览等模组）都会覆盖它，导致界面内容被记到
   错误的方块上。
2. 多格容器（如双箱）的根位置取自 `connected.get(0)`，而该集合按 `BlockPos::asLong` 排序，取到的是数值较小的
   那一半，于是记忆被反复建键到相邻方块，产生重复。
3. `MemoryKeyImpl.remove()` 未清理 `connected` 映射，也未删除 `otherPositions` 指向被删位置的兄弟记忆，脏数据
   因此长期残留并被搜索命中。

**实测：** 在真实存档中，40 条记忆里有 **33 条**与方块实体的实际内容不符，其中 5 对内容完全相同但坐标相邻，
还有 1 条指向 `minecraft:air`。

**注意：** 修复只阻止**新的**错误写入，已存在的错误记忆不会自动纠正。请在游戏内清空对应记忆库后重新开箱收录。

---

## 📥 Requirements

- [Fabric API](https://modrinth.com/mod/fabric-api)
- [YACL](https://modrinth.com/mod/yacl)

Chest Tracker embeds [Where Is It](https://modrinth.com/mod/where-is-it),
[JackFredLib](https://github.com/JackFred2/JackFredLib) and
[Searchables](https://github.com/jaredlll08/searchables).

## 📖 Usage

Press **Y** to search by an Item Stack; this uses Where Is It's keybind.

Press **GRAVE** `` ` `` to open the main GUI. In the GUI, click an item to search for it in your current dimension. Use
the search bar and its various filters to narrow down your search.

For more details on using the mod, see [the upstream wiki](https://github.com/JackFred2/ChestTracker/wiki).

## 🔨 Building

Requires **JDK 21**.

```bash
./gradlew build
```

The output jar is written to `build/libs/`.

> **Note on Expanded Storage:** upstream declared a compile-time dependency on `maven.modrinth:expanded-storage`, but
> that Modrinth project has been removed, so the artifact can no longer be downloaded and a clean checkout of upstream
> fails to configure. This fork loads the Expanded Storage integration reflectively instead, so the project builds
> without that dependency while the integration still works when the mod is installed.

## ✏️ License

This fork is distributed under the **same license as upstream: [LGPL-3.0-only](LICENSE)**.

- Original work © JackFred2 — <https://github.com/JackFred2/ChestTracker>
- Modifications in this fork © the fork's contributors, also licensed under LGPL-3.0-only.

Under the LGPL-3.0 you may use, modify and redistribute this mod, provided that you keep the original copyright
notices, state that changes were made, license derivative works under the LGPL-3.0, and make the corresponding source
available. The full license text is in [LICENSE](LICENSE).

You have permission to use the textures, modified or not, in your resource pack.

Legacy Chest Tracker 1.x is licensed under MIT (with some EPL rendering code) and is available on
[the legacy branch](https://github.com/JackFred2/ChestTracker/tree/legacy-1.x).
