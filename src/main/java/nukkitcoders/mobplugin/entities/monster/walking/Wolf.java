package nukkitcoders.mobplugin.entities.monster.walking;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.data.ByteEntityData;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemDye;
import cn.nukkit.item.ItemID;
import cn.nukkit.level.Sound;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.ItemBreakParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.EntityEventPacket;
import cn.nukkit.utils.DyeColor;
import nukkitcoders.mobplugin.MobPlugin;
import nukkitcoders.mobplugin.entities.animal.jumping.Rabbit;
import nukkitcoders.mobplugin.entities.animal.swimming.Turtle;
import nukkitcoders.mobplugin.entities.animal.walking.Fox;
import nukkitcoders.mobplugin.entities.animal.walking.Sheep;
import nukkitcoders.mobplugin.entities.monster.TameableMonster;
import nukkitcoders.mobplugin.utils.Utils;

import java.util.HashMap;

/**
 * @author GoodLucky777
 */
public class Wolf extends TameableMonster {

    public static final int NETWORK_ID = 14;

    private static final String NBT_KEY_ANGRY = "Angry";

    private static final String NBT_KEY_COLLAR_COLOR = "CollarColor";

    private boolean angry;
    private int angryDuration;
    
    private DyeColor collarColor = DyeColor.RED;
    
    private int afterInWater = -1;
    
    private final Vector3 tempVector = new Vector3();

