(() => {
  const healthStatus = document.getElementById("healthStatus");
  const realtimeForm = document.getElementById("realtimeForm");
  const realtimeType = document.getElementById("realtimeType");
  const realtimeKey = document.getElementById("realtimeKey");
  const realtimeMeta = document.getElementById("realtimeMeta");
  const realtimeTable = document.getElementById("realtimeTable");
  const batchRunBtn = document.getElementById("batchRunBtn");
  const batchLoadBtn = document.getElementById("batchLoadBtn");
  const mybatisForm = document.getElementById("mybatisForm");
  const mbBusinessKey = document.getElementById("mbBusinessKey");
  const mbJobName = document.getElementById("mbJobName");
  const mbSourceTable = document.getElementById("mbSourceTable");
  const batchMeta = document.getElementById("batchMeta");
  const batchTable = document.getElementById("batchTable");
  const errorBox = document.getElementById("errorBox");

  function showError(message) {
    errorBox.hidden = !message;
    errorBox.textContent = message || "";
  }

  function escapeHtml(value) {
    return String(value)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;");
  }

  function flattenRow(row, prefix = "") {
    const flat = {};
    if (row == null || typeof row !== "object" || Array.isArray(row)) {
      flat[prefix || "value"] = row;
      return flat;
    }
    Object.entries(row).forEach(([key, value]) => {
      const next = prefix ? `${prefix}.${key}` : key;
      if (value != null && typeof value === "object" && !Array.isArray(value)) {
        Object.assign(flat, flattenRow(value, next));
      } else if (Array.isArray(value)) {
        flat[next] = JSON.stringify(value);
      } else {
        flat[next] = value;
      }
    });
    return flat;
  }

  function parsePayload(payloadJson) {
    if (payloadJson == null || payloadJson === "") {
      return {};
    }
    if (typeof payloadJson === "object") {
      return flattenRow(payloadJson);
    }
    try {
      return flattenRow(JSON.parse(payloadJson));
    } catch (e) {
      return { payload: payloadJson };
    }
  }

  function renderTable(container, rows) {
    if (!rows || rows.length === 0) {
      container.innerHTML = '<p class="empty">표시할 데이터가 없습니다.</p>';
      return;
    }

    const columns = [];
    rows.forEach((row) => {
      Object.keys(row).forEach((key) => {
        if (!columns.includes(key)) {
          columns.push(key);
        }
      });
    });

    const head = columns.map((col) => `<th>${escapeHtml(col)}</th>`).join("");
    const body = rows
      .map((row) => {
        const cells = columns
          .map((col) => {
            const value = row[col];
            const text = value == null ? "" : String(value);
            return `<td>${escapeHtml(text)}</td>`;
          })
          .join("");
        return `<tr>${cells}</tr>`;
      })
      .join("");

    container.innerHTML = `<table><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table>`;
  }

  async function fetchJson(url, options) {
    const response = await fetch(url, options);
    const text = await response.text();
    let data = null;
    try {
      data = text ? JSON.parse(text) : null;
    } catch (e) {
      throw new Error(`JSON 파싱 실패: ${text.slice(0, 120)}`);
    }
    if (!response.ok) {
      const message = data && data.message ? data.message : `HTTP ${response.status}`;
      throw new Error(message);
    }
    return data;
  }

  async function loadHealth() {
    try {
      const health = await fetchJson("/api/health");
      healthStatus.textContent = `status=${health.status} · client=${health.sapClient} · system=${health.systemId}`;
    } catch (e) {
      healthStatus.textContent = `상태 확인 실패: ${e.message}`;
    }
  }

  async function loadRealtime(event) {
    event.preventDefault();
    showError("");
    const key = realtimeKey.value.trim();
    if (!key) {
      showError("조회 키를 입력하세요.");
      return;
    }

    const type = realtimeType.value;
    const url =
      type === "salesOrder"
        ? `/api/realtime/sales-orders/${encodeURIComponent(key)}`
        : `/api/realtime/materials/${encodeURIComponent(key)}`;

    try {
      const data = await fetchJson(url);
      const rows = Array.isArray(data.rows) ? data.rows.map((row) => flattenRow(row)) : [];
      realtimeMeta.hidden = false;
      realtimeMeta.textContent = `mode=${data.mode} · function=${data.functionName} · fetchedAt=${data.fetchedAt} · ${rows.length}건`;
      renderTable(realtimeTable, rows);
    } catch (e) {
      realtimeMeta.hidden = true;
      realtimeTable.innerHTML = "";
      showError(e.message);
    }
  }

  function toTableRows(records) {
    return (records || []).map((item) => {
      const payload = parsePayload(item.payloadJson);
      return {
        id: item.id,
        businessKey: item.businessKey,
        sourceTable: item.sourceTable,
        jobName: item.jobName,
        extractedAt: item.extractedAt,
        ...payload,
      };
    });
  }

  async function loadBatchRecords() {
    showError("");
    try {
      const records = await fetchJson("/api/mybatis/extracts?limit=50");
      const rows = toTableRows(records);
      batchMeta.hidden = false;
      batchMeta.textContent = `MyBatis 조회 ${rows.length}건 · payloadJson 파싱 완료`;
      renderTable(batchTable, rows);
    } catch (e) {
      batchMeta.hidden = true;
      batchTable.innerHTML = "";
      showError(e.message);
    }
  }

  async function searchMyBatis(event) {
    event.preventDefault();
    showError("");
    const params = new URLSearchParams();
    if (mbBusinessKey.value.trim()) params.set("businessKey", mbBusinessKey.value.trim());
    if (mbJobName.value.trim()) params.set("jobName", mbJobName.value.trim());
    if (mbSourceTable.value.trim()) params.set("sourceTable", mbSourceTable.value.trim());
    params.set("limit", "50");
    try {
      const records = await fetchJson(`/api/mybatis/extracts?${params.toString()}`);
      const rows = toTableRows(records);
      batchMeta.hidden = false;
      batchMeta.textContent = `MyBatis 동적 SQL ${rows.length}건 · ${params.toString() || "조건 없음"}`;
      renderTable(batchTable, rows);
    } catch (e) {
      batchMeta.hidden = true;
      batchTable.innerHTML = "";
      showError(e.message);
    }
  }

  async function runBatch() {
    showError("");
    batchRunBtn.disabled = true;
    try {
      const result = await fetchJson("/api/batch/run", { method: "POST" });
      batchMeta.hidden = false;
      batchMeta.textContent = `status=${result.status} · read=${result.readCount} · write=${result.writeCount} · stored=${result.storedCount} · access=${result.access}`;
      await loadBatchRecords();
    } catch (e) {
      showError(e.message);
    } finally {
      batchRunBtn.disabled = false;
    }
  }

  realtimeType.addEventListener("change", () => {
    realtimeKey.value = realtimeType.value === "salesOrder" ? "0000000001" : "10000001";
  });

  realtimeForm.addEventListener("submit", loadRealtime);
  batchRunBtn.addEventListener("click", runBatch);
  batchLoadBtn.addEventListener("click", loadBatchRecords);
  mybatisForm.addEventListener("submit", searchMyBatis);

  loadHealth();
  loadRealtime({ preventDefault() {} });
  loadBatchRecords();
})();
