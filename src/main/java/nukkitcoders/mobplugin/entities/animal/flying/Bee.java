package nukkitcoders.mobplugin.entities.animal.flying;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityArthropod;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.potion.Effect;
import cn.nukkit.utils.Utils;
import nukkitcoders.mobplugin.entities.monster.FlyingMonster;

import java.util.HashMap;

public class Bee extends FlyingMonster implements EntityArthropod {

    public static final int NETWORK_ID = 122;

    private int angry;
    private int dieInTicks = -1;

    public Bee(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public int getKillExperience() {
        return Utils.rand(1, 3);
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public float getWidth() {
        if (this.isBaby()) {
            return 0.275f;
        }
        return 0.55f;
    }

    @Override
    public float getHeight() {
        if (this.isBaby()) {
            return 0.25f;
        }
        return 0.5f;
    }

    @Override
    public void initEntity() {
        this.setMaxHealth(10);
        super.initEntity();

        this.setDamage(new float[]{0, 2, 2, 3});
    }

    @Override
    public double getSpeed() {
        return this.angry > 0 ? 1.5 : 1;
    }

    @Override
    public void attackEntity(Entity player) {
        if (this.attackDelay > 23 && this.distanceSquared(player) < 1.3) {
            this.attackDelay = 0;

            this.target = this.followTarget = null;
            this.dieInTicks = 500;
            this.setAngry(0);

            HashMap<EntityDamageEvent.DamageModifier, Float> damage = new HashMap<>();
            damage.put(EntityDamageEvent.DamageModifier.BASE, this.getDamage());
            if (player instanceof Player) {
                float points = 0;
                for (Item i : ((Player) player).getInventory().getArmorContents()) {
                    points += this.getArmorPoints(i.getId());
                }
                damage.put(EntityDamageEvent.DamageModifier.ARMOR,
                        (float) (damage.getOrDefault(EntityDamageEvent.DamageModifier.ARMOR, 0f) - Math.floor(
                                damage.getOrDefault(EntityDamageEvent.DamageModifier.BASE, 1f) * points * 0.04)));
            }
            if (player.attack(new EntityDamageByEntityEvent(this, player, EntityDamageEvent.DamageCause.ENTITY_ATTACK, damage))) {
                if (this.getServer().getDifficulty() == 2) {
                    player.addEffect(Effect.getEffect(Effect.POISON).setDuration(200));
                } else if (this.getServer().getDifficulty() == 3) {
                    player.addEffect(Effect.getEffect(Effect.POISON).setDuration(360));
                }
            }
        }
    }

    @Override
    public boolean targetOption(EntityCreature creature, double distance) {
        return this.isAngry() && super.targetOption(creature, distance);
    }

    public boolean isAngry() {
        return this.angry > 0;
    }

    public void setAngry(int ticks) {
        this.followBlock = null;
        this.angry = ticks;
        this.setDataFlag(DATA_FLAGS, DATA_FLAG_ANGRY, this.angry > 0);
    }

    public boolean hasSting() {
        return dieInTicks == -1;
    }

    public void setAngry(Entity entity) {
        setAngry(500);
        target(entity);
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        if (source.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
            if (ticksLived < 10) {
                source.setCancelled();
                return false;
            }
        }

        if (source instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) source;
            for (Entity entity : getLevel().getCollidingEntities(this.getBoundingBox().grow(8, 8, 8))) {
                if (entity instanceof Bee && ((Bee) entity).hasSting()) {
                    ((Bee) entity).setAngry(event.getDamager());
                }
            }
        }

        return super.attack(source);
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (closed) {
            return false;
        }

        if (!hasSting() && isAlive()) {
            dieInTicks--;
            if (dieInTicks <= 0) {
                kill();
            }
        }

        if (this.angry > 0) {
            if (this.angry == 1) {
                this.setAngry(0); // Reset flag
            } else {
                this.angry--;
            }
        }

        return super.onUpdate(currentTick);
    }

    @Override
    public void saveNBT() {
        super.saveNBT();
    }

    @Override
    public boolean canDespawn() {
        return false;
    }

    private void target(Vector3 pos) {
        this.target = this.followBlock = pos;
        this.stayTime = 0;
    }
}
