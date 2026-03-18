package me.lianecx.discordlinker.common.hooks;

import org.jetbrains.annotations.Nullable;

public final class HookLoader<T> {

    private final HookProvider<? extends T>[] providers;

    @SafeVarargs
    public HookLoader(HookProvider<? extends T>... providers) {
        this.providers = providers;
    }

    public @Nullable T load() {
        for(HookProvider<? extends T> provider : providers) {
            try {
                T bridge = provider.createSafely();
                if(bridge != null) return bridge;
            }
            catch (LinkageError | RuntimeException ignored) {
                // Failed optional provider, try next.
            }
        }
        return null;
    }
}
