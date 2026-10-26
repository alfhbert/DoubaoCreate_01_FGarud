# 安全守护 · SafetyGuard

> 家人之间的守护应用：两台手机相互查看**电量、实时位置、通话状态**，面向老人与儿童防诈骗。
> 家用 / 非商用。Android 8.0+（API 26）。

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Android CI](https://github.com/alfhbert/DoubaoCreate_01_FGarud/actions/workflows/android.yml/badge.svg)](https://github.com/alfhbert/DoubaoCreate_01_FGarud/actions/workflows/android.yml)

---

## 目录

- [功能特性](#功能特性)
- [为什么需要它](#为什么需要它)
- [快速开始](#快速开始)
- [配对与使用](#配对与使用)
- [权限清单](#权限清单)
- [安全与隐私设计](#安全与隐私设计)
- [技术架构](#技术架构)
- [从源码构建](#从源码构建)
- [发布签名说明](#发布签名说明)
- [已知限制](#已知限制)
- [路线图](#路线图)
- [许可证与免责声明](#许可证与免责声明)

---

## 功能特性

| 功能 | 说明 |
|---|---|
| 电量互看 | 实时显示双方电量与充电状态；对方电量低于阈值（默认 20%，可调）自动提醒 |
| 实时位置 | OpenStreetMap 地图互显位置，支持「跟随对方」；首页显示双方直线距离 |
| 通话状态互看 | 空闲 / 来电响铃 / 通话中实时同步给对方；可显示来电号码与是否在通讯录 |
| 可疑来电提醒 | 境外来电、400/800 热线、95 号段、106 短信平台号段、异常长号码、黑名单号码自动告警，**本机与对方手机同时收到提醒** |
| SOS 一键求助 | 首页大红按钮，一键将求助 + 当前位置发送给对方（强提醒 + 震动） |
| 安全区域围栏 | 设定安全中心与半径，本机或对方离开该区域即提醒 |
| 号码黑名单 | 手动添加号码，命中即高等级告警 |
| 超大字体模式 | 适老化界面，默认开启，可随时关闭 |
| 事件记录 | 可疑来电、SOS、低电量、离开安全区域等全部留痕，可一键清空 |
| 开机自启 | 配对后开机自动恢复守护服务 |
| 防骗课堂 | 内置常见骗术与防骗要点，适合老人阅读 |

## 为什么需要它

老人和儿童是电信诈骗的高危人群。本应用解决两个家庭痛点：

1. **看得见**：子女随时能看到老人/孩子的电量、位置、是否在通话，心里有底；
2. **拦得住**：可疑号码来电时，老人手机立即弹窗警示，子女手机同步收到提醒，可第一时间电话确认。

它**不做**：不拦截电话、不监听通话内容、不读取短信，仅做状态可见性与风险提示。

## 快速开始

- **方式一（推荐）**：直接安装 `安全守护-v1.0.0.apk`（见 [Releases](https://github.com/alfhbert/DoubaoCreate_01_FGarud/releases)）。需要允许「安装未知来源应用」。
- **方式二**：从源码构建，见[从源码构建](#从源码构建)。

安装后按提示授予权限（定位、电话状态、通讯录、通知）。**定位与通知权限是核心功能所必需**，缺失时守护服务可能无法启动。

## 配对与使用

1. 两台手机都安装本应用（Android 8.0 及以上）。
2. 手机 A：打开 App → 右上角「守护配对」→「生成守护码」→ 把 6 位数字码告诉手机 B（电话里说即可）。
3. 手机 B：「守护配对」→ 输入该 6 位码 →「加入守护」。
4. 回到首页，双方将看到彼此的守护状态卡片。两台手机需同时联网在线。

> 配对的守护码只用于两台设备之间的身份协商与加密密钥派生，**不会发送到任何服务器**。

## 权限清单

最小权限原则：**只有实现功能所必需的权限**，逐一说明：

| 权限 | 用途 | 必需性 |
|---|---|---|
| `INTERNET` | MQTT 状态同步、地图瓦片加载 | 必需 |
| `ACCESS_NETWORK_STATE` | 判断网络连通性 | 必需 |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | 获取本机位置并同步给对方 | 必需 |
| `ACCESS_BACKGROUND_LOCATION` | 应用退到后台时持续定位（守护的本质场景） | 必需（Android 10+） |
| `READ_PHONE_STATE` | 获取通话状态与来电号码 | 必需 |
| `READ_CONTACTS` | 仅用于判断来电号码是否在通讯录（**不读取、不上传通讯录内容**） | 可选（缺失则无法识别“陌生号码”） |
| `POST_NOTIFICATIONS` | 发送守护提醒 | 必需（Android 13+） |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_LOCATION` | 前台守护服务保活 | 必需 |
| `RECEIVE_BOOT_COMPLETED` | 开机自动恢复守护服务 | 可选 |

**明确不申请的权限**：短信（SMS）、通话记录（CALL_LOG）、相机、麦克风、存储。

## 安全与隐私设计

### 数据流

```
手机 A ──电量/位置/通话状态──▶ 公共 MQTT 代理 ◀──电量/位置/通话状态── 手机 B
        ─────── AES-256-GCM 加密，密钥由 6 位守护码派生 ───────
```

- 每台设备只发布到**私有主题**（含随机 deviceId），只订阅同守护码下的主题；
- 消息载荷使用 **AES-256-GCM** 加密（随机 12 字节 IV，密文带认证标签）；
- 密钥由守护码经 **10,000 轮 SHA-256 迭代**派生，提高暴力猜测成本；
- 状态数据**不落地任何服务器**，公共代理仅做消息转发（QoS 1，不持久化）。

### 边界承诺

- ✅ 不注册账户、不收集设备信息、无广告 SDK、无统计埋点；
- ✅ 通讯录/号码仅在**本机内存**中判断是否陌生，不传出设备；
- ✅ 所有数据仅存储在应用私有目录（SharedPreferences），卸载即清除；
- ✅ 未配对时守护服务不联网（不发任何数据）。

### 边界之外的说明

- 公共 MQTT 代理在传输层不加密（TLS 端口未使用），但载荷已应用层加密；家用场景下，即使流量被截获也无法还原内容。
- 6 位守护码为易用性而设计（老人可口头转述）。迭代散列已显著提高在线暴力破解成本；如需更强保障，可自行部署支持 TLS 的 MQTT 服务并修改 `MqttManager.kt` 中的代理地址。

详细说明见 [PRIVACY.md](PRIVACY.md) 与 [SECURITY.md](SECURITY.md)。

## 技术架构

```
┌────────────────────────── SafetyGuard ──────────────────────────┐
│ MainActivity ── 底部四 Tab：守护 / 地图 / 事件 / 设置             │
│   ├─ HomeFragment     状态卡片 + SOS + 快捷入口                  │
│   ├─ MapFragment      osmdroid(OpenStreetMap) 双标记 + 跟随      │
│   ├─ EventsFragment   RecyclerView 事件记录                     │
│   └─ SettingsFragment 名称/服务/阈值/安全区/黑名单/大字模式       │
│                                                                │
│ SafeguardService（前台服务，foregroundServiceType=location）    │
│   ├─ MqttManager      Paho MQTT：3 代理自动切换 + 自动重连       │
│   ├─ PayloadCodec     AES-256-GCM 加密 / 解密（Gson JSON）       │
│   ├─ TelephonyManager 通话状态监听 → ScamChecker 风险识别        │
│   ├─ LocationManager  GPS + 网络定位 → 安全区域判断             │
│   └─ BatteryReceiver  电量/充电 → 低电量告警                    │
│                                                                │
│ BootReceiver —— 开机自启；StatusHub —— 界面状态中枢              │
└─────────────────────────────────────────────────────────────────┘
```

技术栈：Kotlin · 经典 View（无 Compose）· AndroidX / Material 3 · Paho MQTT · osmdroid · Gson。

## 从源码构建

```bash
# 环境要求：JDK 17、Android SDK（platform 34、build-tools 34.0.0）、Gradle 8.7（或直接使用 Android Studio 打开工程）
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk

git clone https://github.com/alfhbert/DoubaoCreate_01_FGarud.git
cd DoubaoCreate_01_FGarud
gradle assembleDebug          # debug 包（开箱即用）
gradle assembleRelease        # release 包（未配置密钥时自动使用 debug 签名）
```

产物目录：`app/build/outputs/apk/{debug,release}/`。

## 发布签名说明

- 仓库内**不包含任何签名密钥**，密码与 keystore 一律通过环境变量注入：
  ```bash
  export SAFEGUARD_KEYSTORE=/path/to/safetyguard-release.jks
  export SAFEGUARD_KEYSTORE_PASS=...
  export SAFEGUARD_KEY_ALIAS=safetyguard
  export SAFEGUARD_KEY_PASS=...
  gradle assembleRelease
  ```
- 未提供环境变量时，release 自动降级为 debug 签名，便于任何人开箱构建；
- **密钥一旦丢失无法找回**，且更换密钥会导致已安装用户无法直接升级覆盖安装，请妥善备份、切勿上传 GitHub。

## 已知限制

- 采用公共 MQTT 代理（broker.emqx.io / broker.hivemq.com / test.mosquitto.org 自动切换），适合家庭试用；对隐私要求更高可自建代理并启用 TLS；
- 防诈骗为**启发式号码规则**（号段 / 长度 / 黑名单），不能替代国家反诈中心等官方渠道，请勿据此断定某号码一定为诈骗；
- Android 12+ 部分机型受系统限制可能无法取得来电号码，此时显示「号码未知」，通话状态仍可同步；
- 双方需保持联网；对方离线超过 3 分钟显示「对方离线」；
- debug 包未经商店审核，安装需允许「未知来源」。

## 路线图

- [ ] 自建 MQTT 服务 + TLS 部署文档（Docker Compose）
- [ ] 号码风险在线查询（对接公开反诈数据源，需用户自选服务）
- [ ] 多设备群守护（1 个守护码多台设备）
- [ ] 位置历史（本机加密存储，可选）
- [ ] 深色模式 / 英文界面

欢迎提 Issue、PR 一起完善。

## 许可证与免责声明

- 代码以 **MIT License** 开源，见 [LICENSE](LICENSE)。
- 本项目为家庭场景设计的辅助工具，**不构成任何法律、安全或反诈承诺**；请同时使用官方反诈渠道（国家反诈中心 App、96110 等），紧急情况请拨打 110。
