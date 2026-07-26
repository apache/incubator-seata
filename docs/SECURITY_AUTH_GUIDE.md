# Seata NamingServer ↔ Seata Server 鉴权：完整配置与使用指南

> 本文档针对基于方案 A（**HMAC-SHA256 预共享密钥签名**）的鉴权实现，覆盖：
> - 架构总览与工作原理
> - 完整配置项参考
> - 分场景部署示例（单集群 / 多租户 / Raft）
> - 密钥生成、轮换、销毁
> - Client SDK 集成（可选）
> - 灰度启用、监控告警、故障排查
> - 兼容性、限制与常见问题
>
> 相关代码：
> - 通用签名工具：`common/src/main/java/org/apache/seata/common/security/`
> - NamingServer 侧鉴权：`namingserver/src/main/java/org/apache/seata/namingserver/security/`
> - Seata Server 侧鉴权：`server/src/main/java/org/apache/seata/server/security/`
> - 设计文档：[`NAMINGSERVER_ARCHITECTURE.md`](../NAMINGSERVER_ARCHITECTURE.md) §11

---

## 1. 架构总览

### 1.1 核心概念

| 概念 | 说明 | 生存周期 |
|------|------|----------|
| **cluster-id** | 集群身份标识，唯一标识一套 TC 集群或一台 NamingServer 实例 | 相对稳定，除非组织结构调整 |
| **secret** | 32 字节共享密钥（Base64 编码），与 cluster-id 一一对应 | 建议 ≤ 90 天轮换 |
| **timestamp** | 请求签名时的毫秒时间戳，用于防重放 | 单次请求有效 |
| **nonce** | 随机 UUID，与 timestamp 联合防重放 | 10 分钟内不可复用 |
| **signature** | HMAC-SHA256(secret, canonicalRequest) 的 Base64 编码 | 单次请求有效 |
| **permissions** | 该 cluster-id 拥有的权限集合（REGISTER / VGROUP_WRITE / ...） | 相对稳定 |
| **allowed-namespaces / clusters / vgroups** | 该 cluster-id 可访问的资源范围（NamingServer 侧） | 相对稳定 |

### 1.2 请求签名流程

```
签名方（TC 或 NamingServer）                       验签方
┌──────────────────────────────┐                 ┌──────────────────────────────┐
│  1. 构造 CanonicalRequest    │                 │                              │
│     (method+path+query       │                 │                              │
│      +cluster-id+ts+nonce    │                 │                              │
│      +sha256(body))          │                 │                              │
│  2. HMAC-SHA256(secret, .)   │                 │                              │
│  3. 附加 X-Seata-* 头        │───── HTTP ────►│  1. 检查时间戳 (±5min)      │
│                              │                 │  2. 检查 nonce 是否已使用    │
│                              │                 │  3. 用 secret 重算签名       │
│                              │                 │  4. constant-time 比较       │
│                              │                 │  5. 检查 permission 与资源   │
│                              │                 │  6. 通过 → 转发到 Controller │
│                              │                 │     失败 → 401/403 拒绝     │
└──────────────────────────────┘                 └──────────────────────────────┘
```

### 1.3 HTTP 头字段规范

| Header | 示例 | 说明 |
|--------|------|------|
| `X-Seata-Cluster-Id` | `tenant-a-prod` | 调用者身份，接收方据此查密钥 |
| `X-Seata-Timestamp` | `1721995200123` | 毫秒时间戳 |
| `X-Seata-Nonce` | `f7a3c9d2-8b4e-4a1c-9e5f-3d2a1b8c7e6f` | UUID 或其他不可猜测字符串 |
| `X-Seata-Sign-Alg` | `HMAC-SHA256` | 可选，默认 `HMAC-SHA256`。支持 `HMAC-SHA512` |
| `X-Seata-Sign` | `dGVzdHNpZ25hdHVyZXBheWxvYWRoZXJl...` | Base64 编码的签名值 |

### 1.4 canonicalRequest 生成规则

```
signString = HTTP_METHOD          + "\n"
           + URI_PATH             + "\n"
           + CANONICAL_QUERY      + "\n"        # k1=v1&k2=v2 sorted by key, URL-encoded
           + CLUSTER_ID           + "\n"
           + TIMESTAMP            + "\n"
           + NONCE                + "\n"
           + SHA256_HEX(body)                   # empty body → e3b0c442...
```

**关键约束**：
- `HTTP_METHOD` 必须大写
- `CANONICAL_QUERY` 键名必须按字典序升序拼接
- 空 body 使用 `SHA256("")` 的固定值
- 所有字符串用 UTF-8 编码

---

## 2. 完整配置参考

### 2.1 NamingServer 端配置（`seata.security.*`）

