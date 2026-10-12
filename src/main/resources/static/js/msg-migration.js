/* lvy - 报文迁移页面逻辑 */
(function () {
    "use strict";

    var $ = function (sel) {
        return document.querySelector(sel);
    };

    var state = {
        page: 0,
        size: 20,
        keyword: "",
        currentId: null
    };

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

    function progressOn() {
        $("#progress").classList.add("active");
    }

    function progressOff() {
        $("#progress").classList.remove("active");
    }

    function toast(message, isError) {
        var node = document.createElement("div");
        node.className = "cm-toast" + (isError ? " error" : "");
        node.textContent = message;
        $("#toastRoot").appendChild(node);
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

    function formatTime(ms) {
        if (ms === null || ms === undefined) return "-";
        var date = new Date(Number(ms));
        if (isNaN(date.getTime())) return String(ms);
        return date.toLocaleString("zh-CN", { hour12: false });
    }

    /* ---------- 采集 ---------- */

    function collect() {
        var serviceCode = $("#serviceCodeInput").value.trim();
        var limit = parseInt($("#limitInput").value, 10);
        if (!serviceCode) {
            toast("请输入服务码", true);
            return;
        }
        if (!(limit >= 1 && limit <= 1000)) {
            toast("数量需在 1-1000 之间", true);
            return;
        }
        $("#collectBtn").disabled = true;
        progressOn();
        api("/api/msg-migration/collect", { method: "POST", body: { serviceCode: serviceCode, limit: limit } })
            .then(function (result) {
                showCollectResult(result);
                state.keyword = serviceCode;
                state.page = 0;
                $("#filterInput").value = serviceCode;
                return loadSamples();
            })
            .catch(function (err) {
                toast(err.message, true);
            })
            .finally(function () {
                $("#collectBtn").disabled = false;
                progressOff();
            });
    }

    function showCollectResult(result) {
        var box = $("#collectResult");
        box.classList.remove("hidden");
        box.innerHTML = "";
        var byType = result.collectedByType || {};
        var summary = document.createElement("div");
        summary.textContent = "服务码 " + result.serviceCode
            + "：bzjson " + (byType.bzjson || 0) + " 条、soap " + (byType.soap || 0) + " 条"
            + "（共 " + result.collected + " 条），跳过重复 " + result.skipped
            + " 条，扫描分区 " + result.scannedPartitions + " 个"
            + (result.completed ? "（两类均已达到目标）" : "（已扫描全部分区）");
        box.appendChild(summary);
        if (result.hitPartitions && result.hitPartitions.length) {
            var hit = document.createElement("div");
            hit.textContent = "命中分区：" + result.hitPartitions.join("、");
            box.appendChild(hit);
        }
    }

    /* ---------- 列表 ---------- */

    function loadSamples() {
        var query = "?page=" + state.page + "&size=" + state.size;
        if (state.keyword) {
            query += "&keyword=" + encodeURIComponent(state.keyword);
        }
        return api("/api/msg-migration/samples" + query)
            .then(renderSamples)
            .catch(function (err) {
                toast(err.message, true);
            });
    }

    function renderSamples(page) {
        var tbody = $("#sampleTableBody");
        tbody.innerHTML = "";
        var items = page.items || [];
        if (!items.length) {
            var emptyRow = document.createElement("tr");
            var emptyCell = document.createElement("td");
            emptyCell.colSpan = 7;
            emptyCell.className = "cm-empty";
            emptyCell.textContent = "暂无数据";
            emptyRow.appendChild(emptyCell);
            tbody.appendChild(emptyRow);
        } else {
            items.forEach(function (item) {
                var row = document.createElement("tr");
                if (item.id === state.currentId) {
                    row.classList.add("active");
                }
                appendCell(row, item.id);
                appendCell(row, item.serviceCode);
                appendCell(row, item.partitionName);
                appendCell(row, item.transId);
                appendCell(row, formatTime(item.txnTime));
                appendCell(row, item.messageType);
                appendCell(row, item.edited ? "是" : "");
                row.addEventListener("click", function () {
                    openDetail(item.id);
                });
                tbody.appendChild(row);
            });
        }
        var totalPages = Math.max(1, Math.ceil((page.total || 0) / (page.size || state.size)));
        $("#pageInfo").textContent = "第 " + ((page.page || 0) + 1) + "/" + totalPages + " 页 · 共 " + (page.total || 0) + " 条";
        $("#prevBtn").disabled = state.page <= 0;
        $("#nextBtn").disabled = state.page + 1 >= totalPages;
    }

    function appendCell(row, value) {
        var cell = document.createElement("td");
        cell.textContent = value === null || value === undefined ? "" : String(value);
        row.appendChild(cell);
    }

    /* ---------- 详情与修改 ---------- */

    function openDetail(id) {
        api("/api/msg-migration/samples/" + id)
            .then(function (sample) {
                state.currentId = sample.id;
                $("#detailTitle").textContent = "#" + sample.id + " " + sample.serviceCode;
                renderMeta(sample);
                $("#contentArea").value = sample.content || "";
                highlightRow(sample.id);
            })
            .catch(function (err) {
                toast(err.message, true);
            });
    }

    function renderMeta(sample) {
        var meta = $("#detailMeta");
        meta.innerHTML = "";
        var fields = [
            ["服务码", sample.serviceCode],
            ["分区", sample.partitionName],
            ["流水号", sample.transId],
            ["来源IP", sample.sourceIp],
            ["交易时间", formatTime(sample.txnTime)],
            ["类型", sample.messageType],
            ["全局流水号", sample.globalSeqNo],
            ["柜员号", sample.tranTellerNo],
            ["修改过", sample.edited ? "是" : "否"],
            ["更新时间", sample.updatedAt]
        ];
        fields.forEach(function (pair) {
            var item = document.createElement("div");
            var key = document.createElement("div");
            key.className = "cm-meta-k";
            key.textContent = pair[0];
            var value = document.createElement("div");
            value.className = "cm-meta-v";
            value.textContent = pair[1] === null || pair[1] === undefined ? "-" : String(pair[1]);
            item.appendChild(key);
            item.appendChild(value);
            meta.appendChild(item);
        });
    }

    function highlightRow(id) {
        Array.prototype.slice.call($("#sampleTableBody").children).forEach(function (row) {
            row.classList.remove("active");
        });
        Array.prototype.slice.call($("#sampleTableBody").children).forEach(function (row) {
            if (row.firstChild && row.firstChild.textContent === String(id)) {
                row.classList.add("active");
            }
        });
    }

    function saveDetail() {
        if (state.currentId === null) {
            toast("请先选择报文", true);
            return;
        }
        $("#saveBtn").disabled = true;
        api("/api/msg-migration/samples/" + state.currentId, {
            method: "PUT",
            body: { content: $("#contentArea").value }
        }).then(function (sample) {
            renderMeta(sample);
            toast("已保存");
            return loadSamples();
        }).catch(function (err) {
            toast(err.message, true);
        }).finally(function () {
            $("#saveBtn").disabled = false;
        });
    }

    function formatContent() {
        var area = $("#contentArea");
        var text = area.value;
        if (!text || !text.trim()) {
            toast("内容为空", true);
            return;
        }
        var trimmed = text.trim();
        if (trimmed.charAt(0) === "{") {
            try {
                area.value = JSON.stringify(JSON.parse(trimmed), null, 2);
                toast("已格式化 JSON");
                return;
            } catch (e) {
                toast("内容不是合法 JSON", true);
                return;
            }
        }
        if (trimmed.charAt(0) === "<") {
            var formatted = prettyXml(trimmed);
            if (formatted) {
                area.value = formatted;
                toast("已格式化 XML");
            } else {
                toast("内容不是合法 XML", true);
            }
            return;
        }
        toast("暂不支持该内容格式", true);
    }

    function prettyXml(text) {
        try {
            var doc = new DOMParser().parseFromString(text, "application/xml");
            if (!doc || doc.getElementsByTagName("parsererror").length) {
                return null;
            }
            var source = new XMLSerializer().serializeToString(doc).replace(/>\s*</g, "><");
            var tokens = source.replace(/(>)(<)/g, "$1\n$2").split("\n");
            var depth = 0;
            var lines = [];
            tokens.forEach(function (token) {
                if (/^<\/[^>]+>$/.test(token)) {
                    depth = Math.max(depth - 1, 0);
                }
                lines.push(new Array(depth + 1).join("  ") + token);
                if (/^<[^!?/][^>]*[^/]>$/.test(token) && !/<\/[^>]+>$/.test(token)) {
                    depth++;
                }
            });
            return lines.join("\n");
        } catch (e) {
            return null;
        }
    }

    /* ---------- 事件 ---------- */

    function bind() {
        $("#collectBtn").addEventListener("click", collect);
        $("#refreshBtn").addEventListener("click", function () {
            state.keyword = $("#filterInput").value.trim();
            state.page = 0;
            loadSamples();
        });
        $("#filterInput").addEventListener("keydown", function (e) {
            if (e.key === "Enter") {
                state.keyword = $("#filterInput").value.trim();
                state.page = 0;
                loadSamples();
            }
        });
        $("#prevBtn").addEventListener("click", function () {
            if (state.page > 0) {
                state.page--;
                loadSamples();
            }
        });
        $("#nextBtn").addEventListener("click", function () {
            state.page++;
            loadSamples();
        });
        $("#saveBtn").addEventListener("click", saveDetail);
        $("#formatBtn").addEventListener("click", formatContent);
    }

    bind();
    loadSamples();
})();
