# 安卓收藏管理 App 实施文档（可直接交给 Codex）

## 1. 项目目标

开发一个 Android App，用于快捷录入、查看、搜索和管理个人收藏记录。应用定位为“接近 Excel 的本地结构化管理工具”，但交互上更适合手机使用。

### 已确认需求
- 数据规模：几千条
- 使用场景：多设备使用，但当前阶段 WebDAV 仅做手动备份/恢复，不做自动双向同步
- 核心字段：番号、标题、多个演员、评分（5 分制）
- 扩展字段：标签、备注、收藏时间、发布日期、是否已看、是否星标、封面图片、来源链接
- CSV：支持导出，也支持手工修改后再导回 App
- 导入策略：导入前必须人工确认
- 数据交换格式：CSV 和 JSON 都支持
- WebDAV：坚果云
- 视图：表格视图 + 卡片列表视图都要
- 录入方式：偏单条详细录入
- 隐私：需要密码锁
- 后续网站目标：只是把数据搬上去，不要求安卓与网站实时共用数据库

---

## 2. 产品边界

### V1 范围（本次实现）
- 本地数据库管理收藏记录
- 卡片列表视图
- 表格视图
- 单条详细录入/编辑
- 搜索、排序、筛选
- CSV 导入导出
- JSON 导入导出
- WebDAV 手动备份到远端
- WebDAV 从远端恢复
- 本地密码锁

### 明确不做
- 不做 WebDAV 双向同步
- 不做自动定时同步
- 不做网站端 API 对接
- 不做云端用户系统
- 不做复杂图片抓取

---

## 3. 技术选型

- 语言：Kotlin
- UI：Jetpack Compose
- 架构：Clean Architecture（轻量） + MVVM
- 本地数据库：Room
- 设置存储：DataStore
- 序列化：kotlinx.serialization
- 并发：Kotlin Coroutines + Flow
- 文件导入导出：Storage Access Framework
- 密码锁：Android Keystore + DataStore
- WebDAV：OkHttp + 自定义 WebDAV Client
- 图片：Coil
- DI：Hilt
- 日志：Timber
- 单元测试：JUnit
- UI 测试：Compose UI Test

---

## 4. 推荐项目结构

```text
app/
  src/main/java/com/example/collectionapp/
    App.kt
    MainActivity.kt

    core/
      common/
        Result.kt
        AppDispatchers.kt
        Constants.kt
        Extensions.kt
      security/
        PasswordManager.kt
        CryptoManager.kt
        AppLockManager.kt
      csv/
        CsvReader.kt
        CsvWriter.kt
        CsvSchema.kt
      json/
        JsonBackupSerializer.kt
      webdav/
        WebDavClient.kt
        WebDavModels.kt
      util/
        DateTimeUtil.kt
        FileUtil.kt
        HashUtil.kt

    data/
      local/
        db/
          AppDatabase.kt
          Converters.kt
        entity/
          EntryEntity.kt
          PerformerEntity.kt
          EntryPerformerCrossRef.kt
          TagEntity.kt
          EntryTagCrossRef.kt
        dao/
          EntryDao.kt
          PerformerDao.kt
          TagDao.kt
          EntryRelationDao.kt
      remote/
        webdav/
          WebDavRemoteDataSource.kt
      repository/
        EntryRepositoryImpl.kt
        ImportExportRepositoryImpl.kt
        BackupRepositoryImpl.kt
        SettingsRepositoryImpl.kt
        SecurityRepositoryImpl.kt
      mapper/
        EntryMapper.kt
        BackupMapper.kt

    domain/
      model/
        Entry.kt
        Performer.kt
        Tag.kt
        EntryFilter.kt
        EntrySort.kt
        BackupManifest.kt
        ImportPreview.kt
      repository/
        EntryRepository.kt
        ImportExportRepository.kt
        BackupRepository.kt
        SettingsRepository.kt
        SecurityRepository.kt
      usecase/
        entry/
          AddEntryUseCase.kt
          UpdateEntryUseCase.kt
          DeleteEntryUseCase.kt
          GetEntryDetailUseCase.kt
          SearchEntriesUseCase.kt
          FilterEntriesUseCase.kt
          BatchDeleteUseCase.kt
        importexport/
          ExportCsvUseCase.kt
          ExportJsonUseCase.kt
          ParseCsvUseCase.kt
          PreviewImportUseCase.kt
          ConfirmImportUseCase.kt
        backup/
          BackupToWebDavUseCase.kt
          RestoreFromWebDavUseCase.kt
          ListRemoteBackupsUseCase.kt
        security/
          SetPasswordUseCase.kt
          VerifyPasswordUseCase.kt
          IsAppLockEnabledUseCase.kt

    presentation/
      navigation/
        AppNavGraph.kt
        Routes.kt
      ui/
        lock/
        home/
        detail/
        edit/
        search/
        importexport/
        backup/
        settings/
      component/
        EntryCard.kt
        EntryTableRow.kt
        SearchBar.kt
        RatingBar.kt
        TagChip.kt
      state/
        UiState.kt

    di/
      DatabaseModule.kt
      RepositoryModule.kt
      UseCaseModule.kt
      NetworkModule.kt
      SecurityModule.kt
```

