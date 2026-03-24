package me.lianecx.discordlinker.common.abstraction;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface GroupPermissionsBridge {

    String id();

    boolean hasPermission(LinkerOfflinePlayer player, String permission);

    CompletableFuture<List<String>> getPlayersInGroup(String group);

    CompletableFuture<Void> addToGroup(String group, String uuid);

    CompletableFuture<Void> removeFromGroup(String group, String uuid);

    List<String> listGroups();
}
