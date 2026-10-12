# 设计：新增 XML Body 类型

日期：2026-10-08
主题：为 lvy 轻量 API 客户端增加发送/展示 XML 报文的能力

## 背景

lvy 是一个 Spring Boot + 原生前端的轻量 API 客户端。请求 Body 类型当前支持
`none`、`raw`、`x-www-form-urlencoded`、`form-data`，其中 `raw` 在
`ProxyService.buildBody` 中把 `Content-Type` 硬编码为 `application/json`。
用户需要发送 XML 报文，因此新增独立的 XML Body 类型。

## 目标

- 在 Body 类型中新增 `XML` 选项，发送时自动使用 `Content-Type: application/xml`。
- XML 请求体与 XML 响应均支持格式化/压缩。
- 保持与现有 JSON 编辑体验一致，尽量复用既有 UI 与代码路径。

## 非目标

- 不引入 XML 语法高亮、XPath、Schema 校验等高级能力。
- 不改变 `raw`（JSON）与其他 Body 类型的既有行为。

## 架构与改动点

### 后端

`src/main/java/cn/sunline/lvy/service/ProxyService.java`

- `buildBody` 新增 `case "xml"`：以 UTF-8 输出 `bodyContent`，
  `Content-Type` 为 `application/xml; charset=utf-8`。
- `raw` 维持 `application/json; charset=utf-8` 不变；用户显式设置
  `Content-Type` 请求头时仍优先于默认值（沿用现有 `hasContentType` 逻辑）。
- 响应解码无需改动：`isBinary` 已将 `xml` 归为文本，`resolveCharset` 已支持
  解析 `charset`。
- `ProxyRequest`、实体与数据库无需迁移（`bodyType` 为 String，
  `ddl-auto: update`）。

### 前端

`src/main/resources/static/index.html`

- Body 类型 radio 组新增 `value="xml"` 的 `XML` 选项。
- 复用 raw 编辑区（`#bodyRaw` / `#rawBodyInput`）承载 XML 内容，按类型切换
  占位符。

`src/main/resources/static/js/app.js`

- `toggleBodyAreas`：`raw` 或 `xml` 均显示编辑区。
- `buildProxyPayload`：`xml` 与 `raw` 一致取 `rawBody` 作为 `bodyContent`。
- `applyBodyContent`：`xml` 也回填 `rawBody`，修正当前 else 分支会清空内容的问题，
  保证从历史/集合恢复 XML 请求时内容不丢失。
- 新增 XML 工具函数：
  - 解析：`DOMParser` 解析 `application/xml`，通过 `parsererror` 检测非法 XML。
  - 压缩：去除元素间空白文本节点后序列化，保留注释、CDATA、处理指令。
  - 格式化：对序列化结果用字符扫描 tokenizer 按深度缩进，正确处理含 `>` 的
    属性值与文本节点。
- 请求/响应的“格式化、压缩”按钮统一分派：JSON 走 `JSON.parse`，XML 走 XML
  工具，均不支持时给出提示。
- 响应 Pretty 视图：识别为 XML 时以缩进后的 `pre` 展示，否则维持现有 JSON
  树/纯文本逻辑。

## 数据流

Body 选择 XML → 编辑 `rawBody` → `buildProxyPayload` 携带 `bodyType:"xml"` →
后端设置 `application/xml` 发送 → 响应依据 `Content-Type` 或内容识别 XML →
可格式化/压缩展示。

XML 识别规则：`Content-Type` 含 `xml`，或内容 trim 后以 `<` 开头且能成功解析。

## 错误处理

- 非法 XML：格式化/压缩时提示“不是合法 XML”，不修改原文。
- 发送不受格式化限制，非法或任意文本仍按原文发送（与 raw 一致）。
- 空内容：与 raw 一致，发送空体。

## 测试与验证

- 后端新增测试：使用 JDK 自带 `com.sun.net.httpserver.HttpServer` 启动本地回显
  服务，调用 `ProxyService.send`，断言服务端收到的 `Content-Type` 含
  `application/xml` 且 body 原样一致；并覆盖显式请求头覆盖默认值的场景。
- 运行 `mvn test`。
- 前端：启动应用后在浏览器验证 XML 请求发送、格式化/压缩、XML 响应展示。
