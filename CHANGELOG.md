## 4.2.0 - 1.8 Support and Vault Integration
- Required-role check now happens before the player fully joins, so denied players never enter the world
- Added Vault support for group/permission-based role syncing
- Fix color codes now stripped from forwarded chat messages
- Spigot build now supports 1.8 through latest in a single jar
- Fixed Spigot commands not correctly identifying player command senders

# 4.1.0 - Hybrid Server Support
- Added support for hybrid servers (e.g. MohistMC, Magma, Arclight, etc.)

## 4.0.0 - Mod Update
- Now available as a Fabric, Forge, and NeoForge mod in addition to Spigot
- Supports Minecraft 1.12.2, 1.16.5, 1.18.2, 1.19.2, 1.20/1.20.1, and 1.21.1
- Server console can now be forwarded to Discord via chat channels
- Command tab-completions are now sent to the Discord bot
- Synced roles are automatically synced when the server reconnects
- Added a direction setting for synced roles to choose which side takes priority
- Added LuckPerms support for team and group management
- Added `/linker debug` command for troubleshooting
- Improved connection stability and reconnection handling
- HTTP connections are now deprecated in favor of WebSocket
- Various bug fixes and improvements