---

## 5. 架构分层说明

### Presentation 层
职责：页面展示、用户输入、状态管理。

每个页面使用：
- `ViewModel`
- `UiState`
- `UiEvent`
- `UiEffect`（可选）

### Domain 层
职责：核心业务规则。

不要依赖 Android SDK，只依赖抽象接口。

### Data 层
职责：
- Room 数据访问
- CSV/JSON 文件读写
- WebDAV 上传下载
- Repository 实现

---

## 6. 数据模型设计

由于一条记录可能有多个演员、多标签，因此采用规范化表结构。

### 6.1 entries 表

```sql
CREATE TABLE entries (
  id TEXT PRIMARY KEY NOT NULL,
  code TEXT NOT NULL,
  title TEXT NOT NULL,
  rating REAL NOT NULL DEFAULT 0,
  notes TEXT,
  status TEXT NOT NULL DEFAULT 'COLLECTED',
  favorite INTEGER NOT NULL DEFAULT 0,
  watched INTEGER NOT NULL DEFAULT 0,
  release_date TEXT,
  collected_at TEXT NOT NULL,
  source_url TEXT,
  cover_local_path TEXT,
  cover_remote_url TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  deleted_at TEXT
);
```

说明：
- `id`：UUID
- `code`：番号，业务上应尽量唯一，但不强制作为主键
- `rating`：0.0 ~ 5.0，支持 0.5 步长
- `status`：如 `WISH`, `COLLECTED`, `WATCHED`, `ARCHIVED`
- `deleted_at`：预留软删除能力，便于未来恢复和导出一致性

### 6.2 performers 表

```sql
CREATE TABLE performers (
  id TEXT PRIMARY KEY NOT NULL,
  name TEXT NOT NULL,
  normalized_name TEXT NOT NULL,
  created_at TEXT NOT NULL,
  UNIQUE(normalized_name)
);
```

说明：
- `normalized_name`：用于去重搜索，比如统一小写、去空格

### 6.3 entry_performer_ref 表

```sql
CREATE TABLE entry_performer_ref (
  entry_id TEXT NOT NULL,
  performer_id TEXT NOT NULL,
  PRIMARY KEY (entry_id, performer_id),
  FOREIGN KEY(entry_id) REFERENCES entries(id) ON DELETE CASCADE,
  FOREIGN KEY(performer_id) REFERENCES performers(id) ON DELETE CASCADE
);
```

### 6.4 tags 表

```sql
CREATE TABLE tags (
  id TEXT PRIMARY KEY NOT NULL,
  name TEXT NOT NULL,
  normalized_name TEXT NOT NULL,
  created_at TEXT NOT NULL,
  UNIQUE(normalized_name)
);
```

### 6.5 entry_tag_ref 表

```sql
CREATE TABLE entry_tag_ref (
  entry_id TEXT NOT NULL,
  tag_id TEXT NOT NULL,
  PRIMARY KEY (entry_id, tag_id),
  FOREIGN KEY(entry_id) REFERENCES entries(id) ON DELETE CASCADE,
  FOREIGN KEY(tag_id) REFERENCES tags(id) ON DELETE CASCADE
);
```

---

## 7. Kotlin Domain Model

```kotlin
@Serializable
data class Entry(
    val id: String,
    val code: String,
    val title: String,
    val performers: List<Performer>,
    val tags: List<Tag>,
    val rating: Float,
    val notes: String? = null,
    val status: EntryStatus = EntryStatus.COLLECTED,
    val favorite: Boolean = false,
    val watched: Boolean = false,
    val releaseDate: String? = null,
    val collectedAt: String,
    val sourceUrl: String? = null,
    val coverLocalPath: String? = null,
    val coverRemoteUrl: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class Performer(
    val id: String,
    val name: String,
)

@Serializable
data class Tag(
    val id: String,
    val name: String,
)

enum class EntryStatus {
    WISH,
    COLLECTED,
    WATCHED,
    ARCHIVED
}
```

