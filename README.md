# Discord-Linker

[![SpigotMC](https://img.shields.io/spiget/downloads/98749?logo=spigotmc&label=SpigotMC)](https://www.spigotmc.org/resources/discord-linker.98749/)
[![Modrinth](https://img.shields.io/modrinth/dt/xfelWIYh?logo=modrinth&label=Modrinth)](https://modrinth.com/plugin/xfelWIYh)
[![CurseForge](https://img.shields.io/curseforge/dt/1487126?logo=curseforge&label=CurseForge)](https://www.curseforge.com/minecraft/mc-mods/discord-linker)
[![Hangar](https://img.shields.io/hangar/dt/Discord-Linker?logo=paper&label=Hangar)](https://hangar.papermc.io/Lianecx/Discord-Linker)
[![Discord](https://img.shields.io/discord/844156404477853716?logo=discord&label=Discord)](https://discord.gg/rX36kZUGNK)

The server-side plugin/mod for [MC Linker](https://mclinker.com) — the easiest way to connect your Minecraft server with Discord.
No custom bot creation or configuration files needed!

## Features

- **Chat Bridge** — Relay messages bidirectionally between Minecraft and Discord
- **Player Stats** — View player statistics, advancements, and inventories from Discord
- **Command Execution** — Run Minecraft commands directly from Discord with tab completions
- **Server Moderation** — Ban, kick, and manage players from Discord
- **Account Linking** — Connect Discord accounts with Minecraft players
- **Status Channels** — Display live online player counts in Discord
- **Synced Roles** — Sync Discord roles with Minecraft teams or LuckPerms groups
- **Console Forwarding** — Forward your server console output to a Discord channel

## Supported Platforms

| Platform | Versions |
|----------|----------|
| Spigot   | 1.8+  |
| Fabric   | 1.16.5+  |
| Forge    | 1.16.5 – 1.20.x |
| NeoForge | 1.21.1+  |
| Hybrid (MohistMC, Magma, Arclight) | Supported |

## Setup

1. Invite the **MC Linker** Discord bot from the [App Directory](https://discord.com/application-directory/712759741528408064) or from [Top.gg](https://top.gg/bot/712759741528408064)
2. Download the plugin from [Modrinth](https://modrinth.com/plugin/xfelWIYh), [CurseForge](https://www.curseforge.com/minecraft/mc-mods/discord-linker), [Hangar](https://hangar.papermc.io/Lianecx/Discord-Linker), or [Aternos](https://aternos.org)
3. Install it on your Minecraft server and restart
4. Run `/connect plugin` in Discord and follow the instructions
5. Optionally run `/chatchannel add #channel` in Discord to set up the chat bridge

## In-Game Commands

| Command | Description                                          |
|---------|------------------------------------------------------|
| `/linker reload` | Reload configuration and reconnect                   |
| `/linker connect <code>` | Connect server to Discord bot                        |
| `/linker disconnect` | Disconnect server from Discord                       |
| `/linker bot_port [port]` | Get or set the bot port (for custom-bot connections) |
| `/linker debug` | Toggle debug mode                                    |
| `/verify <code>` | Verify your Minecraft account with Discord           |
| `/discord` | Get the Discord server invite link                   |

## Plugin Integrations (Hooks)

Discord-Linker supports optional integrations with popular permission plugins to power the **Synced Roles** feature:

| Hook | Platform | Description |
|------|----------|-------------|
| [LuckPerms](https://luckperms.net) | All platforms | Sync Discord roles with LuckPerms groups. Supports live group change events. |
| [Vault](https://dev.bukkit.org/projects/vault) | Spigot only | Sync Discord roles with any Vault-compatible permission plugin's groups. |

If both LuckPerms and Vault are present, LuckPerms takes priority. Vanilla Minecraft teams are used as a fallback when neither hook is available.

## Discord Bot Commands

Run `/help` in Discord for a full list of commands, or check the [MC Linker Guide](https://guide.mclinker.com) for more details.

## Links

- [Website](https://mclinker.com)
- [Discord Bot Repository](https://github.com/MC-Linker/MC-Linker)
- [Support Server](https://discord.gg/rX36kZUGNK)
- [Top.gg](https://top.gg/bot/712759741528408064)

## License

[CC BY-NC 4.0](LICENSE.md) — Attribution-NonCommercial 4.0 International