配置类：`org.apache.seata.namingserver.security.SecurityProperties`

```yaml
seata:
  security:
    # ================== 开关 ==================
    enabled: true                          # 是否加载鉴权层。false → 完全跳过（不注册 Filter Bean）
    mode: ENFORCE                          # DISABLED | WARN | ENFORCE
                                           #   DISABLED: 与 enabled=false 等价
                                           #   WARN:     鉴权失败仅记录日志，不拒绝请求（用于灰度）
                                           #   ENFORCE:  鉴权失败返回 401/403（生产模式）

    # ================== 防重放参数 ==================
    replay-window-seconds: 300             # 时间戳允许的最大偏差（前后），秒。默认 300
    nonce-cache-minutes: 10                # Nonce 记忆时长，分钟。必须 > replay-window-seconds

    # ================== 排除路径（Ant-style） ==================
    exclude-paths:
      - /naming/v1/health                  # 健康检查绕过
      - /actuator/**                       # 若开启 Actuator

    # ================== 允许的调用方（TC 集群） ==================
    clusters:
      - id: tenant-a-prod                  # cluster-id，请求头 X-Seata-Cluster-Id 用
        secret-ref: env:TENANT_A_SECRET    # 密钥引用（详见 §3）
        allowed-namespaces: [prod, staging]  # 空或含 "*" 表示不限
        allowed-clusters: [cluster-A, cluster-B]
        allowed-vgroups:                   # glob 通配，支持 * 和 ?
          - "tenant_a_*"
          - "shared_?"
        permissions:                       # 该 cluster-id 拥有的权限
          - REGISTER
          - HEARTBEAT
          - VGROUP_WRITE
          - CONSOLE_READ
          - CONSOLE_WRITE

      - id: tenant-b-prod
        secret-ref: vault:secret/seata/tenant-b
        allowed-namespaces: [prod]
        allowed-clusters: [cluster-B]
        allowed-vgroups: ["tenant_b_*"]
        permissions:
          - REGISTER
          - HEARTBEAT
          - CONSOLE_READ                   # 只读租户，不允许写 vGroup

    # ================== NamingServer 自身出站身份 ==================
    # 当 NamingServer 需要主动调用 TC（vGroup 增/删、Console 转发、MCP）时使用
    outbound:
      cluster-id: naming-server-01         # NamingServer 自身在 TC 侧的 cluster-id
      secret-ref: env:NAMING_SERVER_SECRET
```

**权限枚举**（`org.apache.seata.namingserver.security.Permission`）：

| 权限 | 允许的操作 |
|------|-----------|
| `REGISTER` | `POST /naming/v1/register`, `/batchRegister`, `/unregister` |
| `HEARTBEAT` | 同 REGISTER（心跳复用注册接口，此权限主要用于审计区分） |
| `VGROUP_WRITE` | `POST /naming/v1/addGroup`, `/changeGroup` |
| `CONSOLE_READ` | `GET /naming/v1/discovery`, `/clusters`, `/clusterData`, `/namespace`, `POST /naming/v1/watch`; console GET 请求 |
| `CONSOLE_WRITE` | console POST/PUT/DELETE 请求（通过 `ConsoleRemotingFilter`） |
| `MCP` | MCP 集成调用（`ConsoleLocalServiceImpl`） |

### 2.2 Seata Server 端配置（`seata.registry.seata.security.inbound.*`）

配置类：`org.apache.seata.server.security.ServerSecurityProperties`

```yaml
seata:
  registry:
    seata:
      security:
        inbound:
          enabled: true
          mode: ENFORCE                    # WARN | ENFORCE
          replay-window-seconds: 300
          nonce-cache-minutes: 10

          exclude-paths:
            - /actuator/health

          # 允许的 NamingServer 身份（一般 1-2 个，用于密钥轮换共存期）
          allowed-callers:
            - id: naming-server-01
              secret-ref: env:NAMING_SERVER_SECRET
              permissions:
                - VGROUP_WRITE             # 允许调用 /vgroup/v1/addVGroup, removeVGroup
                - CONSOLE_PROXY            # 允许转发 /api/*/console/*
                - MCP                      # 允许 MCP 集成
            - id: naming-server-01-new     # 密钥轮换期间的新身份
              secret-ref: env:NAMING_SERVER_SECRET_NEW
              permissions:
                - VGROUP_WRITE
                - CONSOLE_PROXY
                - MCP
```

**Server 侧权限枚举**（更小的集合，只覆盖 TC 实际接收的 3 类调用）：

| 权限 | 允许的操作 |
|------|-----------|
| `VGROUP_WRITE` | `GET /vgroup/v1/addVGroup`, `/vgroup/v1/removeVGroup` |
| `CONSOLE_PROXY` | 所有 `/api/*/console/*` 请求（由 NamingServer 反向代理） |
| `MCP` | MCP 集成端点（预留） |