---

## 8. Room DAO 设计

### EntryDao 核心接口

```kotlin
@Dao
interface EntryDao {
    @Transaction
    @Query("SELECT * FROM entries WHERE deleted_at IS NULL ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<EntryWithRelations>>

    @Transaction
    @Query("SELECT * FROM entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EntryWithRelations?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: EntryEntity)

    @Update
    suspend fun update(entry: EntryEntity)

    @Query("UPDATE entries SET deleted_at = :deletedAt, updated_at = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: String, updatedAt: String)

    @Query("SELECT * FROM entries WHERE code LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' ORDER BY updated_at DESC")
    suspend fun searchSimple(query: String): List<EntryEntity>
}
```

### 关联查询模型

```kotlin
data class EntryWithRelations(
    @Embedded val entry: EntryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = EntryPerformerCrossRef::class,
            parentColumn = "entry_id",
            entityColumn = "performer_id"
        )
    )
    val performers: List<PerformerEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = EntryTagCrossRef::class,
            parentColumn = "entry_id",
            entityColumn = "tag_id"
        )
    )
    val tags: List<TagEntity>
)
```

---

## 9. Repository 接口定义

### EntryRepository

```kotlin
interface EntryRepository {
    fun observeEntries(): Flow<List<Entry>>
    suspend fun getEntry(id: String): Entry?
    suspend fun addEntry(entry: Entry)
    suspend fun updateEntry(entry: Entry)
    suspend fun deleteEntry(id: String)
    suspend fun search(query: String, filter: EntryFilter?, sort: EntrySort): List<Entry>
}
```

### ImportExportRepository

```kotlin
interface ImportExportRepository {
    suspend fun exportCsv(entries: List<Entry>, uri: Uri): Result<Unit>
    suspend fun exportJson(entries: List<Entry>, uri: Uri): Result<Unit>
    suspend fun parseCsv(uri: Uri): Result<List<CsvRowRaw>>
    suspend fun previewImport(rows: List<CsvRowRaw>): Result<ImportPreview>
    suspend fun confirmImport(preview: ImportPreview, decision: ImportDecision): Result<ImportResult>
}
```

### BackupRepository

```kotlin
interface BackupRepository {
    suspend fun backupToWebDav(config: WebDavConfig, payload: BackupPayload): Result<BackupResult>
    suspend fun restoreFromWebDav(config: WebDavConfig, remoteFileName: String): Result<BackupPayload>
    suspend fun listRemoteBackups(config: WebDavConfig): Result<List<RemoteBackupFile>>
}
```

### SecurityRepository

```kotlin
interface SecurityRepository {
    suspend fun isPasswordSet(): Boolean
    suspend fun setPassword(password: String): Result<Unit>
    suspend fun verifyPassword(password: String): Boolean
    suspend fun clearPassword(): Result<Unit>
}
```

---

## 10. CSV 设计规范

### 10.1 导出原则
- 使用 UTF-8 with BOM，提升 Excel 兼容性
- 第一行为固定英文表头
- 演员和标签使用 `|` 作为内部数组分隔符，避免与 CSV 逗号冲突
- 空值导出为空字符串

### 10.2 CSV 表头

```csv
id,code,title,performers,tags,rating,notes,status,favorite,watched,release_date,collected_at,source_url,cover_local_path,cover_remote_url,created_at,updated_at
```

### 10.3 示例行

```csv
7f3f1c5d-1d8a-4c9c-a223-111111111111,ABP-123,示例标题,演员A|演员B,剧情|收藏,4.5,备注内容,COLLECTED,true,false,2025-08-01,2026-03-28T10:00:00Z,https://example.com,,https://img.example.com/a.jpg,2026-03-28T10:00:00Z,2026-03-28T10:00:00Z
```

### 10.4 导入规则
- 必须支持读取上述标准表头
- 可兼容部分别名列（如 `performer_names` -> `performers`）
- 导入前展示预览界面
- 预览时识别：
  - 新增记录
  - 疑似重复记录
  - 缺失关键字段记录
  - 非法评分记录

### 10.5 重复判断规则
默认按以下优先级判定：
1. `id` 完全相同
2. `code` 相同
3. `code + title` 相同

### 10.6 导入确认交互
用户在导入预览页中可对每条冲突记录选择：
- 跳过
- 新建
- 覆盖已有
- 手动合并（可后续实现；V1 先只支持前 3 项）

---

## 11. JSON 备份格式

JSON 用于系统级备份和后续网站迁移，必须完整保留结构。