    public Wolf(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public float getWidth() {
        return 0.6f;
    }

    @Override
    public float getHeight() {
        return 0.8f;
    }

    @Override
    public double getSpeed() {
        return 1.2;
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        
        this.setFriendly(true);

        if (this.namedTag.contains(NBT_KEY_ANGRY)) {
            if (this.namedTag.getByte(NBT_KEY_ANGRY) == 1) {
                this.setAngry(true);
            }
        }

        if (this.namedTag.contains(NBT_KEY_COLLAR_COLOR)) {
            this.collarColor = DyeColor.getByDyeData(this.namedTag.getByte(NBT_KEY_COLLAR_COLOR));
            if (this.collarColor == null) {
                this.collarColor = DyeColor.RED;
            }
        }

        this.setMaxHealth(8);
        this.setDamage(new float[] { 0, 3, 4, 6 });
    }

    @Override
    public void saveNBT() {
        super.saveNBT();

        this.namedTag.putBoolean(NBT_KEY_ANGRY, this.angry);
        this.namedTag.putByte(NBT_KEY_COLLAR_COLOR, this.collarColor.getDyeData());
    }

    @Override
    public boolean targetOption(EntityCreature creature, double distance) {
        if (!creature.isAlive() || creature.closed || distance > 256) {
            return false;
        }
        
        if (this.isAngry() && this.isAngryTo == creature.getId()) {
            return true;
        }
        
        if (creature instanceof Player) {
            if (distance <= 64 && this.isBeggingItem(((Player) creature).getInventory().getItemInHand())) {
                // TODO: Begging
                if (distance <= 9) {
                    stayTime = 40;
                }
                return true;
            } else if (this.hasOwner() && creature.equals(this.getOwner())) {
                if (distance <= 4) {
                    return false;
                } else if (distance <= 100) {
                    return true;
                }
            }
        }
        
        if (!this.hasOwner() && distance <= 256 && (
            (creature instanceof Skeleton && !Utils.entityInsideWaterFast(creature)) ||
            creature instanceof Sheep ||
            creature instanceof Rabbit ||
            creature instanceof Fox ||
            (creature instanceof Turtle && ((Turtle) creature).isBaby() && !Utils.entityInsideWaterFast(creature))
        )) {
            this.isAngryTo = creature.getId();
            this.setAngry(true);
            return true;
        } else if (this.hasOwner() && distance <= 256 && creature instanceof Skeleton) {
            this.isAngryTo = creature.getId();
            this.setAngry(true);
            return true;
        }
        
        return false;
    }

    public boolean isAngry() {
        return this.angry;
    }

    public void setAngry(boolean angry) {
        this.angry = angry;
        this.setDataFlag(DATA_FLAGS, DATA_FLAG_ANGRY, angry);
        this.angryDuration = angry ? 500 : 0;
        this.setFriendly(!angry);
    }

    @Override
    public boolean onInteract(Player player, Item item, Vector3 clickedPos) {
        int healable = this.getHealableItem(item);
        
        if (item.getId() == ItemID.BONE) {
            if (!this.hasOwner() && !this.isAngry()) {
                player.getInventory().decreaseCount(player.getInventory().getHeldItemIndex());
                if (Utils.rand(1, 3) == 3) {
                    EntityEventPacket packet = new EntityEventPacket();
                    packet.eid = this.getId();
                    packet.event = EntityEventPacket.TAME_SUCCESS;
                    player.dataPacket(packet);
                    
                    this.setMaxHealth(20);
                    this.setHealth(20);
                    this.setOwner(player);
                    this.setCollarColor(DyeColor.RED);
                    this.setRoute(null);

                    if (MobPlugin.getInstance().config.noXpOrbs) {
                        player.addExperience(Utils.rand(1, 7));
                    } else {
                        this.getLevel().dropExpOrb(this, Utils.rand(1, 7));
                    }
                    return true;
                } else {
                    EntityEventPacket packet = new EntityEventPacket();
                    packet.eid = this.getId();
                    packet.event = EntityEventPacket.TAME_FAIL;
                    player.dataPacket(packet);
                }
            }
        } else if (item.getId() == Item.DYE) {
            if (this.hasOwner() && player.equals(this.getOwner())) {
                this.setCollarColor(((ItemDye) item).getDyeColor());
                return true;
            }
        } else if (this.isBreedingItem(item) || healable != 0) {
            this.getLevel().addSound(this, Sound.RANDOM_EAT);
            this.getLevel().addParticle(new ItemBreakParticle(this.add(0, this.getMountedYOffset(), 0), Item.get(item.getId(), 0, 1)));
            if (!this.isInLoveCooldown()) {
                this.setInLove();
            }
            
            if (healable != 0) {
                this.setHealth(Math.max(this.getMaxHealth(), this.getHealth() + healable));
            }
            
            return true;
        } else if (this.hasOwner() && player.equals(this.getOwner()) && !this.isAngry()) {
            this.setSitting(!this.isSitting());
        }
        
        return super.onInteract(player, item, clickedPos);
    }

    @Override
    public boolean attack(EntityDamageEvent ev) {
        if (super.attack(ev)) {
            this.setSitting(false);
            if (ev instanceof EntityDamageByEntityEvent) {
                if (((EntityDamageByEntityEvent) ev).getDamager() instanceof Player) {
                    Player player = (Player) ((EntityDamageByEntityEvent) ev).getDamager();
                    if (!(player.isSurvival() || player.isAdventure()) || (this.hasOwner() && player.equals(this.getOwner()))) {
                        return true;
                    }
                }
                this.isAngryTo = ((EntityDamageByEntityEvent) ev).getDamager().getId();
                this.setAngry(true);
            }
            return true;
        }

        return false;
    }

    @Override
    public void attackEntity(Entity entity) {
        if (entity instanceof Player && (
            (!this.isAngry() && this.isBeggingItem(((Player) entity).getInventory().getItemInHand())) ||
            (this.hasOwner() && entity.equals(this.getOwner()))
            )
        ) {
            return;
        }
        
        if (this.attackDelay > 23 && this.distanceSquared(entity) < 1.5) {
            this.attackDelay = 0;
            HashMap<EntityDamageEvent.DamageModifier, Float> damage = new HashMap<>();
            damage.put(EntityDamageEvent.DamageModifier.BASE, this.getDamage());

            if (entity instanceof Player) {
                float points = 0;
                for (Item i : ((Player) entity).getInventory().getArmorContents()) {
                    points += this.getArmorPoints(i.getId());
                }

                damage.put(EntityDamageEvent.DamageModifier.ARMOR,
                        (float) (damage.getOrDefault(EntityDamageEvent.DamageModifier.ARMOR, 0f) - Math.floor(damage.getOrDefault(EntityDamageEvent.DamageModifier.BASE, 1f) * points * 0.04)));
            }
            
            this.setMotion(tempVector.setComponents(0, this.getGravity() * 5, 0)); // TODO: Jump before attack
            
            entity.attack(new EntityDamageByEntityEvent(this, entity, EntityDamageEvent.DamageCause.ENTITY_ATTACK, damage));
        }
    }
    
    @Override
    public boolean entityBaseTick(int tickDiff) {
        boolean hasUpdate = super.entityBaseTick(tickDiff);
        
        if (this.angryDuration == 1) {
            this.setAngry(false);
        } else if (this.angryDuration > 0) {
            this.angryDuration--;
        }
        
        if (Utils.entityInsideWaterFast(this)) {
            afterInWater = 0;
        } else if (afterInWater != -1) {
            afterInWater++;
        }
        
        if (afterInWater > 60) {
            afterInWater = -1;
            
            this.stayTime = 40;
            
            EntityEventPacket packet = new EntityEventPacket();
            packet.eid = this.getId();
            packet.event = EntityEventPacket.SHAKE_WET;
            Server.broadcastPacket(this.getViewers().values(), packet);
        }
        
        return hasUpdate;
    }
    
    @Override
    protected void checkTarget() {
        if (this.isKnockback()) {
            return;
        }
        
        if (!this.isSitting() && this.hasOwner() && this.distanceSquared(this.getOwner()) > 144) {
            this.setAngry(false);
            this.setRoute(null);
            // TODO: Safe teleport (on ground)
            this.teleport(this.getOwner());
            this.move(0, 0.0001, 0); // To fix floating problem
            return;
        }

        if (this.followTarget instanceof EntityCreature && !this.followTarget.closed && this.followTarget.isAlive() && this.targetOption((EntityCreature) this.followTarget, this.distanceSquared(this.followTarget))) {
            return;
        }

        double near = Integer.MAX_VALUE;

        for (Entity entity : this.getLevel().getNearbyEntities(this.getBoundingBox().grow(17, 17, 17), this)) {
            if (!(entity instanceof EntityCreature)) {
                continue;
            }

            EntityCreature creature = (EntityCreature) entity;
            
            double distance = this.distanceSquared(creature);
            if (distance > near || !this.targetOption(creature, distance)) {
                continue;
            }
            near = distance;

            this.stayTime = 0;
            this.moveTime = 0;
            this.followTarget = creature;
            if (this.route == null && this.passengers.isEmpty()) {
                this.target = creature;
            }
        }

        if (this.target != null) {
            return;
        }

        int x, z;
        if (this.stayTime > 0) {
            if (Utils.rand(1, 100) > 5) {
                return;
            }
            x = Utils.rand(10, 30);
            z = Utils.rand(10, 30);
            this.target = this.add(Utils.rand() ? x : -x, Utils.rand(-20.0, 20.0) / 10, Utils.rand() ? z : -z);
        } else if (Utils.rand(1, 100) == 1) {
            x = Utils.rand(10, 30);
            z = Utils.rand(10, 30);
            this.stayTime = Utils.rand(100, 200);
            this.target = this.add(Utils.rand() ? x : -x, Utils.rand(-20.0, 20.0) / 10, Utils.rand() ? z : -z);
        } else if (this.moveTime <= 0 || this.target == null) {
            x = Utils.rand(20, 100);
            z = Utils.rand(20, 100);
            this.stayTime = 0;
            this.moveTime = Utils.rand(100, 200);
            this.target = this.add(Utils.rand() ? x : -x, 0, Utils.rand() ? z : -z);
        }
    }

    @Override
    public int getKillExperience() {
        return this.isBaby() ? 0 : 3;
    }

    public void setCollarColor(DyeColor color) {
        this.namedTag.putByte(NBT_KEY_COLLAR_COLOR, color.getDyeData());
        this.setDataProperty(new ByteEntityData(DATA_COLOUR, color.getWoolData()));
        this.collarColor = color;
    }
    
    @Override
    public boolean canDespawn() {
        if (this.hasOwner(false)) {
            return false;
        }
        return super.canDespawn();
    }
    
    public boolean isBeggingItem(Item item) {
        return item.getId() == ItemID.BONE ||
            item.getId() == ItemID.RAW_CHICKEN ||
            item.getId() == ItemID.COOKED_CHICKEN ||
            item.getId() == ItemID.RAW_BEEF ||
            item.getId() == ItemID.COOKED_BEEF ||
            item.getId() == ItemID.RAW_MUTTON ||
            item.getId() == ItemID.COOKED_MUTTON ||
            item.getId() == ItemID.RAW_PORKCHOP ||
            item.getId() == ItemID.COOKED_PORKCHOP ||
            item.getId() == ItemID.RAW_RABBIT ||
            item.getId() == ItemID.COOKED_RABBIT ||
            item.getId() == ItemID.ROTTEN_FLESH;
    }
    
    public boolean isBreedingItem(Item item) {
        return item.getId() == ItemID.RAW_CHICKEN ||
            item.getId() == ItemID.COOKED_CHICKEN ||
            item.getId() == ItemID.RAW_BEEF ||
            item.getId() == ItemID.COOKED_BEEF ||
            item.getId() == ItemID.RAW_MUTTON ||
            item.getId() == ItemID.COOKED_MUTTON ||
            item.getId() == ItemID.RAW_PORKCHOP ||
            item.getId() == ItemID.COOKED_PORKCHOP ||
            item.getId() == ItemID.RAW_RABBIT ||
            item.getId() == ItemID.COOKED_RABBIT ||
            item.getId() == ItemID.ROTTEN_FLESH;
    }
    
    public int getHealableItem(Item item) {
        switch (item.getId()) {
            case ItemID.RAW_PORKCHOP:
            case ItemID.RAW_BEEF:
            case ItemID.RAW_RABBIT:
                return 3;
            case ItemID.COOKED_PORKCHOP:
            case ItemID.COOKED_BEEF:
                return 8;
            case ItemID.RAW_FISH:
            case ItemID.RAW_SALMON:
            case ItemID.RAW_CHICKEN:
            case ItemID.RAW_MUTTON:
                return 2;
            case ItemID.CLOWNFISH:
            case ItemID.PUFFERFISH:
                return 1;
            case ItemID.COOKED_FISH:
            case ItemID.COOKED_RABBIT:
                return 5;
            case ItemID.COOKED_SALMON:
            case ItemID.COOKED_CHICKEN:
            case ItemID.COOKED_MUTTON:
                return 6;
            case ItemID.ROTTEN_FLESH:
                return 4;
            case ItemID.RABBIT_STEW:
                return 10;
            default:
                return 0;
        }
    }

    @Override
    public boolean canTarget(Entity entity) {
        return true;
    }
}
