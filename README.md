
View minecraft stats, advancements and inventories of every member in Discord!

# DESCRIPTION
Look at the Minecraft server stats, advancements and inventories of any member in Discord: When you killed the ender dragon, how many raids you have won or even how long your friend played on the server, this plugin can show it all. Also supports two-way chat with Minecraft.

# SETUP
+ Invite the Discord bot using [this link](https://top.gg/bot/712759741528408064)
+ After you installed the plugin, execute `/connect plugin YOUR.SERVER.IP` in Discord
+ Follow the instructions sent in DM
+ After connecting you can also execute `/chatchannel CHANNEL` in Discord if you want to connect the Minecraft chat with Discord

# ADDITIONAL INFO
+ For a list of all bot commands execute `/help` in Discord
+ For more info about the Discord bot join the [Support Server](https://discord.gg/rX36kZUGNK) or visit the bot’s [top.gg page](https://top.gg/bot/712759741528408064)

# TROUBLESHOOTING
+ Unfortunately, **Aternos** and **Minehut** servers are not currently supported as they do not have ftp or additional ports for plugins.
+ If you receive the error: `Address already in use` in the server console follow the instructions below.
+ If you receive the error: `Plugin does not respond` although your server is online, follow the next steps:
	+ Register an additional port (if supported from your server host)
	+ Enter that port in the plugin’s `config.yml`.
	+ Execute `reload confirm` in your server console
	+ Execute `/connect plugin YOUR.SERVER.IP config_port` in Discord and make sure to specify the correct port from the config.yml
+ More help => [Support Server](https://discord.gg/rX36kZUGNK)