### 11.1 备份文件结构

```json
{
  "schemaVersion": 1,
  "exportedAt": "2026-03-28T10:00:00Z",
  "appVersion": "1.0.0",
  "entries": [
    {
      "id": "7f3f1c5d-1d8a-4c9c-a223-111111111111",
      "code": "ABP-123",
      "title": "示例标题",
      "performers": [
        { "id": "p1", "name": "演员A" },
        { "id": "p2", "name": "演员B" }
      ],
      "tags": [
        { "id": "t1", "name": "剧情" },
        { "id": "t2", "name": "收藏" }
      ],
      "rating": 4.5,
      "notes": "备注内容",
      "status": "COLLECTED",
      "favorite": true,
      "watched": false,
      "releaseDate": "2025-08-01",
      "collectedAt": "2026-03-28T10:00:00Z",
      "sourceUrl": "https://example.com",
      "coverLocalPath": null,
      "coverRemoteUrl": "https://img.example.com/a.jpg",
      "createdAt": "2026-03-28T10:00:00Z",
      "updatedAt": "2026-03-28T10:00:00Z"
    }
  ]
}
```

### 11.2 网站迁移建议
网站侧优先读取 JSON，CSV 仅作为辅助导入格式。

---

## 12. WebDAV 备份/恢复设计

当前阶段 WebDAV 只做“手动备份/恢复”，不做自动同步。

### 12.1 WebDAV 配置项

```kotlin
data class WebDavConfig(
    val baseUrl: String,
    val username: String,
    val password: String,
    val remoteDir: String,
)
```

坚果云常见信息：
- URL：用户填写
- 用户名：用户填写
- 密码：应用专用密码
- remoteDir：如 `/collection-app/`

### 12.2 远端目录结构

```text
/collection-app/
  manifest.json
  backup-latest.json
  backup-latest.csv
  backups/
    backup-2026-03-28T10-00-00.json
    backup-2026-03-28T10-00-00.csv
```

### 12.3 备份流程
1. 从本地数据库读取全部记录
2. 生成 JSON 备份文件
3. 生成 CSV 导出文件
4. 生成 manifest.json
5. 通过 WebDAV 创建目录（若不存在）
6. 上传时间戳版本文件到 `/backups/`
7. 覆盖上传 `backup-latest.json`
8. 覆盖上传 `backup-latest.csv`
9. 覆盖上传 `manifest.json`
10. 返回成功结果与远端文件名

### 12.4 恢复流程
1. 连接 WebDAV
2. 拉取远端文件列表
3. 用户选择一个备份文件
4. 下载 JSON（优先）或 CSV
5. 解析成本地 `BackupPayload`
6. 展示恢复预览
7. 用户确认恢复策略：
   - 全量覆盖本地
   - 仅导入新增
8. 执行事务写入数据库

### 12.5 备份 manifest 结构

```json
{
  "schemaVersion": 1,
  "exportedAt": "2026-03-28T10:00:00Z",
  "recordCount": 1234,
  "jsonFile": "backup-latest.json",
  "csvFile": "backup-latest.csv",
  "sha256": {
    "json": "xxx",
    "csv": "yyy"
  }
}
```

---

## 13. WebDAV Client 最小实现要求

### 必须支持的 HTTP / WebDAV 方法
- `PROPFIND`：列目录/文件
- `MKCOL`：创建目录
- `PUT`：上传文件
- `GET`：下载文件
- `DELETE`：删除文件（可选）

### 接口定义建议

```kotlin
interface WebDavClient {
    suspend fun ensureDir(config: WebDavConfig, path: String): Result<Unit>
    suspend fun list(config: WebDavConfig, path: String): Result<List<WebDavFileItem>>
    suspend fun upload(config: WebDavConfig, remotePath: String, bytes: ByteArray, contentType: String): Result<Unit>
    suspend fun download(config: WebDavConfig, remotePath: String): Result<ByteArray>
}
```

### 认证
- Basic Auth
- 凭证保存在加密存储中，不明文落库

---

## 14. 密码锁设计

### 功能要求
- 首次启用时设置 4~6 位数字密码或完整文本密码
- App 冷启动进入锁屏页
- 切后台超过指定时长后回前台需要重新解锁
- 设置中支持修改密码、关闭密码锁

### 安全要求
- 密码不能明文保存
- 使用：
  - Android Keystore 保护加密密钥
  - 保存 PBKDF2 / bcrypt / scrypt 后的密码摘要
- 错误次数可增加短暂冷却

