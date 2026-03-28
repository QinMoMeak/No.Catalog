# NoCatalog 工作日志

本文档使用 UTF-8 编码维护，按阶段记录开发动作、结论与下一步。

## 2026-03-28

### 已完成

- 仔细阅读 [android_collection_app_codex_spec.md](/E:/Android/Project_/No.Catalog/docs/android_collection_app_codex_spec.md)，确认 V1 范围、技术选型和推荐实施顺序。
- 确认仓库当前为空白起步状态，适合从 Android 工程骨架开始落地。
- 建立 `implementation_plan.md`，将工作拆为 5 个阶段。
- 初始化 Android 工程基础目录。
- 初始化 Gradle Wrapper、版本目录、`app` 模块和 UTF-8 构建参数。
- 完成 Compose + Hilt + Room + DataStore + Navigation 的基础构建链路。
- 完成 `core / domain / data / presentation / di` 第一批骨架代码。
- 完成首页、编辑页、锁屏页、导入预览页、详情页、设置页、备份页的导航与页面骨架。
- 完成 CSV 读写器、JSON 备份序列化器、WebDAV Client 骨架、密码摘要与解锁状态管理骨架。
- 补充本机 `local.properties` 指向现有 Android SDK。
- 执行 `.\gradlew.bat :app:assembleDebug --no-daemon`，确认当前工程可成功构建。
- 打通本地 CRUD 主链路：
  - 首页可搜索、切换卡片/表格视图、切换排序
  - 新增页支持真实写库
  - 编辑页支持按 `entryId` 回填并更新
  - 详情页支持真实查询、星标切换、已看切换、删除
- 完成设置页第一版：
  - 可切换默认首页视图
  - 可切换默认排序
  - 可设置和关闭密码锁
- 再次执行 `.\gradlew.bat :app:assembleDebug --no-daemon`，确认增强后仍可成功构建。
- 生成可安装 debug 包：
  - `app/build/outputs/apk/debug/app-debug.apk`
- 完成导入导出实装：
  - 支持导出 CSV 与 JSON
  - 支持导入 CSV 与 JSON
  - 支持导入预览、逐行切换导入动作、确认写库
- 完成 WebDAV 备份恢复实装：
  - 支持保存 WebDAV 配置
  - 支持测试连接、立即备份、远端备份列表
  - 支持从远端备份执行“仅导入新增”或“全量恢复”
- 完成锁屏会话增强：
  - 支持后台超时后重新锁定
  - 导航层会根据会话锁状态重新进入锁屏页
- 为首页补充基础筛选：
  - 状态筛选
  - 星标筛选
  - 已看筛选
- 最终再次执行 `.\gradlew.bat :app:assembleDebug --no-daemon`，确认完整功能版 debug 包构建通过。

### 进行中

- 当前 V1 主要功能已落地，后续属于体验优化和更深入测试阶段。

### 下一步

- 增加单元测试、集成测试和 Compose UI 测试。
- 优化 WebDAV PROPFIND 兼容性与异常提示。
- 继续打磨导入冲突的逐条编辑与批量确认体验。
- 继续保持所有新增与更新文件使用 UTF-8 编码。
