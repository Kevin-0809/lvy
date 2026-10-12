-- Ivy 业务主库完整 DDL（openGauss / PostgreSQL 兼容模式，UTF-8）。
-- 在 TSS 连接的目标 schema 中执行；不涉及只读 BXDS 源表。
-- 不保留历史数据：先删除既有 Ivy 表再重建，请勿在生产数据上直接执行。
-- 整个脚本在一个事务中完成，可重复执行。
BEGIN;

DROP TABLE IF EXISTS ivy_api_request CASCADE;
DROP TABLE IF EXISTS ivy_request_history CASCADE;
DROP TABLE IF EXISTS ivy_collection CASCADE;
DROP TABLE IF EXISTS ivy_msg_sample CASCADE;

CREATE TABLE ivy_collection (
    id bigserial CONSTRAINT ivy_collection_pkey PRIMARY KEY,
    name varchar(200) NOT NULL,
    description varchar(1000),
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL
);
COMMENT ON TABLE ivy_collection IS 'Ivy HTTP 请求集合';
COMMENT ON COLUMN ivy_collection.id IS '集合主键，自增编号';
COMMENT ON COLUMN ivy_collection.name IS '集合名称';
COMMENT ON COLUMN ivy_collection.description IS '集合说明';
COMMENT ON COLUMN ivy_collection.created_at IS '创建时间';
COMMENT ON COLUMN ivy_collection.updated_at IS '最后更新时间';

CREATE TABLE ivy_api_request (
    id bigserial CONSTRAINT ivy_api_request_pkey PRIMARY KEY,
    collection_id bigint NOT NULL,
    name varchar(200) NOT NULL,
    method varchar(10),
    url varchar(4000),
    headers text,
    params text,
    body_type varchar(30),
    body_content text,
    auth_type varchar(30),
    auth_config text,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL
);
-- collection_id 为应用维护的逻辑关联，与现有实体映射一致。
COMMENT ON TABLE ivy_api_request IS 'Ivy 已保存的 HTTP 请求';
COMMENT ON COLUMN ivy_api_request.id IS '请求主键，自增编号';
COMMENT ON COLUMN ivy_api_request.collection_id IS '所属集合编号，逻辑关联 ivy_collection.id';
COMMENT ON COLUMN ivy_api_request.name IS '请求名称';
COMMENT ON COLUMN ivy_api_request.method IS 'HTTP 请求方法';
COMMENT ON COLUMN ivy_api_request.url IS '请求地址';
COMMENT ON COLUMN ivy_api_request.headers IS '请求头键值列表，JSON 格式';
COMMENT ON COLUMN ivy_api_request.params IS '查询参数键值列表，JSON 格式';
COMMENT ON COLUMN ivy_api_request.body_type IS '请求体类型：none、raw、xml、x-www-form-urlencoded 或 form-data';
COMMENT ON COLUMN ivy_api_request.body_content IS '请求体内容，表单类型存储为 JSON';
COMMENT ON COLUMN ivy_api_request.auth_type IS '认证类型：none、bearer 或 basic';
COMMENT ON COLUMN ivy_api_request.auth_config IS '认证配置，JSON 格式，接口返回时对密码和令牌脱敏';
COMMENT ON COLUMN ivy_api_request.created_at IS '创建时间';
COMMENT ON COLUMN ivy_api_request.updated_at IS '最后更新时间';

CREATE TABLE ivy_request_history (
    id bigserial CONSTRAINT ivy_request_history_pkey PRIMARY KEY,
    method varchar(10),
    url varchar(4000),
    headers text,
    params text,
    body_type varchar(30),
    body_content text,
    auth_type varchar(30),
    auth_config text,
    status_code integer,
    duration_ms bigint,
    response_size bigint,
    created_at timestamp NOT NULL
);
COMMENT ON TABLE ivy_request_history IS 'Ivy HTTP 请求发送历史';
COMMENT ON COLUMN ivy_request_history.id IS '历史主键，自增编号';
COMMENT ON COLUMN ivy_request_history.method IS 'HTTP 请求方法';
COMMENT ON COLUMN ivy_request_history.url IS '请求地址';
COMMENT ON COLUMN ivy_request_history.headers IS '请求头键值列表，JSON 格式';
COMMENT ON COLUMN ivy_request_history.params IS '查询参数键值列表，JSON 格式';
COMMENT ON COLUMN ivy_request_history.body_type IS '请求体类型';
COMMENT ON COLUMN ivy_request_history.body_content IS '请求体内容';
COMMENT ON COLUMN ivy_request_history.auth_type IS '认证类型';
COMMENT ON COLUMN ivy_request_history.auth_config IS '认证配置，JSON 格式，接口返回时对密码和令牌脱敏';
COMMENT ON COLUMN ivy_request_history.status_code IS '目标接口 HTTP 响应状态码';
COMMENT ON COLUMN ivy_request_history.duration_ms IS '请求耗时，单位毫秒';
COMMENT ON COLUMN ivy_request_history.response_size IS '响应内容大小，单位字节';
COMMENT ON COLUMN ivy_request_history.created_at IS '请求历史创建时间';

CREATE TABLE ivy_msg_sample (
    id bigserial CONSTRAINT ivy_msg_sample_pkey PRIMARY KEY,
    service_code varchar(128) NOT NULL,
    partition_name varchar(32),
    trans_id varchar(64) NOT NULL,
    source_ip varchar(64) NOT NULL,
    txn_time bigint,
    message_type varchar(32) NOT NULL,
    global_seq_no varchar(64),
    tran_teller_no varchar(32),
    content text,
    edited boolean NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT uk_ivy_msg_sample_key UNIQUE (trans_id, source_ip, message_type)
);
CREATE INDEX idx_ivy_msg_sample_service_code ON ivy_msg_sample (service_code);
COMMENT ON TABLE ivy_msg_sample IS 'Ivy 从 BXDS 采集的报文样本及明文';
COMMENT ON COLUMN ivy_msg_sample.id IS '样本主键，自增编号';
COMMENT ON COLUMN ivy_msg_sample.service_code IS '服务码，不含报文格式后缀';
COMMENT ON COLUMN ivy_msg_sample.partition_name IS '来源日期分区名称';
COMMENT ON COLUMN ivy_msg_sample.trans_id IS '来源交易流水号';
COMMENT ON COLUMN ivy_msg_sample.source_ip IS '来源 IP 地址';
COMMENT ON COLUMN ivy_msg_sample.txn_time IS '来源交易时间，毫秒时间戳';
COMMENT ON COLUMN ivy_msg_sample.message_type IS '报文类型：bzjson 或 soap';
COMMENT ON COLUMN ivy_msg_sample.global_seq_no IS '全局流水号';
COMMENT ON COLUMN ivy_msg_sample.tran_teller_no IS '交易柜员号';
COMMENT ON COLUMN ivy_msg_sample.content IS '报文明文，TEXT 类型，保存解码后的 JSON 或 XML 文本';
COMMENT ON COLUMN ivy_msg_sample.edited IS '是否修改过明文';
COMMENT ON COLUMN ivy_msg_sample.created_at IS '采集入库时间';
COMMENT ON COLUMN ivy_msg_sample.updated_at IS '最后更新时间';

-- tss_service_auth 为 TSS 既有表，由外部维护，脚本不管理其 DDL。

COMMIT;
