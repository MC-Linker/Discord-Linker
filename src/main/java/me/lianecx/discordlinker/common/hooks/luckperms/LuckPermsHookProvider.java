package me.lianecx.discordlinker.common.hooks.luckperms;

import me.lianecx.discordlinker.common.abstraction.GroupPermissionsBridge;
import me.lianecx.discordlinker.common.hooks.HookProvider;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.jetbrains.annotations.Nullable;

public final class LuckPermsHookProvider implements HookProvider<GroupPermissionsBridge> {

    @Override
    public @Nullable GroupPermissionsBridge createSafely() {
        try {
            LuckPerms api = LuckPermsProvider.get();
            return new LuckPermsGroupPermissionsBridge(api);
        }
        catch (IllegalStateException | NoClassDefFoundError e) {
            return null;
        }
    }
}