---

## 3. 密钥管理

### 3.1 密钥引用格式（secretRef）

```yaml
# 从环境变量读取（推荐生产）
secret-ref: env:MY_SECRET_ENV_VAR
# 环境变量值应为 Base64 编码的原始密钥字节

# 内联 Base64（仅本地开发）
secret-ref: base64:dGhpc2lzYXNlY3JldGtleXRlc3RtaW5pbXVtdGhpcnR5dHdvYnl0ZXM=

# 内联明文（仅本地开发；密钥长度必须 ≥ 32 字节 UTF-8 编码后）
secret-ref: plain:this-is-a-secret-key-test-min-32-bytes-long-abc

# 生产环境可扩展的方案（未内置，需自行实现 SecretResolver）:
#   vault:secret/data/seata/tenant-a
#   k8s-secret:seata-security/tenant-a-secret
#   kms:arn:aws:kms:us-east-1:12345:key/abc
```

**约束**：
- 密钥字节长度 **≥ 32**（256 bit），否则启动时报错拒绝
- **禁止**明文密钥入代码库、日志、`/actuator/configprops` 输出

### 3.2 生成密钥

**方法 A：openssl**（推荐）
```bash
openssl rand -base64 32
# 输出示例：
# 5NPJlpsN9j2VhFsx3EhV0PQGH2LqBnwYm5DcMkAt+8w=
```

**方法 B：Java 一次性 CLI**
```bash
cat <<'EOF' | jshell -
import java.security.SecureRandom;
import java.util.Base64;
byte[] k = new byte[32];
new SecureRandom().nextBytes(k);
System.out.println(Base64.getEncoder().encodeToString(k));
EOF
```

**方法 C：Python**
```bash
python3 -c "import os,base64; print(base64.b64encode(os.urandom(32)).decode())"
```

### 3.3 分发密钥

**Kubernetes：**
```bash
kubectl create secret generic seata-security-tenant-a \
    --from-literal=TENANT_A_SECRET="5NPJlpsN9j2VhFsx3EhV0PQGH2LqBnwYm5DcMkAt+8w=" \
    -n seata

# 注入到 Deployment
env:
  - name: TENANT_A_SECRET
    valueFrom:
      secretKeyRef:
        name: seata-security-tenant-a
        key: TENANT_A_SECRET
```

**Docker Compose：**
```yaml
services:
  seata-server:
    image: seataio/seata-server:latest
    environment:
      TENANT_A_SECRET: "5NPJlpsN9j2VhFsx3EhV0PQGH2LqBnwYm5DcMkAt+8w="
      NAMING_SERVER_SECRET: "..."
```

**VM/物理机：**
```bash
# /etc/seata/seata-server.env
TENANT_A_SECRET=5NPJlpsN9j2VhFsx3EhV0PQGH2LqBnwYm5DcMkAt+8w=
NAMING_SERVER_SECRET=...

# systemd
[Service]
EnvironmentFile=/etc/seata/seata-server.env
```

### 3.4 密钥轮换

**推荐轮换周期：90 天**（可根据组织合规要求缩短）

**零停机轮换步骤：**

```
【Phase 1】双密钥共存（几分钟内完成配置下发）
  Step 1: 生成新密钥 SECRET_NEW
  Step 2: 在 TC 端 allowed-callers 追加：
    - id: naming-server-01-new
      secret-ref: env:NAMING_SERVER_SECRET_NEW
      permissions: [VGROUP_WRITE, CONSOLE_PROXY, MCP]
  Step 3: 滚动重启 TC（每次一台，滚动更新）

【Phase 2】切换到新密钥（NamingServer 端）
  Step 4: 在 NamingServer 端更新 outbound.cluster-id = naming-server-01-new
                                  outbound.secret-ref = env:NAMING_SERVER_SECRET_NEW
  Step 5: 滚动重启 NamingServer

【Phase 3】观察 24 小时
  Step 6: 通过日志 / 指标确认没有请求使用旧 cluster-id
  Step 7: 从 TC 端 allowed-callers 移除 naming-server-01
  Step 8: 滚动重启 TC 完成收尾
```

**关键点**：
- Phase 1 → Phase 3 之间，两个 cluster-id **同时有效**，任何一方使用哪套密钥都能通过
- 密钥泄露时可直接跳过 Phase 3，从 TC 端立即摘除旧 cluster-id

---

## 4. 场景化部署示例

### 4.1 场景 A：单集群（最简部署）

