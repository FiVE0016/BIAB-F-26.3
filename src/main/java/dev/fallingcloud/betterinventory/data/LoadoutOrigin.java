package dev.fallingcloud.betterinventory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record LoadoutOrigin(UUID owner, String section, int slot) {
    public static final Codec<LoadoutOrigin> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    UUIDUtil.CODEC.fieldOf("owner").forGetter(LoadoutOrigin::owner),
                    Codec.STRING.fieldOf("section").forGetter(LoadoutOrigin::section),
                    Codec.INT.fieldOf("slot").forGetter(LoadoutOrigin::slot)
            ).apply(instance, LoadoutOrigin::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, LoadoutOrigin> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodec(UUIDUtil.STRING_CODEC), LoadoutOrigin::owner,
            ByteBufCodecs.STRING_UTF8, LoadoutOrigin::section,
            ByteBufCodecs.INT, LoadoutOrigin::slot,
            LoadoutOrigin::new
    );
}
