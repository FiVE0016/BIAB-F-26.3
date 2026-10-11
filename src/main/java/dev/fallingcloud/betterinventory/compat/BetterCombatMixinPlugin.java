package dev.fallingcloud.betterinventory.compat;

import com.mojang.logging.LogUtils;
import dev.fallingcloud.betterinventory.platform.ModPresence;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** 1.4.1: gates the Better Combat mixins behind the bettercombat mod. */
public final class BetterCombatMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BETTER_COMBAT = "bettercombat";
    private boolean active;

    @Override
    public void onLoad(String mixinPackage) {
        this.active = ModPresence.isLoaded(BETTER_COMBAT);
        LOGGER.info("[Better Inventory] Better Combat patch {}.",
                this.active ? "active" : "dormant - bettercombat not installed");
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return this.active;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        LOGGER.info("[Better Inventory] Better Combat patch applied: {} -> {}", mixinClassName, targetClassName);
    }
}
