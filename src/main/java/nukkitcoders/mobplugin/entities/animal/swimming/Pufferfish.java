package nukkitcoders.mobplugin.entities.animal.swimming;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.ByteEntityData;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.potion.Effect;
import nukkitcoders.mobplugin.utils.Utils;

public class Pufferfish extends Fish {

    public static final int NETWORK_ID = 108;

    private int puffed = 0;

    public Pufferfish(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    int getBucketMeta() {
        return 5;
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public float getWidth() {
        return 0.35f;
    }

    @Override
    public float getHeight() {
        return 0.35f;
    }

    @Override
    public void initEntity() {
        this.setMaxHealth(3);
        super.initEntity();
    }

    @Override
    public Item[] getDrops() {
        return new Item[]{Item.get(Item.PUFFERFISH, 0, 1), Item.get(Item.BONE, 0, Utils.rand(0, 2))};
    }

    @Override
    public boolean attack(EntityDamageEvent ev) {
        super.attack(ev);
        
        if (ev.getCause() != DamageCause.ENTITY_ATTACK) {
            return true;
        }
        
        if (ev instanceof EntityDamageByEntityEvent) {            
            Entity damager = ((EntityDamageByEntityEvent) ev).getDamager();
            if (damager instanceof Player) {
                if (this.puffed > 0) {
                    return true;
                }
                this.puffed = 200;
                damager.addEffect(Effect.getEffect(Effect.POISON).setDuration(200));
                this.setDataProperty(new ByteEntityData(DATA_PUFFERFISH_SIZE, 2));
            }
        }

        return true;
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        if (puffed == 0) {
            if (this.getDataPropertyByte(DATA_PUFFERFISH_SIZE) == 2) {
                this.setDataProperty(new ByteEntityData(DATA_PUFFERFISH_SIZE, 0));
            }
        }

        if (puffed > 0) {
            puffed--;
        }

        return super.entityBaseTick(tickDiff);
    }

    public boolean isPuffed() {
        return this.puffed > 0;
    }
}