### 推荐实现
- `CryptoManager` 负责 Keystore 密钥
- `PasswordManager` 负责 hash 和 verify
- `AppLockManager` 负责记录当前解锁状态与超时逻辑

### DataStore 字段
- `app_lock_enabled: Boolean`
- `password_hash: String`
- `password_salt: String`
- `last_unlock_elapsed_realtime: Long`
- `lock_timeout_seconds: Int`

---

## 15. UI 页面定义

### 15.1 锁屏页
- 密码输入
- 解锁
- 进入设置修改密码（需先验证）

### 15.2 首页
支持切换两种模式：
1. 卡片列表视图
2. 表格视图

功能：
- 顶部搜索栏
- 排序菜单
- 筛选入口
- 新增按钮
- 批量选择入口

### 15.3 详情页
- 展示所有字段
- 编辑
- 删除
- 切换星标/已看

### 15.4 编辑页
字段：
- 番号（必填）
- 标题（必填）
- 多演员
- 多标签
- 评分（0~5，0.5 步长）
- 备注
- 状态
- 是否已看
- 是否星标
- 发布日期
- 收藏时间
- 来源链接
- 封面路径/封面 URL

### 15.5 导入导出页
- 导出 CSV
- 导出 JSON
- 导入 CSV
- 导入 JSON
- 导入预览
- 冲突确认

### 15.6 备份页
- WebDAV 配置
- 测试连接
- 立即备份
- 查看远端备份列表
- 选择远端备份恢复

### 15.7 设置页
- 密码锁设置
- 默认首页视图
- 默认排序方式
- CSV 编码与分隔符设置
- WebDAV 账号设置

---

## 16. 搜索、排序、筛选规则

### 搜索范围
- 番号
- 标题
- 演员名
- 标签名
- 备注

### 排序
- 更新时间降序（默认）
- 创建时间降序
- 评分降序
- 标题升序
- 番号升序
- 发布日期降序

### 筛选条件
- 状态
- 已看/未看
- 星标/非星标
- 评分区间
- 指定演员
- 指定标签
- 发布日期范围

---

## 17. 关键 UseCase 列表

### 记录管理
- `AddEntryUseCase`
- `UpdateEntryUseCase`
- `DeleteEntryUseCase`
- `GetEntryDetailUseCase`
- `SearchEntriesUseCase`
- `FilterEntriesUseCase`

### 导入导出
- `ExportCsvUseCase`
- `ExportJsonUseCase`
- `ParseCsvUseCase`
- `PreviewImportUseCase`
- `ConfirmImportUseCase`

### 备份恢复
- `BackupToWebDavUseCase`
- `ListRemoteBackupsUseCase`
- `RestoreFromWebDavUseCase`

### 安全
- `SetPasswordUseCase`
- `VerifyPasswordUseCase`
- `RequireUnlockUseCase`

---

## 18. 导入预览模型设计

```kotlin
@Serializable
data class ImportPreview(
    val totalRows: Int,
    val validRows: Int,
    val invalidRows: Int,
    val conflictRows: Int,
    val rows: List<ImportPreviewRow>
)

@Serializable
data class ImportPreviewRow(
    val index: Int,
    val raw: Map<String, String>,
    val parsed: Entry?,
    val status: ImportRowStatus,
    val conflictTargetId: String? = null,
    val message: String? = null,
)

enum class ImportRowStatus {
    NEW,
    CONFLICT,
    INVALID
}
```

---

## 19. 错误处理要求

统一使用：
- `sealed class AppError`
- UI 显示用户可读错误信息
- Repository 返回 `Result<T>` 或项目统一错误包装

重点处理场景：
- CSV 编码错误
- 缺少必填字段
- 评分越界
- WebDAV 鉴权失败
- 网络超时
- 远端目录不存在
- 数据库事务失败
- 恢复时文件格式版本不兼容

---

## 20. 性能与约束

### 数据量：几千条
该规模下 Room 完全足够。

### 性能要求
- 首页初次加载 < 1 秒（几千条下）
- 搜索响应 < 300ms
- 导入预览支持 1000+ 行 CSV
- 所有数据库批量写入必须使用事务

### 建议优化
- 首页分页或懒加载
- 列表项轻量化
- 表格视图避免一次渲染全部列宽计算

---

## 21. 测试要求

### 单元测试
- CSV 解析
- CSV 导出格式
- JSON 序列化/反序列化
- 去重规则
- 导入预览规则
- 密码验证

### 集成测试
- Room 增删改查
- 导入确认后写库
- WebDAV 客户端 mock 测试

### UI 测试
- 首页切换视图
- 新增记录
- 编辑记录
- 导入预览确认
- 锁屏解锁流程

