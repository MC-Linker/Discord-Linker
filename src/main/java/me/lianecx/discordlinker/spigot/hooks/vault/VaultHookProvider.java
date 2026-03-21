package me.lianecx.discordlinker.spigot.hooks.vault;

import me.lianecx.discordlinker.common.abstraction.GroupPermissionsBridge;
import me.lianecx.discordlinker.common.hooks.HookProvider;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.Nullable;

public final class VaultHookProvider implements HookProvider<GroupPermissionsBridge> {

    @Override
    public @Nullable GroupPermissionsBridge createSafely() {
        try {
            RegisteredServiceProvider<Permission> rsp = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
            if(rsp == null) return null;

            Permission permission = rsp.getProvider();
            return new VaultGroupPermissionsBridge(permission);
        }
        catch(NoClassDefFoundError e) {
            return null;
        }
    }
}
