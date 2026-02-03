# 🛡️ SoulGuard Security - Official Wiki

Welcome to the official documentation for **SoulGuard**, the ultimate security suite for Minecraft servers. This wiki provides a deep dive into every feature, command, and configuration option.

---

## 🚀 Quick Start
1. **Installation**: Drop `SoulGuard.jar` into your `/plugins` folder.
2. **Setup**: Run `/sg setup` in-game to select a security preset.
3. **Configure**: Customize your settings in the modular files in `/plugins/SoulGuard/`.

---

## 🛠️ Command Reference
SoulGuard uses a streamlined command system. Hover over commands in-game for more info.

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/sg gui` | `soulguard.admin` | Open the central security control panel. |
| `/sg setup` | `soulguard.admin` | Launch the guided setup wizard. |
| `/sg help` | `soulguard.help` | View interactive help menu. |
| `/sg reload` | `soulguard.admin` | Reload all configurations and modules. |
| `/sg panic` | `soulguard.panic` | Toggle emergency lockdown mode. |
| `/sg audit` | `soulguard.audit` | Search security logs asynchronously. |

---

## 📦 Module Deep-Dive
SoulGuard is built with an ultra-modular architecture. Each module can be toggled independently.

### 🔐 Identity & Access
- **AuthModule**: Handles `/login` and `/register`. Uses Argon2 hashing for maximum security.
- **StaffSecurity**: Adds IP-whitelisting and GUI Pattern PINs for all administrators.
- **IP Drift Lock**: Prevents session hijacking by monitoring IP changes in real-time.

### 🛡️ Defensive Systems
- **AntiExploit**: Blocks malformed packets, crash items, and illegal inventory data.
- **VPNDetector**: Multi-API detection of Proxies, VPNs, and Datacenters.
- **AI Bot Firewall**: Uses heuristic analysis to distinguish bots from human players.

---

## 🔑 Permissions Hierarchy

```mermaid
graph TD
    Owner["Owner (soulguard.*)"] --> Admin["Admin (soulguard.admin)"]
    Admin --> Staff["Staff (soulguard.staff)"]
    Staff --> User["User (soulguard.player)"]
    
    subgraph Nodes
    Admin --> Audit["soulguard.audit"]
    Admin --> Setup["soulguard.setup"]
    Staff --> Help["soulguard.help"]
    User --> Login["soulguard.login"]
    end
```

---

## 📁 Configuration Layout
Since version 2.1+, SoulGuard uses a **Modular Config System**:

- `config.yml`: Core settings and global toggles.
- `auth.yml`: All identity and access settings.
- `protection.yml`: Anti-cheat, anti-bot, and exploit settings.
- `database.yml`: Connection strings for MySQL/SQLite/MongoDB.
- `messages.yml`: Full translation and prefix customization.
- `advanced.yml`: Technical performance and threading tweaks.

---

## ❓ FAQ & Troubleshooting
> [!TIP]
> **Issue**: A player is wrongly blocked as a VPN.
> **Fix**: Add their IP to the `whitelist` in `protection.yml` or lower the `risk-threshold`.

> [!IMPORTANT]
> **Performance**: For 200+ players, we recommend switching to **MySQL** and enabling `async-logs` in `advanced.yml`.

---
*Powered by SoulGuard Intelligence Engine*