---

## 22. 交付优先级

### P0（必须完成）
- 本地数据库
- 首页双视图
- 新增/编辑/删除
- 搜索/筛选/排序
- CSV 导入导出
- JSON 导入导出
- WebDAV 手动备份/恢复
- 密码锁

### P1（建议完成）
- 常用演员/标签自动补全
- 批量删除
- 远端备份列表展示
- 导入冲突分批确认

### P2（可后续补充）
- 图片选择与本地压缩
- 手动合并冲突
- 软删除回收站

---

## 23. Codex 开发指令建议

可以将以下内容直接作为给 Codex 的任务说明：

```text
请基于 Kotlin + Jetpack Compose + Room + Hilt + DataStore + kotlinx.serialization，为 Android 开发一个本地收藏管理 App。

要求：
1. 使用 MVVM + Clean Architecture 轻量分层。
2. 支持收藏记录管理，字段包括：
   - id
   - code
   - title
   - 多个 performers
   - 多个 tags
   - rating（0~5，支持 0.5）
   - notes
   - status
   - favorite
   - watched
   - releaseDate
   - collectedAt
   - sourceUrl
   - coverLocalPath
   - coverRemoteUrl
   - createdAt
   - updatedAt
3. Room 使用规范化表结构：entries、performers、entry_performer_ref、tags、entry_tag_ref。
4. 首页支持卡片列表和表格视图切换。
5. 支持搜索、排序、筛选。
6. 支持详细录入和编辑页面。
7. 支持 CSV 导入导出：
   - UTF-8 with BOM
   - 表头固定为：
     id,code,title,performers,tags,rating,notes,status,favorite,watched,release_date,collected_at,source_url,cover_local_path,cover_remote_url,created_at,updated_at
   - performers/tags 使用 | 分隔
   - 导入前必须预览并人工确认冲突
8. 支持 JSON 备份导入导出，结构与 domain model 对齐。
9. 支持 WebDAV 手动备份/恢复：
   - 使用 OkHttp 实现 Basic Auth WebDAV Client
   - 支持 PROPFIND/MKCOL/PUT/GET
   - 上传 manifest.json、backup-latest.json、backup-latest.csv、以及 /backups/ 时间戳版本文件
10. 支持密码锁：
   - 冷启动要求解锁
   - 使用 Android Keystore + 安全 hash 保存密码摘要
11. 请先生成：
   - 完整项目目录结构
   - Domain model
   - Room entities/dao/database
   - Repository interfaces and implementations skeleton
   - CSV parser/writer
   - JSON backup serializer
   - WebDAV client skeleton
   - 首页、编辑页、锁屏页、导入预览页 Compose 骨架
12. 所有关键类添加中文注释，保证代码可扩展。
13. 优先保证架构清晰和可运行骨架，再逐步补齐页面逻辑。
```

---

## 24. 实施顺序建议

### 第一步
先搭项目骨架：
- Hilt
- Navigation
- Room
- DataStore
- 主题
- 基础页面导航

### 第二步
完成本地数据闭环：
- 数据库
- Repository
- 首页列表
- 新增/编辑/详情

### 第三步
完成导入导出：
- CSV
- JSON
- 导入预览

### 第四步
完成密码锁

### 第五步
完成 WebDAV 手动备份恢复

---

## 25. 额外实现建议

1. `code` 输入时自动转大写
2. `rating` 使用滑块 + 可点击星级组件双输入
3. 演员、标签使用 Chips 编辑器
4. 来源链接点击可跳浏览器
5. 导入预览高亮异常字段
6. 恢复前先自动生成本地临时备份，降低误操作风险

---

## 26. 本轮变更需求（增量实施）

基于当前已完成状态与工作日志，新增以下迭代需求：工作日志显示当前工程已完成本地 CRUD、导入导出、WebDAV 手动备份恢复、锁屏会话和基础筛选，并已多次成功构建 debug 包，可在现有工程上直接增量开发。fileciteturn0file0

### 26.1 封面增强

#### 新需求
1. 除网络 `coverRemoteUrl` 外，增加本地图片上传能力
2. 对有封面的记录，在首页卡片视图、详情页、编辑页中展示封面缩略图
3. 导入本地图片后自动压缩和优化尺寸，避免原图过大导致存储膨胀和列表卡顿

#### 数据模型调整
`entries` 表保留并强化以下字段：
- `cover_local_path TEXT`
- `cover_remote_url TEXT`
- `cover_thumb_path TEXT`（新增，保存压缩后缩略图路径，推荐新增）
- `cover_updated_at TEXT`（新增，可选，用于封面更新时间）

