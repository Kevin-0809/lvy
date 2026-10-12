# 设计：报文迁移与本地管理（Ivy）

日期：2026-10-08
主题：在 Ivy 中新增按服务码从 bxds.msg_flow_log_request 采集报文、本地落明文并支持查看/修改的功能

## 背景

Ivy 是 Spring Boot + 原生前端的轻量工具。现需要新增"报文迁移"：输入服务码，
从 bxds 数据源的 `msg_flow_log_request` 表按分区从近到远查找满足条件的报文，
直到满足数量；报文本地落明文；并提供独立页面查看、修改报文。

已确认事实（来自环境与代码探索）：

- 服务码 = `msg_flow_log_request.txn_code`
- `message_type` 取值就是 `bzjson` / `soap`，只采集这两种
- `msg_flow_log_request` 是 openGauss 分区表，分区名形如 `p20260901`（按日）
- bxds 连接为 openGauss（PostgreSQL 协议），rose 项目以 `adp` 用户访问
- `request_message` 为 blob，需解码为明文文本

## 目标

- 输入服务码 + 数量，按分区从近到远采集 `bzjson`/`soap` 报文。
- 报文明文存本地 H2，支持在独立页面查看与修改。
- 采集幂等，重复报文不重复入库。

## 非目标

- 不做回写 bxds、不做与 tss 的跨库配对迁移。
- 不做响应报文 `msg_flow_log_response` 的处理。
- 不做导出、批量删除等未要求能力。

## 架构与改动点

### 依赖与配置

- `pom.xml` 新增 `org.postgresql:postgresql`。
- 新增 `BxdsDataSourceConfig`：注册 `bxdsDataSource` 与 `bxdsJdbcTemplate`，
  配置项 `ivy.datasource.bxds.url/username/password/driver`；
  现有 H2 `spring.datasource` 保持 primary。
- 配置项 `ivy.msg-migration.charset`（默认 `UTF-8`）。报文源 SQL 不指定 schema，
  使用 bxds 数据源连接的默认 schema（由数据库用户的 `search_path` 决定）。
- `application.yml` 增加上述配置。

### 采集流程（`MsgMigrationService`）

输入：`serviceCode`（= `txn_code`）、`limit`。

1. 解析源 schema，查询 `pg_partition` 获取该表所有分区名
   `pYYYYMMDD`，按日期**降序（近到远）**排序。
2. 逐个分区执行：
   `select source_ip, trans_id, txn_code, txn_time, message_type, request_message,
   global_seq_no, tran_teller_no from {schema}.msg_flow_log_request partition({p})
   where txn_code = :code and message_type in ('bzjson','soap')
   order by txn_time desc`
3. 读取 `request_message`（blob）解码为明文：默认 UTF-8，遇到非法字节回退 GBK。
4. 写入本地 H2；已存在的 `(trans_id, source_ip, message_type)` 跳过且不计入新增。
5. 新增数达到 `limit` 即停止；扫完仍不足则返回实际数量。
6. 返回统计：新增数、跳过数、扫描分区数、命中分区列表。

数量语义：目标是本地**新增** `limit` 条满足条件的报文；重复的跳过。

### 本地存储（H2 新表 `msg_sample`）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK | 自增 |
| service_code | varchar(128) | 服务码（txn_code） |
| partition_name | varchar(32) | 源分区名 pYYYYMMDD |
| trans_id | varchar(64) | 交易流水号 |
| source_ip | varchar(64) | 来源 IP |
| txn_time | bigint | 交易时间（毫秒） |
| message_type | varchar(32) | bzjson / soap |
| global_seq_no | varchar(64) | 全局流水号 |
| tran_teller_no | varchar(32) | 交易柜员号 |
| content | clob | 明文报文 |
| edited | boolean | 是否被修改过 |
| created_at | timestamp | 采集时间 |
| updated_at | timestamp | 更新时间 |

约束：唯一键 `(trans_id, source_ip, message_type)`；`service_code` 建索引。

### 接口（`MsgMigrationController`，`/api/msg-migration`）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /collect | 入参 `{serviceCode, limit}`，执行采集，返回统计 |
| GET | /samples?serviceCode=&page=&size= | 分页查询已采集元数据 |
| GET | /samples/{id} | 查看单条明文报文 |
| PUT | /samples/{id} | 保存修改后的明文，置 edited=true |

统一使用现有 `ApiResponse` 包装；异常走 `BizException` + `GlobalExceptionHandler`。

### 独立页面

- `static/msg-migration.html` + `static/js/msg-migration.js`，复用 `css/style.css`。
- 采集表单：服务码、数量（默认 50，范围 1–1000）、采集按钮与结果提示。
- 列表：服务码、分区、trans_id、txn_time、message_type、是否修改、更新时间；
  支持按服务码筛选与分页。
- 详情区：查看明文（依据 message_type 展示 JSON/XML），可编辑并保存。

## 错误处理

- bxds 连接失败 / 未找到分区：返回明确错误，不影响本地已有数据。
- 单分区查询失败：记录并继续下一个分区，最终汇总。
- `request_message` 为空或解码失败：保留占位提示，不中断整体采集。
- `limit`、`serviceCode` 校验失败：返回 400。

## 测试与验证

- `MsgMigrationService` 单元测试：分区倒序扫描、limit 截断、去重跳过、解码逻辑。
- `mvn test` 通过。
- 启动应用后手工验证页面采集、列表、查看、修改保存。
