# 🛡️ SoulGuard - Elite Security & Anti-Cheat Suite

SoulGuard is a massive, multi-layered security ecosystem for Minecraft servers (1.20+). It combines advanced account protection, staff security, anti-exploit measures, and a custom anti-cheat engine to provide the ultimate shield for your community.

---

## ✨ Features & Modules

SoulGuard is divided into specialized modules, each targeting a specific security vector.

### � Staff & Admin Security
*   **Staff PIN System**: Secure numeric authentication required for all staff members.
*   **IP Whitelisting**: Lock staff accounts to specific IP addresses.
*   **OpGuard**: Intelligent monitoring and prevention of unauthorized `/op` assignments.
*   **Command Firewall**: Deep inspection of sensitive administrative commands.
*   **Staff Analytics**: Real-time tracking of staff activity and efficiency.
*   **Staff Rate Limiter**: Prevents accidental or malicious command spamming by administrators.

### 👤 Identity & Account Protection
*   **Integrated Auth Engine**: Robust login/register system with configurable session timeouts.
*   **Mojang Premium Sync**: Seamlessly verify authentic accounts while maintaining local security.
*   **Identity Guard**: Advanced player identity verification to prevent account spoofing.
*   **Alt Linker**: Automatic detection and linking of player alternative accounts.
*   **User Security Module**: Hardens user accounts against common social engineering and brute force attacks.
*   **Inventory Snapshots**: Automatically saves player inventories during critical security events for easy rollback.

### 🛡️ Advanced Defense
*   **Captcha System**: Multi-stage visual challenges for suspicious connection attempts.
*   **GeoIP Intelligence**: Block or monitor connections from specific countries.
*   **VPN/Proxy Detector**: Real-time database lookup to block malicious proxy nodes.
*   **TravelGuard**: Monitors sudden geographical changes in user connections.
*   **Global Rate Limiter**: Protects the server from packet-based overflow and DDoS-like activities.
*   **Anti-AFK Pro**: Advanced inactivity detection with immunity bypasses.

### ⚔️ Anti-Cheat (Neo-Guard Engine)
*   **Internal AC Core**: A built-in, lightweight anti-cheat solution.
*   **Flight Detection**: Prevents all forms of flying and hovering.
*   **Speed Verification**: Validates movement speed across all surfaces.
*   **Combat Analysis**: Proactive detection of Killaura (GCD based) and Reach.
*   **LiquidWalk**: Blocks "Jesus" and other liquid-based movement hacks.
*   **InventoryMove**: Detects movement or interactions while in a GUI/Menu.
*   **NoWeb Check**: Prevents illegal speed transitions through cobwebs.

### ☣️ Anti-Exploit & Technical Protection
*   **Anti-Exploit Core**: Stops NBT exploits, illegal enchantments, and crasher items.
*   **Packet Inspector**: Deep-packet inspection (DPI) to prevent server-level exploitation.
*   **Command Guard**: Sanitizes all command inputs to stop injection attempts.
*   **Command Blocker**: Granular control over hidden or sensitive plugin commands.
*   **Trade Guard**: Protects the server market from item duplication through trading.
*   **Ghost Guard**: Monitors "ghost" connections and zombie sessions.

### � Social & Moderation
*   **Smart Chat Filter**: Advanced anti-flood, caps, and repetition filters.
*   **ScamGuard**: Proactively identifies phishing links and known scam patterns.
*   **PrivacyGuard**: Automatically masks sensitive data (emails, IPs) in chat.
*   **Moderation Engine**: Full warn, mute, and ban system with persistent history.
*   **Auto Punish**: Configurable automatic responses to detected violations.
*   **Shadow Ban**: Silently isolate toxic players without them knowing.
*   **Quarantine**: Temporarily isolate suspicious players for manual review.

### 📊 Server Management & Logs
*   **Panic Mode**: Instant server lockdown for emergency situations.
*   **Maintenance Mode**: Restrict server access to authorized users during updates.
*   **Audit Manager**: Detailed logging of every security-relevant event.
*   **Rollback Manager**: Easily undo changes caused by compromised accounts.
*   **Discord Webhook**: Real-time security alerts sent directly to your Discord staff channels.
*   **Discord Link**: Securely link Minecraft accounts with Discord identities.

---

## 🛠️ Command Reference

### 💻 System & Management
| Command | Description |
| :--- | :--- |
| `/soulguard reload` | Reloads all configurations and modules. |
| `/soulguard audit` | Views the real-time security logs. |
| `/soulguard panic` | Toggles emergency lockdown mode. |
| `/soulguard maintenance` | Toggles server maintenance mode. |
| `/soulguard gui` | Opens the graphical management dashboard. |
| `/soulguard info` | Displays current security status and version. |
| `/soulguard testwebhook` | Verifies the Discord integration. |

### 👮 Moderation
| Command | Description |
| :--- | :--- |
| `/warn <player> <reason>` | Issues a warning to a player. |
| `/history <player>` | Views a player's full punishment history. |
| `/alts <player>` | Lists all linked accounts for a player. |
| `/lock / /unlock` | Temporarily locks/unlocks its current state. |
| `/unban <player>` | Lifts a ban from a player. |
| `/unmute <player>` | Lifts a mute from a player. |

### ⚖️ Punishments
| Command | Usage |
| :--- | :--- |
| `/ban` | `/ban <player> <reason>` |
| `/tempban` | `/tempban <player> <duration> <reason>` |
| `/mute` | `/mute <player> <reason>` |
| `/tempmute` | `/tempmute <player> <duration> <reason>` |

### � User Commands
| Command | Usage | Description |
| :--- | :--- | :--- |
| `/pin` | `/pin <number>` | Authenticate staff session. |
| `/report` | `/report <player> <reason>` | Send a report to staff. |
| `/premium` | `/premium` | Link account to Mojang. |
| `/login` | `/login <pass>` | Authenticate session. |
| `/register` | `/register <pass> <pass>` | Create new account. |
| `/2fa` | `/2fa <setup\|verify> <code>` | Manage Two-Factor Authentication. |

---

## 🔑 Permissions

| Permission | Description |
| :--- | :--- |
| `soulguard.admin` | Full administrative access. |
| `soulguard.staff` | Access to staff security features (PIN). |
| `soulguard.bypass.*` | Permission to bypass specific security checks (exploit, movement, afk). |

---

## 📋 Technical Specs
*   **Minecraft Versions**: 1.20.1 and higher.
*   **Dependencies**: None (Standalone).
*   **Database Support**: MySQL, MariaDB, Redis (Sync).
*   **Integrations**: Discord (Webhooks).

---
*© 2026 SoulGuard Security Systems. Part of the SoulCraft Network.*