推荐 SQL 增量迁移：

```sql
ALTER TABLE entries ADD COLUMN cover_thumb_path TEXT;
ALTER TABLE entries ADD COLUMN cover_updated_at TEXT;
```

#### 实现要求
- 编辑页新增“选择本地图片”入口
- 使用系统文件选择器选择图片
- 将原图复制到 App 私有目录，例如：
  - `/files/covers/original/`
  - `/files/covers/thumbs/`
- 自动生成两份：
  1. 原图压缩版（最长边限制，例如 1600px）
  2. 列表缩略图（例如宽 320px）
- 优先展示 `cover_thumb_path`
- 详情页点击封面可查看大图（V1 可先做简单预览）
- 删除记录时同步删除封面文件
- 更换封面时删除旧封面文件，避免垃圾文件堆积

#### 推荐实现类
```text
core/image/
  ImageCompressor.kt
  ImageStorageManager.kt
  ImagePickerHandler.kt
```

#### 推荐接口
```kotlin
interface ImageStorageManager {
    suspend fun importCoverFromUri(sourceUri: Uri, entryId: String): Result<CoverImageResult>
    suspend fun deleteCoverFiles(entryId: String): Result<Unit>
}

data class CoverImageResult(
    val localPath: String,
    val thumbPath: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
)
```

#### 图片压缩建议
- 解码前先读取尺寸
- 按目标尺寸采样压缩
- JPEG/WebP 有损压缩质量建议 80~88
- 避免在主线程处理图片
- 列表中使用 Coil 加载缩略图

---

### 26.2 增强筛选：标签筛选 + 演员筛选

#### 新需求
在现有状态筛选、星标筛选、已看筛选基础上，新增：
- 标签筛选
- 演员筛选

#### 数据层要求
由于项目已使用规范化表结构（`performers`、`tags`、交叉表），应直接基于关联表实现筛选，不要回退为字符串 contains。 

#### Domain 模型调整
扩展 `EntryFilter`：

```kotlin
data class EntryFilter(
    val statuses: Set<EntryStatus> = emptySet(),
    val favorite: Boolean? = null,
    val watched: Boolean? = null,
    val performerIds: Set<String> = emptySet(),
    val tagIds: Set<String> = emptySet(),
    val ratingMin: Float? = null,
    val ratingMax: Float? = null,
)
```

#### Repository / DAO 要求
- 提供查询全部标签列表接口
- 提供查询全部演员列表接口
- 支持多选筛选
- 推荐语义：
  - 选中多个标签：默认“任一命中”
  - 选中多个演员：默认“任一命中”
- 如后续需要，可扩展为“同时命中全部”模式

#### UI 要求
首页新增筛选面板，支持：
- 状态多选
- 星标
- 已看
- 标签多选
- 演员多选
- 清空筛选

可先使用底部弹窗或全屏筛选页实现。

---

### 26.3 增加统计模块

#### 新需求
新增“统计”模块，作为底部导航一级页面。

#### 统计页首版内容
1. 总记录数
2. 已看数量
3. 未看数量
4. 星标数量
5. 平均评分
6. 按状态统计
7. 按标签 Top N
8. 按演员 Top N
9. 最近新增数量（近 7 天 / 30 天）

#### Domain 模型建议
```kotlin
data class StatisticsSummary(
    val totalCount: Int,
    val watchedCount: Int,
    val unwatchedCount: Int,
    val favoriteCount: Int,
    val averageRating: Float,
    val statusCounts: List<StatusCount>,
    val topTags: List<NameCount>,
    val topPerformers: List<NameCount>,
    val addedIn7Days: Int,
    val addedIn30Days: Int,
)

data class StatusCount(
    val status: EntryStatus,
    val count: Int,
)

data class NameCount(
    val id: String,
    val name: String,
    val count: Int,
)
```

#### DAO 建议
新增 `StatisticsDao` 或在 `EntryRelationDao` 中补充聚合查询：
- `getTotalCount()`
- `getWatchedCount()`
- `getFavoriteCount()`
- `getAverageRating()`
- `getStatusCounts()`
- `getTopTags(limit: Int)`
- `getTopPerformers(limit: Int)`
- `getAddedCountSince(date: String)`

#### UI 要求
统计页可使用以下 Compose 组件：
- 顶部概览卡片
- 分类统计卡片
- Top 标签列表
- Top 演员列表
- 简单条形图或进度条（V1 可先用文本 + LinearProgressIndicator）