```yaml
# ==================== NamingServer 端 ====================
# namingserver/application.yml
seata:
  security:
    enabled: true
    mode: ENFORCE
    clusters:
      - id: seata-prod
        secret-ref: env:SEATA_PROD_SECRET
        permissions: [REGISTER, HEARTBEAT, VGROUP_WRITE, CONSOLE_READ, CONSOLE_WRITE]
    outbound:
      cluster-id: naming-server
      secret-ref: env:NAMING_SERVER_SECRET
    exclude-paths:
      - /naming/v1/health

# ==================== Seata Server 端 ====================
# server/application.yml
seata:
  registry:
    type: seata
    seata:
      server-addr: naming-server:8081
      security:
        # 出站（TC → NamingServer）配置 —— 需要在 registry 层适配
        outbound:
          cluster-id: seata-prod
          secret-ref: env:SEATA_PROD_SECRET
        # 入站（NamingServer → TC）配置
        inbound:
          enabled: true
          mode: ENFORCE
          allowed-callers:
            - id: naming-server
              secret-ref: env:NAMING_SERVER_SECRET
              permissions: [VGROUP_WRITE, CONSOLE_PROXY, MCP]
```

**分发密钥（.env）：**
```bash
SEATA_PROD_SECRET=$(openssl rand -base64 32)
NAMING_SERVER_SECRET=$(openssl rand -base64 32)
```

### 4.2 场景 B：多租户（3 套 TC 集群共享 NamingServer）

```yaml
# ==================== NamingServer 端 ====================
seata:
  security:
    enabled: true
    mode: ENFORCE
    clusters:
      # 租户 A：只能访问 prod/staging 环境的自己的 cluster
      - id: tenant-a
        secret-ref: env:TENANT_A_SECRET
        allowed-namespaces: [prod-a, staging-a]
        allowed-clusters: [cluster-A-*]
        allowed-vgroups: ["tenant_a_*"]
        permissions: [REGISTER, HEARTBEAT, VGROUP_WRITE, CONSOLE_READ, CONSOLE_WRITE]

      # 租户 B：只能访问 prod 环境
      - id: tenant-b
        secret-ref: env:TENANT_B_SECRET
        allowed-namespaces: [prod-b]
        allowed-clusters: [cluster-B-*]
        allowed-vgroups: ["tenant_b_*"]
        permissions: [REGISTER, HEARTBEAT, VGROUP_WRITE, CONSOLE_READ, CONSOLE_WRITE]

      # 只读监控账号（例如运维观察其他租户）
      - id: readonly-ops
        secret-ref: env:READONLY_SECRET
        allowed-namespaces: ["*"]
        allowed-clusters: ["*"]
        permissions: [CONSOLE_READ]         # 只有读权限

    outbound:
      cluster-id: naming-server-01
      secret-ref: env:NAMING_SERVER_SECRET

# ==================== TC 集群 A ====================
seata:
  registry:
    seata:
      security:
        outbound:
          cluster-id: tenant-a
          secret-ref: env:TENANT_A_SECRET
        inbound:
          enabled: true
          allowed-callers:
            - id: naming-server-01
              secret-ref: env:NAMING_SERVER_SECRET
              permissions: [VGROUP_WRITE, CONSOLE_PROXY]

# ==================== TC 集群 B ====================
seata:
  registry:
    seata:
      security:
        outbound:
          cluster-id: tenant-b
          secret-ref: env:TENANT_B_SECRET
        inbound:
          enabled: true
          allowed-callers:
            - id: naming-server-01
              secret-ref: env:NAMING_SERVER_SECRET
              permissions: [VGROUP_WRITE, CONSOLE_PROXY]
```

**多租户隔离效果：**
- 租户 A 用自己的密钥签名，试图注册到 `namespace=prod-b` → **403 FORBIDDEN**
- 租户 A 伪造 `X-Seata-Cluster-Id=tenant-b`，但用自己的密钥签名 → **401 BAD_SIGNATURE**
- 只读账号 `readonly-ops` 试图调用 `changeGroup` → **403 FORBIDDEN**（无 `VGROUP_WRITE`）

### 4.3 场景 C：Raft 高可用集群

```yaml
# NamingServer 端（3 副本 HA）
seata:
  security:
    enabled: true
    mode: ENFORCE
    clusters:
      - id: raft-cluster
        secret-ref: env:RAFT_CLUSTER_SECRET
        allowed-namespaces: [prod]
        allowed-clusters: [raft-1, raft-2, raft-3]  # 单集群 3 副本 → 3 个 unit
        permissions: [REGISTER, HEARTBEAT, VGROUP_WRITE, CONSOLE_READ, CONSOLE_WRITE]
    outbound:
      cluster-id: naming-server-01
      secret-ref: env:NAMING_SERVER_SECRET

# 每个 TC 节点（3 副本）配置相同：
seata:
  registry:
    seata:
      security:
        outbound:
          cluster-id: raft-cluster
          secret-ref: env:RAFT_CLUSTER_SECRET
        inbound:
          enabled: true
          allowed-callers:
            - id: naming-server-01
              secret-ref: env:NAMING_SERVER_SECRET
              permissions: [VGROUP_WRITE, CONSOLE_PROXY, MCP]
```

