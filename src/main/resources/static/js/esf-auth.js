/* lvy - ESF 验证信息弹层逻辑 */
(function () {
    "use strict";

    function $(sel) {
        return document.querySelector(sel);
    }

    function open() {
        var result = $("#esfAuthResult");
        if (result) {
            result.classList.add("hidden");
            result.innerHTML = "";
        }
        $("#esfAuthOverlay").classList.remove("hidden");
        $("#esfEnvId").focus();
    }

    function close() {
        $("#esfAuthOverlay").classList.add("hidden");
    }

    function renderMessage(message) {
        var box = $("#esfAuthResult");
        box.classList.remove("hidden");
        box.innerHTML = "";
        var node = document.createElement("div");
        node.className = "esf-msg";
        node.textContent = message;
        box.appendChild(node);
    }

    function appendRow(box, label, value) {
        var row = document.createElement("div");
        row.className = "esf-row";

        var key = document.createElement("span");
        key.className = "esf-k";
        key.textContent = label;

        var val = document.createElement("span");
        val.className = "esf-v";
        val.textContent = value === null || value === undefined ? "" : String(value);

        var copy = document.createElement("button");
        copy.className = "mini-btn";
        copy.type = "button";
        copy.textContent = "复制";
        copy.addEventListener("click", function () {
            var text = val.textContent;
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(text).then(function () {
                    copy.textContent = "已复制";
                    setTimeout(function () {
                        copy.textContent = "复制";
                    }, 1200);
                });
            }
        });

        row.appendChild(key);
        row.appendChild(val);
        row.appendChild(copy);
        box.appendChild(row);
    }

    function renderResult(data) {
        var box = $("#esfAuthResult");
        box.classList.remove("hidden");
        box.innerHTML = "";
        appendRow(box, "protocol_id", data.protocolId);
        appendRow(box, "sid", data.sid);
        appendRow(box, "authContent", data.authContent);
    }

    function generate() {
        var environmentId = $("#esfEnvId").value.trim();
        var messageType = $("#esfMessageType").value;
        if (!environmentId) {
            renderMessage("请输入环境 ID");
            return;
        }
        var button = $("#esfAuthGenerate");
        button.disabled = true;
        fetch("/api/esf-auth/generate", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ environmentId: environmentId, messageType: messageType })
        }).then(function (res) {
            return res.json().catch(function () {
                throw new Error("响应解析失败");
            });
        }).then(function (payload) {
            if (!payload || typeof payload.code === "undefined") {
                throw new Error("返回格式异常");
            }
            if (payload.code !== 0) {
                throw new Error(payload.message || "生成失败");
            }
            renderResult(payload.data);
        }).catch(function (err) {
            renderMessage(err.message);
        }).finally(function () {
            button.disabled = false;
        });
    }

    function bind() {
        var button = $("#esfAuthBtn");
        if (!button) {
            return;
        }
        button.addEventListener("click", open);
        $("#esfAuthClose").addEventListener("click", close);
        $("#esfAuthGenerate").addEventListener("click", generate);
        $("#esfAuthOverlay").addEventListener("click", function (e) {
            if (e.target === this) {
                close();
            }
        });
        $("#esfEnvId").addEventListener("keydown", function (e) {
            if (e.key === "Enter") {
                generate();
            }
        });
    }

    bind();
})();
