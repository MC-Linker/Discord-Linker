## 4.4.0 - Minecraft 26.2 Support
- Added support for Minecraft 26.2 on Fabric and NeoForge
- Restored 1.21/1.21.1 builds for Fabric and NeoForge
- Fixed the world folder being detected incorrectly on 26.1 and higher, where the overworld moved into a dimension subfolder
- Fixed the server version being reported with a build suffix attached (e.g. `26.1.2.build.53`)
- Fixed private player commands being forwarded through the chat bridge
- Fixed chat message colors not being converted correctly on 26.1 and higher
- Reconnections to the Discord bot now back off gradually instead of retrying at a fixed rate
- Updated LuckPerms integration to API 5.5

## 4.3.2 - Bug Fixes and Improvements
- Synced roles now sync more reliably on reconnect
- Minor bug fixes and improvements

## 4.3.1 - 26+ Support
- This update adds support for minecraft version 26 and higher
- Fixed advancements sometimes not being relayed on version 26 and higher

## 4.3.0 - /dm Command
- Added `/dm <user> <message>` command to send a Discord DM to a user directly from Minecraft; accepts a Discord username, user ID, or (if linked) a Minecraft username or UUID
- Fixed `/discord` link not being clickable in chat
- More Bug Fixes

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