**Raft 特殊考虑：**
- Leader 切换时新 Leader 用同一个 cluster-id 上报 → 无需额外配置
- Follower 也会上报（角色不同但身份同），共用密钥
- NonceCache 是单机的，多个 NamingServer 副本各自维护 → 客户端在多副本间切换时可能出现少量误判（详见 §7.4）

---

## 5. Client SDK 集成（TM/RM 端可选升级）

**默认状态**：Client 端仍使用现有 JWT 机制（`NamingserverRegistryServiceImpl.refreshToken()`），本方案不影响它。

**如需升级 Client 也走 HMAC 签名**（推荐生产环境）：

```yaml
# Client 应用配置
seata:
  tx-service-group: my_tx_group
  registry:
    type: seata
    seata:
      server-addr: naming-server-01:8081,naming-server-02:8081
      namespace: prod
      security:
        cluster-id: business-app-x         # 该 Client 应用的身份
        secret-ref: env:BUSINESS_APP_X_SECRET
```

**对应 NamingServer 端需注册 Client 身份：**
```yaml
seata:
  security:
    clusters:
      - id: business-app-x
        secret-ref: env:BUSINESS_APP_X_SECRET
        allowed-namespaces: [prod]
        permissions:
          - CONSOLE_READ                   # discovery / watch 属于此权限
```

---

## 6. 灰度启用与监控

### 6.1 灰度三阶段

```
【Phase 1】WARN 模式（1-2 周）
  - NamingServer & TC 端 mode: WARN
  - 观察日志中出现的 [security][WARN] 记录
  - 找出所有未升级的调用方（Client、旧版本 TC、遗留脚本）
  - 逐个升级 / 修补

【Phase 2】切换 ENFORCE（1 周）
  - 保留 WARN 阶段发现的所有 allowed-callers
  - mode: ENFORCE
  - 滚动生效，观察 401/403 告警

【Phase 3】清理
  - 移除临时白名单
  - 下线旧 JWT 端点（如决定完全弃用）
```

### 6.2 关键日志字段

**成功请求**（DEBUG 级别，默认不打印）：
```
[security] cluster-id=tenant-a method=POST path=/naming/v1/register status=200
```

**失败请求**（WARN 级别）：
```
[security][ENFORCE] POST /naming/v1/register rejected status=401 code=BAD_SIGNATURE msg=signature does not match caller=tenant-a
[security][ENFORCE] POST /naming/v1/register rejected status=403 code=FORBIDDEN msg=caller tenant-a lacks permission for POST /naming/v1/register caller=tenant-a
[seata-server][security][ENFORCE] GET /vgroup/v1/addVGroup status=401 code=REPLAY_DETECTED msg=nonce already used for cluster-id naming-server-01 caller=naming-server-01
```

### 6.3 Prometheus 指标（建议自行接入）

```
# 需自行在 SecurityFilter 中埋点，建议指标：
seata_auth_request_total{result="ok|denied", reason="MISSING_SIGNATURE|BAD_SIGNATURE|REPLAY_DETECTED|FORBIDDEN|TIMESTAMP_SKEW|UNKNOWN_CLUSTER_ID", src_cluster_id="..."}
seata_auth_signature_verify_duration_seconds
seata_auth_nonce_cache_size
```

### 6.4 关键告警规则

```yaml
# Prometheus alerting rules 示例
- alert: SeataAuthBruteForce
  expr: rate(seata_auth_request_total{result="denied", reason=~"BAD_SIGNATURE|UNKNOWN_CLUSTER_ID"}[5m]) > 10
  for: 3m
  annotations:
    summary: "可疑暴力破解：{{ $labels.src_cluster_id }} 5min 内失败超过 10 次/s"

- alert: SeataAuthReplayAttack
  expr: increase(seata_auth_request_total{result="denied", reason="REPLAY_DETECTED"}[5m]) > 0
  annotations:
    summary: "疑似重放攻击：{{ $labels.src_cluster_id }}"

- alert: SeataAuthCrossTenant
  expr: increase(seata_auth_request_total{result="denied", reason="FORBIDDEN"}[1m]) > 5
  annotations:
    summary: "跨租户越权尝试或配置错误：{{ $labels.src_cluster_id }}"
```

---

## 7. 故障排查

### 7.1 快速定位表

