package me.lianecx.discordlinker.spigot.hooks.vault;

import me.lianecx.discordlinker.common.abstraction.LinkerOfflinePlayer;
import me.lianecx.discordlinker.common.abstraction.GroupPermissionsBridge;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static me.lianecx.discordlinker.common.DiscordLinkerCommon.getLogger;

public final class VaultGroupPermissionsBridge implements GroupPermissionsBridge {

    private final Permission permission;

    public VaultGroupPermissionsBridge(Permission permission) {
        this.permission = permission;
    }

    @Override
    public String id() {
        return "vault";
    }

    @Override
    public boolean hasPermission(LinkerOfflinePlayer player, String perm) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(player.getUUID()));
        if(offlinePlayer.isOnline()) return permission.playerHas((String) null, offlinePlayer.getPlayer(), perm);
        return permission.playerHas((String) null, offlinePlayer, perm);
    }

    @Override
    public CompletableFuture<List<String>> getPlayersInGroup(String group) {
        // Vault has no direct "all players in group" API.
        // We check all players Bukkit knows about (online + cached offline).
        return CompletableFuture.supplyAsync(() -> {
            String[] allGroups = permission.getGroups();
            if(allGroups == null || Arrays.stream(allGroups).noneMatch(g -> g.equalsIgnoreCase(group))) return null;

            List<String> result = new ArrayList<>();
            for(OfflinePlayer p : Bukkit.getOfflinePlayers()) {
                if(permission.playerInGroup((String) null, p, group)) {
                    result.add(p.getUniqueId().toString());
                }
            }
            return result;
        });
    }

    @Override
    public CompletableFuture<Void> addToGroup(String group, String uuid) {
        return CompletableFuture.runAsync(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
            getLogger().info("Adding user " + uuid + " to Vault group " + group);
            permission.playerAddGroup((String) null, player, group);
        });
    }

    @Override
    public CompletableFuture<Void> removeFromGroup(String group, String uuid) {
        return CompletableFuture.runAsync(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
            getLogger().info("Removing user " + uuid + " from Vault group " + group);
            permission.playerRemoveGroup((String) null, player, group);
        });
    }

    @Override
    public List<String> listGroups() {
        String[] groups = permission.getGroups();
        return groups != null ? Arrays.asList(groups) : new ArrayList<>();
    }
}
