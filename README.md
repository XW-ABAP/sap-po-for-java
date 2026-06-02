Just for fun


# SAP PO REST 接收方通道配置与测试操作手册

本手册分为三个部分：REST 通道配置更新、底层网络 Ping 测试，以及全链路业务测试指南。

---

## 1. 接收方 REST 通道配置更新

**目标**：将经过 Java UDF 处理后写入的 `token` 和 `userid` 属性提取为变量，并在 HTTP Headers 中作为原生字段直接发送（移除原有的 `Bearer` 前缀）。

### 步骤 1.1：修改【REST URL】页签 (提取变量)

删除原有的 `accessToken` 变量配置，并新增以下两个 **Pattern Variable Replacement** 规则：

* **变量 1：提取 Token**
* **Value Source:** Adapter-Specific Attribute
* **Pattern Element Name:** `var_token`
* **Adapter-Specific Attribute:** Custom Attribute
* **Attribute Name:** `token` *(注意：必须与新代码里写入的属性名完全一致)*


* **变量 2：提取 UserID**
* *(点击左侧的 `+` 号添加新行)*
* **Value Source:** Adapter-Specific Attribute
* **Pattern Element Name:** `var_userid`
* **Adapter-Specific Attribute:** Custom Attribute
* **Attribute Name:** `userid` *(注意：必须与新代码里写入的属性名完全一致)*



### 步骤 1.2：修改【HTTP Headers】页签 (拼装请求头)

删除原有的 `Authorization` 行，保留原有的 `Content-Type: application/json`。根据目标系统要求，添加以下三行 Header 配置：

| Header Name | Value Pattern | 说明 |
| --- | --- | --- |
| `token` | `{var_token}` | 动态取值。注意：**无需** `Bearer` 前缀 |
| `userid` | `{var_userid}` | 动态取值 |
| `appid` | `dangan_sap` | 固定值 |

**完成上述配置后，请保存并激活 (Activate) 该通道。**

---

## 2. 通道网络连通性测试 (Ping 测试)

**目标**：验证 PO 服务器与目标地址的底层网络（域名解析、443 端口、SSL 证书握手）是否连通。

### 步骤 2.1：进入通信通道监控器

1. 登录 SAP PO 主页（URL 通常为 `http://<PO服务器IP>:<端口>/dir` 或 `/pimon`）。
2. 点击进入 **Configuration and Monitoring Home** (NWA - 配置和监控主页)。
3. 导航路径：**Monitoring** (监控) -> **Adapter Engine** (适配器引擎) -> **Communication Channel Monitor** (通信通道监控)。
* *(快捷方式：直接点击带有“放大镜和扳手”图标的通信通道监控器)*



### 步骤 2.2：执行 Ping 操作

1. 在界面的 **Channel** (通道) 搜索过滤框中，输入通道名称：`CC_REST_FI080_RECEIVER`。
2. 点击 **Go** (执行) 按钮进行搜索。
3. 在下方结果列表中，单击选中该通道记录。
4. 点击界面上的 **Ping** 按钮。
5. 系统将返回 Ping 结果日志。若显示为绿色，则证明网络层已连通。

---

## 3. 全链路业务连通性测试指南

⚠️ **重要提示：Ping 测试的局限性**
REST 通道的 Ping 功能**仅限底层网络探测**。它**不会**执行 Java UDF 代码，**不会**获取 Token 或进行 RSA 加密，也**不会**执行目标系统的业务鉴权。

要验证动态 Header 与 Token 逻辑是否真正生效，必须触发真实的报文转换。请从以下两种方式中选择：

### 方法 A：源系统直发（首选推荐）

1. 通知前端源系统（例如 S/4HANA 的 ABAP 侧）触发一条真实的测试数据。
2. 在 PO 监控平台点击左上角的 **Message Monitor** (消息监控器)。
3. 追踪该消息日志。若成功，目标系统将接收到数据；若失败，直接查看 **Message Log**，定位具体报错节点（例如 Java 映射报错或目标系统返回 401 鉴权失败）。

### 方法 B：使用 Send Test Message 工具

1. 在 NWA 的监控页签下，打开 **Send Test Message** (发送测试消息) 工具。
2. 手动选择对应的 Sender 和 Receiver 接口配置。
3. 粘贴一段符合目标数据结构（如 `DT_FI180_REQ`）的假冒 XML 报文。
4. 点击 **Send** 发送，模拟完整的数据转换和发送流程以查看结果。