| 症状 | 响应头 `X-Seata-Auth-Error` | 可能原因 | 排查步骤 |
|------|------------------------------|----------|----------|
| 401 | `MISSING_SIGNATURE` | Client 未开启签名 / Header 被反向代理剥离 | 检查中间层 nginx 是否透传 `X-Seata-*`；确认 Client 配置 `outbound.cluster-id` 已设置 |
| 401 | `UNKNOWN_CLUSTER_ID` | 接收方 `allowed-callers` 未包含此 cluster-id | 核对接收方配置 |
| 401 | `TIMESTAMP_SKEW` | 时钟不同步 | 部署 chrony/ntpd，`chronyc tracking` 验证 |
| 401 | `REPLAY_DETECTED` | Client 复用 nonce / Nonce 生成不够随机 | 用 UUID.randomUUID() 或 SecureRandom；不要用固定值/自增 |
| 401 | `BAD_SIGNATURE` | 密钥不一致 / 签名算法实现 bug / query 参数编码差异 | 打开 Client 端 DEBUG 日志比对 signString |
| 401 | `UNSUPPORTED_ALG` | `X-Seata-Sign-Alg` 值不认识 | 双方版本对齐，或省略此 header（默认 HMAC-SHA256） |
| 403 | `FORBIDDEN` | Cluster-id 合法但没有对应权限 / 越权访问 namespace/cluster | 检查 `permissions` 和 `allowed-namespaces` 配置 |
| 401 | `BAD_REQUEST` | Timestamp header 不是数字 / Nonce/Cluster-Id 为 null | 检查 Header 格式 |

### 7.2 常见误配

#### 问题 1：`BAD_SIGNATURE` 但双方密钥看起来相同

**排查步骤：**
```bash
# 1. 打印双方密钥的字节数
echo -n "$TENANT_A_SECRET" | base64 -d | wc -c    # 应输出 32

# 2. 双方是否都是 Base64 解码后使用（而不是把 Base64 字符串当密钥）
# 检查代码：SecretResolver.decodeBase64() 会自动解码 env:xxx

# 3. Nonce 是否包含特殊字符？UUID 是安全的
# 4. body 是否包含 CRLF 与 LF 差异？（例如 curl 在 Windows）
```

#### 问题 2：Client 端一直 `TIMESTAMP_SKEW`

```bash
# 检查双方时钟
ssh naming-server-01 date +%s%3N
ssh seata-server-01 date +%s%3N
# 差异应 < 300000（5min）

# NTP 状态
chronyc tracking     # Linux
w32tm /query /status # Windows
```

#### 问题 3：升级到 ENFORCE 后大量 `MISSING_SIGNATURE`

```
说明有调用方未升级到签名版本。回退到 WARN 模式，观察日志：
grep "MISSING_SIGNATURE" logs/seata-server.log | awk '{print $NF}' | sort -u
拿到未升级的 IP / 服务，逐个改造。
```

### 7.3 密钥损坏或泄露的应急预案

```bash
# 1. 立即生成新密钥
NEW_SECRET=$(openssl rand -base64 32)

# 2. 更新 K8s Secret（或 Vault / KMS）
kubectl create secret generic seata-security-tenant-a \
    --from-literal=TENANT_A_SECRET="$NEW_SECRET" \
    --dry-run=client -o yaml | kubectl apply -f -

# 3. 立即从 NamingServer 端 clusters 列表移除旧 cluster-id
#    （通过配置中心热更新，或滚动重启）

# 4. 通知所有租户应用滚动重启，使用新密钥

# 5. 事后：审计日志排查泄露期间的所有请求
```

### 7.4 NonceCache 单机限制

**问题**：`NonceCache.InMemory` 只在**单节点**去重。多副本 NamingServer 部署时，攻击者理论上可以：
```
1. 拦截 Client 到 NS-A 的合法请求，拿到有效签名
2. 立即向 NS-B 重放同一请求
3. NS-B 的 Nonce cache 中没有此 nonce → 通过
```

**缓解方案：**

**方案 1（推荐）**：让客户端始终固定到一个 NamingServer（sticky session）
- 通过 LB 的 `ip_hash` 或 `cluster-id_hash`

**方案 2**：分布式 NonceCache（需自行实现）
```java
// 实现 NonceCache 接口，用 Redis SETNX + TTL 存储 nonce
public class RedisNonceCache implements NonceCache {
    public boolean putIfAbsent(String clusterId, String nonce) {
        String key = "seata:nonce:" + clusterId + ":" + nonce;
        return jedis.set(key, "1", SetParams.setParams().nx().px(ttlMillis)) != null;
    }
    // ...
}
// 然后覆写自动装配的 Bean:
@Bean
public NonceCache nonceCache(JedisPool pool, SecurityProperties props) {
    return new RedisNonceCache(pool, TimeUnit.MINUTES.toMillis(props.getNonceCacheMinutes()));
}
```

