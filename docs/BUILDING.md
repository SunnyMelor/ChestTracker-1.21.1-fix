# 构建说明

本仓库的 `dist/` 目录存放已构建好、可直接安装的 jar。

## 重新构建

需要 **JDK 21**。

```bat
gradlew.bat build
```

产物路径：

```
build\libs\chesttracker-2.6.7+1.21.1.jar
```

## 当前发布物校验值

| 文件 | 大小 | SHA-256 |
| --- | --- | --- |
| `chesttracker-2.6.7+1.21.1-fork.jar` | 1,316,438 B | `C9E6219A87EDE0383C778408F601F0ACC348A65274731D6C97A20D59CCBB5891` |

## 依赖解析说明

上游的 `build.gradle.kts` 声明了 `maven.modrinth:expanded-storage` 作为编译期依赖，但该 Modrinth
项目已被移除（`https://api.modrinth.com/v2/project/expanded-storage` 返回 404），构件无法下载，
**干净检出在上游状态下会在配置阶段失败**。

本仓库的对应调整：

- 注释掉该依赖；
- `ExpandedStorageIntegration` 改为反射加载（见 [changes.md](./changes.md#19-implcompatmodsexpandedstorageintegrationjava)）。

因此本仓库**无需任何额外配置即可直接构建**，且装了 Expanded Storage 时集成仍然生效。

## 网络说明

若依赖下载失败，通常为网络问题。Gradle 支持通过 JVM 参数指定代理：

```bat
gradlew.bat -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 ^
            -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897 build
```

首次构建需要下载 Minecraft、映射与全部依赖，耗时较长（约 3–5 分钟），之后有缓存会快很多。

## 相对上游的构建配置调整

这些调整**仅影响发布/构建流程**，不改变模组运行逻辑：

1. 注释 `expanded-storage` 编译期依赖（项目已下线）
2. `project.grgit` → `project.extensions.findByType(Grgit::class.java)`（支持非 git 目录构建）
3. 删除 `.github/workflows/publish.yml`（防止误推原作者 Maven）

## CI

`.github/workflows/build.yml`（未修改）会在 push 与 PR 时于 `ubuntu-22.04` 与 `windows-2022`
上以 JDK 21 执行 `./gradlew check build`。

本仓库的首次提交已通过两平台验证。
