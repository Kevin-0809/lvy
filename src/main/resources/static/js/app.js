/* lvy - 原生 JS 前端逻辑 */
(function () {
    "use strict";

    var $ = function (sel, root) {
        return (root || document).querySelector(sel);
    };
    var $$ = function (sel, root) {
        return Array.prototype.slice.call((root || document).querySelectorAll(sel));
    };

    var STATUS_TEXT = {
        200: "OK", 201: "Created", 202: "Accepted", 204: "No Content",
        301: "Moved Permanently", 302: "Found", 304: "Not Modified",
        400: "Bad Request", 401: "Unauthorized", 403: "Forbidden", 404: "Not Found",
        405: "Method Not Allowed", 409: "Conflict", 415: "Unsupported Media Type",
        422: "Unprocessable Entity", 429: "Too Many Requests",
        500: "Internal Server Error", 502: "Bad Gateway", 503: "Service Unavailable",
        504: "Gateway Timeout"
    };

    var state = {
        collections: [],
        activeCollectionId: null,
        collectionRequests: {},
        history: [],
        editingRequestId: null,
        response: null,
        request: defaultRequest()
    };

    function defaultRequest() {
        return {
            method: "GET",
            url: "",
            params: [],
            headers: [],
            bodyType: "none",
            rawBody: "",
            formBody: [],
            authType: "none",
            authConfig: {}
        };
    }

    /* ---------- 工具 ---------- */

    function el(tag, cls, text) {
        var node = document.createElement(tag);
        if (cls) node.className = cls;
        if (text !== undefined && text !== null) node.textContent = text;
        return node;
    }

    function svgIcon(path) {
        var ns = "http://www.w3.org/2000/svg";
        var svg = document.createElementNS(ns, "svg");
        svg.setAttribute("viewBox", "0 0 24 24");
        var p = document.createElementNS(ns, "path");
        p.setAttribute("d", path);
        svg.appendChild(p);
        return svg;
    }

    function methodTag(method) {
        var m = (method || "GET").toUpperCase();
        var tag = el("span", "method-tag");
        tag.dataset.method = m;
        tag.textContent = m;
        return tag;
    }

    function getProgressBar() {
        return $("#progress");
    }

    function progressOn() {
        getProgressBar().classList.add("active");
    }

    function progressOff() {
        getProgressBar().classList.remove("active");
    }

    function toast(message, isError) {
        var root = $("#toastRoot");
        var node = el("div", "toast" + (isError ? " error" : ""), message);
        root.appendChild(node);
        requestAnimationFrame(function () {
            node.classList.add("show");
        });
        setTimeout(function () {
            node.classList.remove("show");
            setTimeout(function () {
                node.remove();
            }, 200);
        }, 2400);
    }

    function api(path, options) {
        options = options || {};
        var init = {
            method: options.method || "GET",
            headers: { "Content-Type": "application/json" }
        };
        if (options.body !== undefined) {
            init.body = JSON.stringify(options.body);
        }
        return fetch(path, init).then(function (res) {
            return res.json().catch(function () {
                throw new Error("响应解析失败");
            });
        }).then(function (payload) {
            if (!payload || typeof payload.code === "undefined") {
                throw new Error("返回格式异常");
            }
            if (payload.code !== 0) {
                throw new Error(payload.message || "请求失败");
            }
            return payload.data;
        });
    }

    function formatBytes(size) {
        if (size === null || size === undefined) return "-";
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return (size / 1024).toFixed(1) + " KB";
        return (size / 1024 / 1024).toFixed(2) + " MB";
    }

    function timeAgo(iso) {
        if (!iso) return "";
        var t = new Date(iso).getTime();
        var diff = Date.now() - t;
        if (diff < 60000) return "刚刚";
        if (diff < 3600000) return Math.floor(diff / 60000) + " 分钟前";
        if (diff < 86400000) return Math.floor(diff / 3600000) + " 小时前";
        return Math.floor(diff / 86400000) + " 天前";
    }

    /* ---------- 键值表编辑器 ---------- */

    function renderKv(container, list, opts) {
        opts = opts || {};
        container.innerHTML = "";
        list.forEach(function (pair) {
            container.appendChild(buildKvRow(container, list, pair, false, opts));
        });
        container.appendChild(buildKvRow(container, list, newPair(), true, opts));
        container.appendChild(buildKvAddButton(container, list, opts));
    }

    function buildKvAddButton(container, list, opts) {
        var wrap = el("div", "kv-add");
        var btn = el("button", "kv-add-btn");
        btn.type = "button";
        btn.appendChild(svgIcon("M12 5v14M5 12h14"));
        btn.appendChild(document.createTextNode("添加一行"));
        btn.addEventListener("click", function () {
            addKvRow(container, list, opts);
        });
        wrap.appendChild(btn);
        return wrap;
    }

    function addKvRow(container, list, opts) {
        list.push(newPair());
        renderKv(container, list, opts);
        var rows = container.querySelectorAll(".kv-row");
        if (rows.length >= 2) {
            var input = rows[rows.length - 2].querySelector('input[type="text"]');
            if (input) input.focus();
        }
    }

    function newPair() {
        return { key: "", value: "", description: "", enabled: true };
    }

    function buildKvRow(container, list, pair, phantom, opts) {
        var row = el("div", "kv-row");
        if (showColumnCount(opts) === 4) {
            row.style.gridTemplateColumns = "28px 1fr 1.4fr 28px";
        }

        var checkWrap = el("div", "kv-cell kv-check");
        var check = document.createElement("input");
        check.type = "checkbox";
        check.checked = pair.enabled !== false;
        checkWrap.appendChild(check);

        var keyCell = el("div", "kv-cell");
        var keyInput = document.createElement("input");
        keyInput.type = "text";
        keyInput.value = pair.key || "";
        keyInput.placeholder = opts.keyPlaceholder || "key";
        keyInput.spellcheck = false;
        keyCell.appendChild(keyInput);

        var valueCell = el("div", "kv-cell");
        var valueInput = document.createElement("input");
        valueInput.type = "text";
        valueInput.value = pair.value || "";
        valueInput.placeholder = opts.valuePlaceholder || "value";
        valueInput.spellcheck = false;
        valueCell.appendChild(valueInput);

        var actionCell = el("div", "kv-cell row-actions");
        var delBtn = el("button", "icon-btn");
        delBtn.title = "删除";
        delBtn.appendChild(svgIcon("M6 6l12 12M18 6L6 18"));
        actionCell.appendChild(delBtn);

        row.appendChild(checkWrap);
        row.appendChild(keyCell);
        row.appendChild(valueCell);

        var descInput = null;
        if (opts.showDescription !== false) {
            var descCell = el("div", "kv-cell");
            descInput = document.createElement("input");
            descInput.type = "text";
            descInput.value = pair.description || "";
            descInput.placeholder = opts.descPlaceholder || "描述";
            descCell.appendChild(descInput);
            row.appendChild(descCell);
        }

        row.appendChild(actionCell);

        function commit() {
            pair.key = keyInput.value;
            pair.value = valueInput.value;
            if (descInput) pair.description = descInput.value;
            pair.enabled = check.checked;
            if (phantom) {
                if (pair.key === "" && pair.value === "" && (!descInput || descInput.value === "")) {
                    return;
                }
                phantom = false;
                row.classList.remove("phantom");
                list.push(pair);
                var fresh = buildKvRow(container, list, newPair(), true, opts);
                container.appendChild(fresh);
            }
            if (opts.onChange) opts.onChange();
        }

        keyInput.addEventListener("input", commit);
        valueInput.addEventListener("input", commit);
        if (descInput) descInput.addEventListener("input", commit);
        check.addEventListener("change", commit);

        function onEnter(e) {
            if (e.key !== "Enter") return;
            e.preventDefault();
            if (phantom) {
                commit();
            } else {
                addKvRow(container, list, opts);
            }
        }

        keyInput.addEventListener("keydown", onEnter);
        valueInput.addEventListener("keydown", onEnter);

        delBtn.addEventListener("click", function () {
            if (phantom) return;
            var index = list.indexOf(pair);
            if (index >= 0) list.splice(index, 1);
            row.remove();
            if (opts.onChange) opts.onChange();
        });

        if (phantom) {
            row.classList.add("phantom");
            delBtn.style.visibility = "hidden";
        }

        return row;
    }

    function showColumnCount(opts) {
        return opts.showDescription === false ? 4 : 5;
    }

    /* ---------- 侧边栏渲染 ---------- */

    function renderCollections() {
        var container = $("#collectionList");
        container.innerHTML = "";
        if (!state.collections.length) {
            container.appendChild(el("div", "side-empty", "暂无集合"));
            return;
        }
        state.collections.forEach(function (collection) {
            var item = el("div", "side-item");
            if (state.activeCollectionId === collection.id) item.classList.add("active");
            var main = el("div", "item-main");
            main.textContent = collection.name;
            var meta = el("span", "item-meta", String(collection.requestCount || 0));
            var actions = el("div", "item-actions");

            var renameBtn = el("button", "icon-btn");
            renameBtn.title = "重命名";
            renameBtn.appendChild(svgIcon("M4 20h4l10-10-4-4L4 16v4z"));
            renameBtn.addEventListener("click", function (e) {
                e.stopPropagation();
                renameCollection(collection);
            });

            var delBtn = el("button", "icon-btn");
            delBtn.title = "删除";
            delBtn.appendChild(svgIcon("M4 7h16M9 7V5h6v2M6 7l1 12h10l1-12"));
            delBtn.addEventListener("click", function (e) {
                e.stopPropagation();
                deleteCollection(collection);
            });

            actions.appendChild(renameBtn);
            actions.appendChild(delBtn);
            item.appendChild(main);
            item.appendChild(meta);
            item.appendChild(actions);
            item.addEventListener("click", function () {
                toggleCollection(collection.id);
            });
            container.appendChild(item);

            if (state.activeCollectionId === collection.id) {
                container.appendChild(buildCollectionRequests(collection.id));
            }
        });
    }

    function buildCollectionRequests(collectionId) {
        var wrap = el("div", "side-nested");
        var requests = state.collectionRequests[collectionId] || [];
        if (!requests.length) {
            wrap.appendChild(el("div", "side-empty", "该集合下暂无请求"));
            return wrap;
        }
        requests.forEach(function (req) {
            var item = el("div", "side-item");
            if (state.editingRequestId === req.id) item.classList.add("active");
            item.appendChild(methodTag(req.method));
            var main = el("div", "item-main");
            main.textContent = req.name;
            var delBtn = el("button", "icon-btn");
            delBtn.title = "删除";
            delBtn.appendChild(svgIcon("M6 6l12 12M18 6L6 18"));
            delBtn.addEventListener("click", function (e) {
                e.stopPropagation();
                deleteRequest(req);
            });
            item.appendChild(main);
            item.appendChild(delBtn);
            item.addEventListener("click", function () {
                loadSavedRequest(req);
            });
            wrap.appendChild(item);
        });
        return wrap;
    }

    function renderHistory() {
        var container = $("#historyList");
        container.innerHTML = "";
        if (!state.history.length) {
            container.appendChild(el("div", "side-empty", "暂无历史"));
            return;
        }
        state.history.forEach(function (item) {
            var row = el("div", "side-item");
            row.appendChild(methodTag(item.method));
            var main = el("div", "item-main");
            var line = el("div", "");
            line.textContent = item.url || "";
            var sub = el("div", "item-sub");
            sub.textContent = (item.statusCode || "-") + " · " + timeAgo(item.createdAt);
            main.appendChild(line);
            main.appendChild(sub);
            var delBtn = el("button", "icon-btn");
            delBtn.title = "删除";
            delBtn.appendChild(svgIcon("M6 6l12 12M18 6L6 18"));
            delBtn.addEventListener("click", function (e) {
                e.stopPropagation();
                removeHistory(item.id);
            });
            row.appendChild(main);
            row.appendChild(delBtn);
            row.addEventListener("click", function () {
                refillFrom(item);
            });
            container.appendChild(row);
        });
    }

    /* ---------- 编辑器状态同步 ---------- */

    function syncEditorFromState() {
        var req = state.request;
        $("#methodSelect").value = req.method;
        applyMethodColor();
        $("#urlInput").value = req.url;

        $$('input[name="bodyType"]').forEach(function (radio) {
            radio.checked = radio.value === req.bodyType;
        });
        toggleBodyAreas();

        $$('input[name="authType"]').forEach(function (radio) {
            radio.checked = radio.value === req.authType;
        });
        toggleAuthAreas();
        $("#bearerToken").value = req.authConfig.token || "";
        $("#basicUsername").value = req.authConfig.username || "";
        $("#basicPassword").value = req.authConfig.password || "";

        $("#rawBodyInput").value = req.rawBody || "";

        renderKv($("#paramsTable"), req.params, {
            keyPlaceholder: "参数名", valuePlaceholder: "值", onChange: updateCounts
        });
        renderKv($("#headersTable"), req.headers, {
            keyPlaceholder: "Header", valuePlaceholder: "值", onChange: updateCounts
        });
        renderKv($("#bodyFormTable"), req.formBody, {
            keyPlaceholder: "字段名", valuePlaceholder: "值", onChange: updateCounts
        });
        updateCounts();
    }

    function updateCounts() {
        var params = state.request.params.filter(function (p) { return p.key; }).length;
        var headers = state.request.headers.filter(function (h) { return h.key; }).length;
        $("#paramsCount").textContent = params ? params : "";
        $("#headersCount").textContent = headers ? headers : "";
    }

    function applyMethodColor() {
        var value = $("#methodSelect").value;
        $("#methodSelect").dataset.method = value;
    }

    function toggleBodyAreas() {
        var type = state.request.bodyType;
        $("#bodyNone").classList.toggle("hidden", type !== "none");
        $("#bodyRaw").classList.toggle("hidden", type !== "raw" && type !== "xml");
        $("#bodyForm").classList.toggle("hidden", type !== "x-www-form-urlencoded" && type !== "form-data");
        $("#rawBodyInput").placeholder = type === "xml"
            ? '<root>\n  <item>lvy</item>\n</root>'
            : '{\n  "name": "lvy"\n}';
    }

    function toggleAuthAreas() {
        var type = state.request.authType;
        $("#authNone").classList.toggle("hidden", type !== "none");
        $("#authBearer").classList.toggle("hidden", type !== "bearer");
        $("#authBasic").classList.toggle("hidden", type !== "basic");
    }

    /* ---------- 请求构造与发送 ---------- */

    function buildProxyPayload() {
        var req = state.request;
        var bodyContent = null;
        if (req.bodyType === "raw" || req.bodyType === "xml") {
            bodyContent = req.rawBody;
        } else if (req.bodyType === "x-www-form-urlencoded" || req.bodyType === "form-data") {
            bodyContent = JSON.stringify(req.formBody.filter(function (p) { return p.key; }));
        }
        var authConfig = {};
        if (req.authType === "bearer") {
            authConfig.token = req.authConfig.token || "";
        } else if (req.authType === "basic") {
            authConfig.username = req.authConfig.username || "";
            authConfig.password = req.authConfig.password || "";
        }
        return {
            method: req.method,
            url: req.url,
            headers: req.headers.filter(function (h) { return h.key; }),
            params: req.params.filter(function (p) { return p.key; }),
            bodyType: req.bodyType,
            bodyContent: bodyContent,
            authType: req.authType,
            authConfig: authConfig
        };
    }

    function sendRequest() {
        if (!state.request.url.trim()) {
            toast("请输入 URL", true);
            $("#urlInput").focus();
            return;
        }
        progressOn();
        $("#sendBtn").disabled = true;
        api("/api/proxy/send", { method: "POST", body: buildProxyPayload() })
            .then(function (data) {
                state.response = data;
                renderResponse();
                loadHistory();
            })
            .catch(function (err) {
                state.response = null;
                renderResponseError(err.message);
                toast(err.message, true);
            })
            .finally(function () {
                progressOff();
                $("#sendBtn").disabled = false;
            });
    }

    /* ---------- 响应渲染 ---------- */

    function statusClass(code) {
        if (code >= 200 && code < 300) return "ok";
        if (code >= 300 && code < 500) return "warn";
        return "error";
    }

    function renderStatusLine(code, duration, size) {
        var wrap = $("#responseStatus");
        wrap.innerHTML = "";
        var dot = el("span", "status-dot " + statusClass(code));
        var text = el("span", "", code + " " + (STATUS_TEXT[code] || ""));
        wrap.appendChild(dot);
        wrap.appendChild(text);
        wrap.appendChild(el("span", "status-sep", "·"));
        wrap.appendChild(el("span", "status-meta", duration + " ms"));
        wrap.appendChild(el("span", "status-sep", "·"));
        wrap.appendChild(el("span", "status-meta", formatBytes(size)));
    }

    function renderResponse() {
        var res = state.response;
        renderStatusLine(res.statusCode, res.durationMs, res.size);

        var pretty = $("#responsePretty");
        var raw = $("#responseRaw");
        pretty.innerHTML = "";
        raw.innerHTML = "";

        var parsed = tryParseJson(res.body);
        if (parsed !== undefined && typeof parsed === "object" && parsed !== null) {
            var tree = el("div", "json-view");
            renderJsonNode(parsed, null, tree, true);
            pretty.appendChild(tree);
        } else if (detectXml(res.contentType, res.body)) {
            var formattedXml = transformXml(res.body, true);
            pretty.appendChild(buildPre(formattedXml !== null ? formattedXml : res.body));
        } else {
            pretty.appendChild(buildPre(res.body));
        }
        raw.appendChild(buildPre(res.body));

        var headersWrap = $("#responseHeaders");
        headersWrap.innerHTML = "";
        var table = el("div", "kv-table");
        (res.headers || []).forEach(function (h) {
            var row = el("div", "kv-row");
            row.style.gridTemplateColumns = "1.2fr 2fr";
            var k = el("div", "kv-cell");
            k.textContent = h.key;
            var v = el("div", "kv-cell");
            v.textContent = h.value;
            v.style.fontFamily = "var(--font-code)";
            v.style.fontSize = "13px";
            row.appendChild(k);
            row.appendChild(v);
            table.appendChild(row);
        });
        headersWrap.appendChild(table);
        switchResponseTab("pretty");
    }

    function renderResponseError(message) {
        var wrap = $("#responseStatus");
        wrap.innerHTML = "";
        wrap.appendChild(el("span", "status-dot error"));
        wrap.appendChild(el("span", "", "请求失败"));
        wrap.appendChild(el("span", "status-sep", "·"));
        wrap.appendChild(el("span", "status-meta", message));
        $("#responsePretty").innerHTML = "";
        $("#responseRaw").innerHTML = "";
        $("#responseHeaders").innerHTML = "";
    }

    function buildPre(text) {
        var pre = document.createElement("pre");
        pre.textContent = text === null || text === undefined ? "" : text;
        return pre;
    }

    function tryParseJson(text) {
        if (!text || typeof text !== "string") return undefined;
        var trimmed = text.trim();
        if (!trimmed) return undefined;
        if (trimmed[0] !== "{" && trimmed[0] !== "[") return undefined;
        try {
            return JSON.parse(trimmed);
        } catch (e) {
            return undefined;
        }
    }

    function reformatJson(text, pretty) {
        if (text === null || text === undefined || !String(text).trim()) return null;
        try {
            var obj = JSON.parse(text);
            return pretty ? JSON.stringify(obj, null, 2) : JSON.stringify(obj);
        } catch (e) {
            return null;
        }
    }

    /* ---------- XML 处理 ---------- */

    function looksLikeXml(text) {
        return !!text && String(text).trim().charAt(0) === "<";
    }

    function isXmlContentType(contentType) {
        return !!contentType && String(contentType).toLowerCase().indexOf("xml") >= 0;
    }

    function parseXmlDocument(text) {
        if (!looksLikeXml(text)) return null;
        try {
            var doc = new DOMParser().parseFromString(String(text).trim(), "application/xml");
            if (!doc || doc.getElementsByTagName("parsererror").length > 0) return null;
            return doc;
        } catch (e) {
            return null;
        }
    }

    function detectXml(contentType, text) {
        if (isXmlContentType(contentType)) return true;
        return parseXmlDocument(text) !== null;
    }

    function transformXml(text, pretty) {
        var doc = parseXmlDocument(text);
        if (!doc) return null;
        removeWhitespaceNodes(doc);
        var source = new XMLSerializer().serializeToString(doc);
        return pretty ? indentXml(source) : source.trim();
    }

    function removeWhitespaceNodes(node) {
        Array.prototype.slice.call(node.childNodes).forEach(function (child) {
            if (child.nodeType === 3) {
                if (!child.nodeValue || !child.nodeValue.trim()) {
                    node.removeChild(child);
                }
            } else if (child.nodeType === 1) {
                removeWhitespaceNodes(child);
            }
        });
    }

    function indentXml(source) {
        var s = String(source).replace(/\r\n/g, "\n").trim();
        var tokens = [];
        var i = 0;
        var n = s.length;
        while (i < n) {
            if (s.charAt(i) !== "<") {
                var next = s.indexOf("<", i);
                if (next < 0) next = n;
                var segment = s.slice(i, next);
                if (segment.trim()) tokens.push({ type: "text", value: segment.trim() });
                i = next;
                continue;
            }
            if (s.indexOf("<!--", i) === i) {
                var endComment = s.indexOf("-->", i + 4);
                if (endComment < 0) endComment = n - 3;
                tokens.push({ type: "line", value: s.slice(i, endComment + 3) });
                i = endComment + 3;
                continue;
            }
            if (s.indexOf("<![CDATA[", i) === i) {
                var endCdata = s.indexOf("]]>", i + 9);
                if (endCdata < 0) endCdata = n - 3;
                tokens.push({ type: "line", value: s.slice(i, endCdata + 3) });
                i = endCdata + 3;
                continue;
            }
            var j = i + 1;
            var quote = null;
            while (j < n) {
                var ch = s.charAt(j);
                if (quote) {
                    if (ch === quote) quote = null;
                } else if (ch === "\"" || ch === "'") {
                    quote = ch;
                } else if (ch === ">") {
                    break;
                }
                j++;
            }
            var tag = s.slice(i, Math.min(j + 1, n));
            var type;
            if (tag.indexOf("</") === 0) type = "close";
            else if (tag.indexOf("<?") === 0 || tag.indexOf("<!") === 0) type = "line";
            else if (/\/>$/.test(tag)) type = "line";
            else type = "open";
            tokens.push({ type: type, value: tag });
            i = j + 1;
        }

        var lines = [];
        var depth = 0;
        tokens.forEach(function (token) {
            if (token.type === "close") {
                depth = Math.max(depth - 1, 0);
                lines.push(xmlIndent(depth) + token.value);
            } else if (token.type === "open") {
                lines.push(xmlIndent(depth) + token.value);
                depth++;
            } else {
                lines.push(xmlIndent(depth) + token.value);
            }
        });
        return lines.join("\n");
    }

    function xmlIndent(depth) {
        return new Array(depth + 1).join("  ");
    }

    function transformRequestBody(pretty) {
        var isXml = state.request.bodyType === "xml";
        var result = isXml
            ? transformXml($("#rawBodyInput").value, pretty)
            : reformatJson($("#rawBodyInput").value, pretty);
        if (result === null) {
            toast(isXml ? "当前内容不是合法 XML" : "当前内容不是合法 JSON", true);
            return;
        }
        $("#rawBodyInput").value = result;
        state.request.rawBody = result;
        toast(pretty ? "已格式化" : "已压缩");
    }

    function transformResponseBody(pretty) {
        if (!state.response) {
            toast("暂无响应内容", true);
            return;
        }
        var isXml = detectXml(state.response.contentType, state.response.body);
        var result = isXml
            ? transformXml(state.response.body, pretty)
            : reformatJson(state.response.body, pretty);
        if (result === null) {
            toast(isXml ? "响应内容不是合法 XML" : "响应内容不是合法 JSON", true);
            return;
        }
        state.response.body = result;
        renderResponse();
        toast(pretty ? "已格式化" : "已压缩");
    }

    function primitiveClass(value) {
        if (value === null) return "json-null";
        if (typeof value === "string") return "json-string";
        if (typeof value === "number") return "json-number";
        return "json-boolean";
    }

    function formatPrimitive(value) {
        if (value === null) return "null";
        if (typeof value === "string") return JSON.stringify(value);
        return String(value);
    }

    function renderJsonNode(value, keyLabel, container, isLast) {
        var isObject = value !== null && typeof value === "object";
        if (!isObject) {
            var line = el("div", "json-line");
            if (keyLabel !== null) {
                line.appendChild(el("span", "json-key", JSON.stringify(keyLabel) + ": "));
            }
            line.appendChild(el("span", primitiveClass(value), formatPrimitive(value)));
            if (!isLast) line.appendChild(el("span", "json-punct", ","));
            container.appendChild(line);
            return;
        }

        var isArray = Array.isArray(value);
        var node = el("div", "json-node");
        var head = el("div", "json-line");
        var toggle = el("span", "json-toggle", "▾");
        head.appendChild(toggle);
        if (keyLabel !== null) {
            head.appendChild(el("span", "json-key", JSON.stringify(keyLabel) + ": "));
        }
        head.appendChild(el("span", "", isArray ? "[" : "{"));
        node.appendChild(head);

        var children = el("div", "json-children");
        children.style.paddingLeft = "16px";
        var entries = isArray
            ? value.map(function (v) { return [null, v]; })
            : Object.keys(value).map(function (k) { return [k, value[k]]; });
        entries.forEach(function (entry, index) {
            renderJsonNode(entry[1], entry[0], children, index === entries.length - 1);
        });
        node.appendChild(children);

        var close = el("div", "json-line");
        close.appendChild(el("span", "", isArray ? "]" : "}"));
        if (!isLast) close.appendChild(el("span", "json-punct", ","));
        node.appendChild(close);

        toggle.addEventListener("click", function () {
            var collapsed = children.classList.toggle("hidden");
            toggle.textContent = collapsed ? "▸" : "▾";
            close.classList.toggle("hidden", collapsed);
        });

        container.appendChild(node);
    }

    /* ---------- 交互绑定 ---------- */

    function switchMainTab(name) {
        $$("#requestTabs .tab").forEach(function (tab) {
            tab.classList.toggle("active", tab.dataset.tab === name);
        });
        $$(".tab-panel").forEach(function (panel) {
            panel.classList.toggle("active", panel.dataset.panel === name);
        });
    }

    function switchResponseTab(name) {
        $$("#responseTabs .tab").forEach(function (tab) {
            tab.classList.toggle("active", tab.dataset.rtab === name);
        });
        $$(".response-panel").forEach(function (panel) {
            panel.classList.toggle("active", panel.dataset.rpanel === name);
        });
    }

    function bindEvents() {
        $$("#requestTabs .tab").forEach(function (tab) {
            tab.addEventListener("click", function () {
                switchMainTab(tab.dataset.tab);
            });
        });
        $$("#responseTabs .tab").forEach(function (tab) {
            tab.addEventListener("click", function () {
                switchResponseTab(tab.dataset.rtab);
            });
        });

        $$("#sidebarNav .nav-item").forEach(function (item) {
            item.addEventListener("click", function () {
                $$("#sidebarNav .nav-item").forEach(function (n) { n.classList.remove("active"); });
                item.classList.add("active");
                $$(".side-view").forEach(function (v) { v.classList.remove("active"); });
                $("#view-" + item.dataset.view).classList.add("active");
            });
        });

        $("#collapseBtn").addEventListener("click", function () {
            $("#sidebar").classList.toggle("collapsed");
        });

        $("#methodSelect").addEventListener("change", function () {
            state.request.method = $("#methodSelect").value;
            applyMethodColor();
        });

        $("#urlInput").addEventListener("input", function () {
            state.request.url = $("#urlInput").value;
        });

        $("#sendBtn").addEventListener("click", sendRequest);
        $("#urlInput").addEventListener("keydown", function (e) {
            if (e.key === "Enter") sendRequest();
        });

        $$('input[name="bodyType"]').forEach(function (radio) {
            radio.addEventListener("change", function () {
                state.request.bodyType = radio.value;
                toggleBodyAreas();
            });
        });

        $$('input[name="authType"]').forEach(function (radio) {
            radio.addEventListener("change", function () {
                state.request.authType = radio.value;
                toggleAuthAreas();
            });
        });

        $("#rawBodyInput").addEventListener("input", function () {
            state.request.rawBody = $("#rawBodyInput").value;
        });

        $("#formatReqBodyBtn").addEventListener("click", function () {
            transformRequestBody(true);
        });
        $("#minifyReqBodyBtn").addEventListener("click", function () {
            transformRequestBody(false);
        });
        $("#formatRespBtn").addEventListener("click", function () {
            transformResponseBody(true);
        });
        $("#minifyRespBtn").addEventListener("click", function () {
            transformResponseBody(false);
        });
        $("#bearerToken").addEventListener("input", function () {
            state.request.authConfig.token = $("#bearerToken").value;
        });
        $("#basicUsername").addEventListener("input", function () {
            state.request.authConfig.username = $("#basicUsername").value;
        });
        $("#basicPassword").addEventListener("input", function () {
            state.request.authConfig.password = $("#basicPassword").value;
        });

        $("#resetBtn").addEventListener("click", function () {
            state.request = defaultRequest();
            state.editingRequestId = null;
            state.response = null;
            renderCollections();
            syncEditorFromState();
            $("#responseStatus").innerHTML = '<span class="status-empty">尚未发送请求</span>';
            $("#responsePretty").innerHTML = "";
            $("#responseHeaders").innerHTML = "";
            $("#responseRaw").innerHTML = "";
        });

        $("#saveBtn").addEventListener("click", openSaveModal);
        $("#newCollectionBtn").addEventListener("click", createCollection);
        $("#clearHistoryBtn").addEventListener("click", clearHistory);

        bindPrompt();
        bindSave();
        bindDivider();
    }

    /* ---------- 分割条拖拽 ---------- */

    function bindDivider() {
        var divider = $("#divider");
        var dragging = false;
        var workspace = $(".workspace");
        var requestPane = $(".request-pane");

        divider.addEventListener("mousedown", function (e) {
            dragging = true;
            divider.classList.add("dragging");
            e.preventDefault();
        });
        document.addEventListener("mousemove", function (e) {
            if (!dragging) return;
            var rect = workspace.getBoundingClientRect();
            var offset = e.clientY - rect.top;
            var min = 120;
            var max = rect.height - 140;
            offset = Math.max(min, Math.min(max, offset));
            requestPane.style.flex = "0 0 " + offset + "px";
        });
        document.addEventListener("mouseup", function () {
            if (!dragging) return;
            dragging = false;
            divider.classList.remove("dragging");
        });
    }

    /* ---------- 集合 ---------- */

    function createCollection() {
        promptDialog("新建集合", "", "集合名称").then(function (name) {
            if (!name) return;
            api("/api/collections", { method: "POST", body: { name: name, description: "" } })
                .then(function () {
                    toast("集合已创建");
                    loadCollections();
                })
                .catch(function (err) { toast(err.message, true); });
        });
    }

    function renameCollection(collection) {
        promptDialog("重命名集合", collection.name, "集合名称").then(function (name) {
            if (!name) return;
            api("/api/collections/" + collection.id, {
                method: "PUT",
                body: { name: name, description: collection.description || "" }
            }).then(function () {
                toast("已重命名");
                loadCollections();
            }).catch(function (err) { toast(err.message, true); });
        });
    }

    function deleteCollection(collection) {
        if (!window.confirm('删除集合 "' + collection.name + '" 及其中的请求？')) return;
        api("/api/collections/" + collection.id, { method: "DELETE" })
            .then(function () {
                if (state.activeCollectionId === collection.id) state.activeCollectionId = null;
                toast("集合已删除");
                loadCollections();
            })
            .catch(function (err) { toast(err.message, true); });
    }

    function toggleCollection(id) {
        if (state.activeCollectionId === id) {
            state.activeCollectionId = null;
            renderCollections();
            return;
        }
        state.activeCollectionId = id;
        api("/api/collections/" + id + "/requests").then(function (requests) {
            state.collectionRequests[id] = requests;
            renderCollections();
        }).catch(function (err) { toast(err.message, true); });
    }

    function loadCollections() {
        return api("/api/collections").then(function (data) {
            state.collections = data || [];
            if (state.activeCollectionId) {
                return api("/api/collections/" + state.activeCollectionId + "/requests").then(function (requests) {
                    state.collectionRequests[state.activeCollectionId] = requests;
                });
            }
        }).then(renderCollections);
    }

    function loadSavedRequest(req) {
        state.editingRequestId = req.id;
        state.request.method = req.method || "GET";
        state.request.url = req.url || "";
        state.request.params = req.params && req.params.length ? req.params : [];
        state.request.headers = req.headers && req.headers.length ? req.headers : [];
        state.request.bodyType = req.bodyType || "none";
        state.request.authType = req.authType || "none";
        state.request.authConfig = req.authConfig || {};
        if (state.request.authConfig.token === "••••••••") state.request.authConfig.token = "";
        if (state.request.authConfig.password === "••••••••") state.request.authConfig.password = "";
        applyBodyContent(req.bodyType, req.bodyContent);
        syncEditorFromState();
        renderCollections();
    }

    function applyBodyContent(bodyType, content) {
        if (bodyType === "raw" || bodyType === "xml") {
            state.request.rawBody = content || "";
            state.request.formBody = [];
        } else if (bodyType === "x-www-form-urlencoded" || bodyType === "form-data") {
            state.request.formBody = parseFormPairs(content);
            state.request.rawBody = "";
        } else {
            state.request.rawBody = "";
            state.request.formBody = [];
        }
    }

    function parseFormPairs(content) {
        if (!content) return [];
        try {
            var parsed = JSON.parse(content);
            if (Array.isArray(parsed)) return parsed;
            if (parsed && typeof parsed === "object") {
                return Object.keys(parsed).map(function (k) {
                    return { key: k, value: parsed[k], enabled: true };
                });
            }
        } catch (e) {
            return [];
        }
        return [];
    }

    function deleteRequest(req) {
        if (!window.confirm('删除请求 "' + req.name + '"？')) return;
        api("/api/requests/" + req.id, { method: "DELETE" }).then(function () {
            if (state.editingRequestId === req.id) state.editingRequestId = null;
            toast("请求已删除");
            loadCollections();
        }).catch(function (err) { toast(err.message, true); });
    }

    /* ---------- 保存请求 ---------- */

    function openSaveModal() {
        if (!state.request.url.trim()) {
            toast("请先填写 URL", true);
            return;
        }
        if (!state.collections.length) {
            toast("请先创建集合", true);
            return;
        }
        var select = $("#saveCollection");
        select.innerHTML = "";
        state.collections.forEach(function (c) {
            var opt = document.createElement("option");
            opt.value = String(c.id);
            opt.textContent = c.name;
            select.appendChild(opt);
        });
        if (state.activeCollectionId) select.value = String(state.activeCollectionId);
        $("#saveName").value = suggestName();
        $("#saveOverlay").classList.remove("hidden");
        $("#saveName").focus();
    }

    function suggestName() {
        try {
            var path = state.request.url.replace(/^https?:\/\//, "").split("?")[0];
            return state.request.method + " " + path.slice(0, 40);
        } catch (e) {
            return "未命名请求";
        }
    }

    function bindSave() {
        $("#saveCancel").addEventListener("click", function () {
            $("#saveOverlay").classList.add("hidden");
        });
        $("#saveNewCollectionBtn").addEventListener("click", function () {
            promptDialog("新建集合", "", "集合名称").then(function (name) {
                if (!name) return;
                api("/api/collections", { method: "POST", body: { name: name, description: "" } })
                    .then(function (created) {
                        return loadCollections().then(function () {
                            openSaveModal();
                            $("#saveCollection").value = String(created.id);
                        });
                    })
                    .catch(function (err) { toast(err.message, true); });
            });
        });
        $("#saveConfirm").addEventListener("click", function () {
            var name = $("#saveName").value.trim();
            var collectionId = Number($("#saveCollection").value);
            if (!name) {
                toast("请输入名称", true);
                return;
            }
            var payload = buildProxyPayload();
            payload.name = name;
            payload.collectionId = collectionId;
            var method = state.editingRequestId ? "PUT" : "POST";
            var path = state.editingRequestId ? "/api/requests/" + state.editingRequestId : "/api/requests";
            api(path, { method: method, body: payload })
                .then(function () {
                    $("#saveOverlay").classList.add("hidden");
                    toast("请求已保存");
                    state.activeCollectionId = collectionId;
                    loadCollections();
                })
                .catch(function (err) { toast(err.message, true); });
        });
    }

    /* ---------- 通用名称输入弹层 ---------- */

    var promptResolver = null;

    function promptDialog(title, defaultValue, placeholder) {
        $("#promptTitle").textContent = title;
        $("#promptInput").value = defaultValue || "";
        $("#promptInput").placeholder = placeholder || "";
        $("#promptOverlay").classList.remove("hidden");
        $("#promptInput").focus();
        return new Promise(function (resolve) {
            promptResolver = resolve;
        });
    }

    function closePrompt(value) {
        $("#promptOverlay").classList.add("hidden");
        if (promptResolver) {
            promptResolver(value);
            promptResolver = null;
        }
    }

    function bindPrompt() {
        $("#promptCancel").addEventListener("click", function () { closePrompt(null); });
        $("#promptConfirm").addEventListener("click", function () {
            var value = $("#promptInput").value.trim();
            closePrompt(value || null);
        });
        $("#promptInput").addEventListener("keydown", function (e) {
            if (e.key === "Enter") {
                var value = $("#promptInput").value.trim();
                closePrompt(value || null);
            }
        });
    }

    /* ---------- 历史 ---------- */

    function loadHistory() {
        return api("/api/history?page=0&size=50").then(function (data) {
            state.history = (data && data.items) || [];
            renderHistory();
        });
    }

    function removeHistory(id) {
        api("/api/history/" + id, { method: "DELETE" }).then(function () {
            state.history = state.history.filter(function (h) { return h.id !== id; });
            renderHistory();
        }).catch(function (err) { toast(err.message, true); });
    }

    function clearHistory() {
        if (!state.history.length) return;
        if (!window.confirm("清空全部历史记录？")) return;
        api("/api/history", { method: "DELETE" }).then(function () {
            state.history = [];
            renderHistory();
            toast("历史已清空");
        }).catch(function (err) { toast(err.message, true); });
    }

    function refillFrom(item) {
        state.editingRequestId = null;
        state.request.method = item.method || "GET";
        state.request.url = item.url || "";
        state.request.params = item.params || [];
        state.request.headers = item.headers || [];
        state.request.bodyType = item.bodyType || "none";
        state.request.authType = item.authType || "none";
        state.request.authConfig = item.authConfig || {};
        applyBodyContent(item.bodyType, item.bodyContent);
        syncEditorFromState();
        renderCollections();
        switchMainTab("params");
        toast("已回填到编辑器");
    }

    /* ---------- 初始化 ---------- */

    function init() {
        bindEvents();
        syncEditorFromState();
        loadCollections().catch(function (err) { toast(err.message, true); });
        loadHistory().catch(function () { });
    }

    document.addEventListener("DOMContentLoaded", init);
})();