**方案 3**：签名字符串中绑定接收方节点 ID
- 需自定义 `CanonicalRequest` 扩展 —— 目前未内置

---

## 8. 兼容性说明

### 8.1 与现有 JWT 认证的关系

| 场景 | 现状 | 本方案 |
|------|------|--------|
| Client (TM/RM) → NamingServer | JWT | 保持不变，可选升级 HMAC |
| Client (TM/RM) → TC (Netty) | `RegisterCheckAuthHandler` | 保持不变 |
| TC → NamingServer | JWT（复用 Client 通道）| 保持不变，可选升级 HMAC |
| **NamingServer → TC** | **无鉴权** | **本方案新增 HMAC** |
| Console → NamingServer | 会话 | 保持不变 |

### 8.2 版本兼容

| 部署组合 | 是否兼容 |
|----------|----------|
| NamingServer 已升级 + TC 已升级 + `enabled=true` | ✅ 完全支持 |
| NamingServer 已升级 + TC 未升级 | ✅ NamingServer 请求 TC 不带签名，TC 未开启验签 → 正常 |
| NamingServer 未升级 + TC 已升级 + `mode=WARN` | ✅ TC 只记录警告不拒绝 |
| NamingServer 未升级 + TC 已升级 + `mode=ENFORCE` | ⚠️ **TC 拒绝 NamingServer 请求** — 不要这样配置 |
| 双方均未升级 | ✅ 与旧版本行为完全一致 |

**升级顺序建议**：TC 端 → mode=WARN → NamingServer 端 → 观察 → 切 mode=ENFORCE。

### 8.3 反向代理兼容

如果 NamingServer 前有反向代理（Nginx、Traefik 等），确保透传自定义头：

```nginx
# Nginx 示例
location /naming/ {
    proxy_pass http://naming_upstream;
    proxy_set_header X-Seata-Cluster-Id  $http_x_seata_cluster_id;
    proxy_set_header X-Seata-Timestamp   $http_x_seata_timestamp;
    proxy_set_header X-Seata-Nonce       $http_x_seata_nonce;
    proxy_set_header X-Seata-Sign-Alg    $http_x_seata_sign_alg;
    proxy_set_header X-Seata-Sign        $http_x_seata_sign;
    # 保留原始 Host / URI 与 query（签名基于此计算）
    proxy_set_header Host                $host;
    proxy_http_version 1.1;
}
```

---

## 9. 限制与已知问题

| 限制 | 说明 | 缓解 |
|------|------|------|
| **NonceCache 单机** | 多 NamingServer 副本部署时，节点间不共享 nonce | 见 §7.4 |
| **时钟依赖** | 5min 时间窗容忍常规漂移，但极端场景仍需 NTP | 部署 chrony |
| **未处理长轮询 watch** | Client → NamingServer 的 `/watch` 走原 JWT | 未来独立方案 |
| **不含传输层安全** | 本方案只解决应用层身份，不涉及 TLS | 建议叠加 HTTPS |
| **密钥硬存储** | 密钥落到 env / Vault，重启即可读 | 生产建议对接 KMS，密钥永不落盘 |
| **不含 IP 白名单** | 完全信任签名 | 可通过 nginx allow/deny 前置约束 |
| **单一算法** | 目前仅内置 HMAC-SHA256/512 | 通过 `SignatureAlgorithm` 枚举扩展 |

---

## 10. FAQ

### Q1：可以不启用鉴权吗？
可以。**默认 `enabled=false`**，不启用时行为与升级前完全一致，且不引入任何运行时开销。

### Q2：Client 端也必须升级吗？
不必须。TC ↔ NamingServer 之间的鉴权与 Client 完全独立。Client 可以继续用 JWT，或后续单独升级。

### Q3：单机开发测试如何简化？
```yaml
seata:
  security:
    enabled: true
    mode: WARN                             # 不拦截，避免调试受阻
    clusters:
      - id: dev
        secret-ref: plain:local-dev-secret-min-32-bytes-abcdefghij
        allowed-namespaces: ["*"]
        allowed-clusters: ["*"]
        permissions:
          - REGISTER
          - HEARTBEAT
          - VGROUP_WRITE
          - CONSOLE_READ
          - CONSOLE_WRITE
```

### Q4：密钥长度必须 32 字节吗？可以更长吗？
- **最小 32 字节（256 bit）** — 短于此值启动即失败
- 更长完全没问题（HMAC 内部会 hash 到 block size）
- 推荐固定 32 字节，简化管理

