package me.lianecx.discordlinker.common.hooks;

import org.jetbrains.annotations.Nullable;

public interface HookProvider<T> {

    @Nullable T createSafely();
}