首版不强制引入图表库，优先保证稳定和信息清晰。

---

### 26.4 UI 改造：底部导航栏

#### 新需求
将主界面调整为底部导航三大块：
- 首页
- 统计
- 设置

#### 导航结构建议
```text
MainScaffold
  BottomBar
    Home
    Stats
    Settings
```

二级页面：
- 详情页
- 编辑页
- 导入导出页
- 备份页
- 锁屏页
- 导入预览页

#### 路由建议
```kotlin
object Routes {
    const val Lock = "lock"
    const val Home = "home"
    const val Stats = "stats"
    const val Settings = "settings"
    const val Detail = "detail/{entryId}"
    const val Edit = "edit?entryId={entryId}"
    const val ImportExport = "import_export"
    const val ImportPreview = "import_preview"
    const val Backup = "backup"
}
```

#### 实现要求
- 首页、统计、设置作为底部一级路由
- 二级页面不显示底部栏，或按需隐藏
- 当前选中项高亮
- 底部栏使用 `NavigationBar`
- 保留首页顶部搜索和筛选入口

---

## 27. 数据库迁移要求

本轮需求需要新增字段，必须补充 Room Migration。

### Migration 方向
- `entries` 新增 `cover_thumb_path`
- `entries` 新增 `cover_updated_at`
- 如当前未落地 `cover_local_path` 字段，则一并补上

示例：

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE entries ADD COLUMN cover_thumb_path TEXT")
        database.execSQL("ALTER TABLE entries ADD COLUMN cover_updated_at TEXT")
    }
}
```

同时：
- 升级 `schemaVersion`
- 更新 JSON 备份结构
- 更新 CSV 导入导出映射（若要加入 `cover_thumb_path` 可选列）

---

## 28. 需要同步修改的模块清单

### Data 层
- `EntryEntity`
- `AppDatabase` migration
- `EntryDao` / `EntryRelationDao`
- `StatisticsDao`（新增）
- `EntryRepositoryImpl`
- `ImportExportRepositoryImpl`

### Domain 层
- `Entry`
- `EntryFilter`
- `StatisticsSummary`（新增）
- `GetStatisticsUseCase`（新增）

### Presentation 层
- 首页筛选 UI
- 首页卡片/表格增加封面展示
- 统计页新增
- 设置页迁移到底部一级入口
- 主导航改为底部导航结构
- 编辑页增加本地图片选择

### Core 层
- `ImageCompressor`
- `ImageStorageManager`
- `ImagePickerHandler`

---

## 29. 给 Codex 的增量开发指令

```text
请在现有 Android 项目基础上继续增量开发，不要重构已有主架构。

本轮新增需求：
1. 封面支持本地图片上传
   - 编辑页增加从系统选择器导入本地图片
   - 将图片复制到 App 私有目录
   - 自动压缩原图并生成缩略图
   - entries 表新增 cover_thumb_path、cover_updated_at
   - 首页卡片视图、表格视图、详情页展示封面；优先加载缩略图
   - 删除或替换封面时清理旧文件
2. 首页筛选增强
   - 在现有状态/星标/已看筛选基础上，新增标签筛选和演员筛选
   - 支持多选
   - 默认语义为“任一命中”
3. 新增统计模块
   - 作为底部导航一级页面
   - 展示：总数、已看、未看、星标、平均评分、按状态统计、Top 标签、Top 演员、近 7/30 天新增数
   - 优先使用 Room 聚合查询，不引入重型图表库
4. 主 UI 改造
   - 增加底部导航栏：首页、统计、设置
   - 详情、编辑、导入导出、备份等作为二级页面

实现要求：
- 使用 Room Migration 处理数据库升级
- 保持 UTF-8 编码
- 保持 Kotlin + Compose + MVVM + Hilt + Room 架构不变
- 优先交付可运行版本
- 所有新增关键类写中文注释

请优先输出：
1. 需要修改的目录和文件列表
2. Room migration 和 entity 更新
3. 统计 DAO / repository / usecase
4. 底部导航改造
5. 本地图片导入与压缩骨架
6. 首页筛选 UI 和 ViewModel 更新
7. 最后确保 assembleDebug 可通过
```

---

## 30. 最终说明

这四项需求都适合在当前代码基线上增量实现，不需要推翻原有架构。根据工作日志，现有版本已经具备较完整的功能闭环和稳定构建基础，因此本轮重点是：
- 补充媒体资源管理（本地封面）
- 扩展关联筛选（标签/演员）
- 增加聚合统计
- 把导航升级为更完整的三段式主框架。fileciteturn0file0