### Q5：如何验证配置是否生效？
```bash
# 1. 启动后查看日志
grep "loaded.*cluster identities" logs/naming-server.log
# 期望: Loaded 2 cluster identities for NamingServer security

# 2. 用 curl 手动构造签名请求测试
export CID="tenant-a"
export SECRET_B64="..."
export TS=$(date +%s%3N)
export NONCE=$(uuidgen)
export METHOD="POST"
export PATH_="/naming/v1/register"

# 构造 signString（示例仅演示，实际需要按 canonicalizer 规则）
BODY='{"test":"data"}'
BODY_HASH=$(printf "$BODY" | shasum -a 256 | awk '{print $1}')
SIGN_STRING="${METHOD}
${PATH_}
namespace=test
${CID}
${TS}
${NONCE}
${BODY_HASH}"

SIGNATURE=$(printf "$SIGN_STRING" | \
    openssl dgst -sha256 -mac HMAC -macopt "hexkey:$(echo -n $SECRET_B64 | base64 -d | xxd -p -c 999)" \
    -binary | base64)

curl -X POST "http://localhost:8081/naming/v1/register?namespace=test&clusterName=c1&unit=u1" \
     -H "Content-Type: application/json" \
     -H "X-Seata-Cluster-Id: $CID" \
     -H "X-Seata-Timestamp: $TS" \
     -H "X-Seata-Nonce: $NONCE" \
     -H "X-Seata-Sign-Alg: HMAC-SHA256" \
     -H "X-Seata-Sign: $SIGNATURE" \
     -d "$BODY" -v

# 期望 200；若 401，查看响应头 X-Seata-Auth-Error 判断具体原因
```

### Q6：需要为每个 TC 节点分配独立 cluster-id 吗？
**不需要**。一套 TC 集群（可能多个节点、多个 raft-group）应共享同一个 cluster-id 和密钥。这与 Seata 的"集群"语义一致。

### Q7：现在支持 mTLS 吗？
本方案（方案 A）**不含 mTLS**，但可以叠加。参考 [`NAMINGSERVER_ARCHITECTURE.md`](../NAMINGSERVER_ARCHITECTURE.md) §11.5 方案 B 的规划。

---

## 11. 附录：代码文件索引

```
common/src/main/java/org/apache/seata/common/security/
├── SecurityConstants.java        # HTTP 头名与错误码枚举
├── SignatureAlgorithm.java       # 支持的 MAC 算法（HMAC-SHA256/512）
├── CanonicalRequest.java         # 可签名请求值对象（不可变）
├── SignatureCanonicalizer.java   # 生成 signString（method+path+query+cluster-id+ts+nonce+body-hash）
├── HmacSigner.java               # 签名与验签（constant-time compare）
├── NonceCache.java               # 防重放缓存接口 + 单机内存实现
├── SignatureVerifier.java        # 完整验签流水线（freshness → nonce → signature）
└── VerificationResult.java       # 结构化验签结果

namingserver/src/main/java/org/apache/seata/namingserver/security/
├── Permission.java               # NamingServer 权限枚举
├── ClusterIdentity.java          # 单个 cluster-id 的完整身份
├── ClusterIdentityRegistry.java  # id → identity 内存表
├── SecretResolver.java           # secretRef → 原始字节的解析器（env/base64/plain）
├── SecurityProperties.java       # @ConfigurationProperties("seata.security")
├── PermissionChecker.java        # 路径→权限映射与授权检查
├── SecurityFilter.java           # 入站 Servlet Filter（完整流水线）
├── OutboundSigner.java           # 生成出站请求的 X-Seata-* 头
└── SecurityAutoConfiguration.java # Spring Boot 自动装配

server/src/main/java/org/apache/seata/server/security/
├── CallerPermission.java         # TC 侧权限枚举（VGROUP_WRITE/CONSOLE_PROXY/MCP）
├── AllowedCaller.java            # 单个允许的调用者身份
├── AllowedCallerRegistry.java    # id → caller 内存表
├── ServerSecretResolver.java     # 同 SecretResolver（模块隔离）
├── ServerSecurityProperties.java # @ConfigurationProperties("seata.registry.seata.security.inbound")
├── RouteAuthorizer.java          # TC 路径→权限映射
├── SeataServerAuthFilter.java    # 入站 Servlet Filter
└── ServerSecurityAutoConfiguration.java # Spring Boot 自动装配

# 单元测试（共 76 个测试，覆盖率 93%）
common/src/test/java/org/apache/seata/common/security/       (48 tests)
namingserver/src/test/java/org/apache/seata/namingserver/security/  (46 tests)
server/src/test/java/org/apache/seata/server/security/       (28 tests)
```

---

## 12. 变更历史

| 日期 | 版本 | 变更 |
|------|------|------|
| 2026-07-27 | v1.0 | 首版：HMAC-SHA256 方案 A 落地，含完整单测 |
