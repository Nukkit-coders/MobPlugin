package nukkitcoders.mobplugin.entities.monster.walking;

import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;

public class ZombieVillager extends Zombie {

    public static final int NETWORK_ID = 44;

    public ZombieVillager(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public String getName() {
        return this.hasCustomName() ? this.getNameTag() : "Zombie Villager";
    }

    @Override
    public boolean canDespawn() {
        return false;
    }
}
