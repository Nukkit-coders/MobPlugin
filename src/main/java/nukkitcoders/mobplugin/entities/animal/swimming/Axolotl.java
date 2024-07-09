package nukkitcoders.mobplugin.entities.animal.swimming;

import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;

public class Axolotl extends Fish {

    public static final int NETWORK_ID = 130;

    public Axolotl(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public void initEntity() {
        this.setMaxHealth(14);
        super.initEntity();
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public float getWidth() {
        return 0.75f;
    }

    @Override
    public float getHeight() {
        return 0.42f;
    }

    @Override
    int getBucketMeta() {
        return 12;
    }
}
