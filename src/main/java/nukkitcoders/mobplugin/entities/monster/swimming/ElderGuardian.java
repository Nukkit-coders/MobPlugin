package nukkitcoders.mobplugin.entities.monster.swimming;

import cn.nukkit.Player;
import cn.nukkit.block.BlockSponge;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.LevelEventPacket;
import cn.nukkit.potion.Effect;
import nukkitcoders.mobplugin.entities.monster.SwimmingMonster;
import nukkitcoders.mobplugin.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class ElderGuardian extends SwimmingMonster {

    public static final int NETWORK_ID = 50;

    public ElderGuardian(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public float getWidth() {
        return 1.9975f;
    }

    @Override
    public float getHeight() {
        return 1.9975f;
    }

    @Override
    public void initEntity() {
        this.setMaxHealth(80);
        super.initEntity();

        this.setDataFlag(DATA_FLAGS, DATA_FLAG_ELDER, true);
    }

    @Override
    public boolean targetOption(EntityCreature creature, double distance) {
        return false;
    }

    @Override
    public void attackEntity(Entity player) {
    }

    @Override
    public Item[] getDrops() {
        List<Item> drops = new ArrayList<>();

        drops.add(Item.get(Item.PRISMARINE_SHARD, 0, Utils.rand(0, 2)));

        if (this.lastDamageCause instanceof EntityDamageByEntityEvent) {
            if (((EntityDamageByEntityEvent) this.lastDamageCause).getDamager() instanceof Player) {
                drops.add(Item.get(Item.SPONGE, BlockSponge.WET, 1));
            }
        }

        return drops.toArray(new Item[0]);
    }

    @Override
    public int getKillExperience() {
        return 10;
    }

    @Override
    public String getName() {
        return this.hasCustomName() ? this.getNameTag() : "Elder Guardian";
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        boolean result = super.entityBaseTick(tickDiff);
        if (!this.closed && this.ticksLived % 1200 == 0 && this.isAlive()) {
            for (Player p : this.level.getPlayers().values()) {
                if (p.getGamemode() % 2 == 0 && p.distanceSquared(this) < 2500 && !p.hasEffect(Effect.MINING_FATIGUE)) {
                    p.addEffect(Effect.getEffect(Effect.MINING_FATIGUE).setAmplifier(2).setDuration(6000));
                    LevelEventPacket pk = new LevelEventPacket();
                    pk.evid = LevelEventPacket.EVENT_GUARDIAN_CURSE;
                    pk.x = (float) this.x;
                    pk.y = (float) this.y;
                    pk.z = (float) this.z;
                    p.dataPacket(pk);
                }
            }
        }
        return result;
    }
}
