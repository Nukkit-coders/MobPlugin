package nukkitcoders.mobplugin.entities;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockAnvil;
import cn.nukkit.block.BlockID;
import cn.nukkit.entity.Attribute;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityAgeable;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.data.LongEntityData;
import cn.nukkit.event.Event;
import cn.nukkit.event.entity.EntityDamageByBlockEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityInteractEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.level.GameRule;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.HeartParticle;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.NukkitMath;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.BossEventPacket;
import cn.nukkit.network.protocol.EntityEventPacket;
import cn.nukkit.network.protocol.MoveEntityAbsolutePacket;
import cn.nukkit.network.protocol.UpdateAttributesPacket;
import cn.nukkit.potion.Effect;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import nukkitcoders.mobplugin.MobPlugin;
import nukkitcoders.mobplugin.entities.animal.Animal;
import nukkitcoders.mobplugin.entities.animal.walking.Cow;
import nukkitcoders.mobplugin.entities.monster.Monster;
import nukkitcoders.mobplugin.utils.Utils;

public abstract class BaseEntity extends EntityCreature implements EntityAgeable {

    private static final Int2ObjectMap<Float> ARMOR_POINTS = new Int2ObjectOpenHashMap<Float>() {
        {
            put(Item.LEATHER_CAP, new Float(1));
            put(Item.LEATHER_TUNIC, new Float(3));
            put(Item.LEATHER_PANTS, new Float(2));
            put(Item.LEATHER_BOOTS, new Float(1));
            put(Item.CHAIN_HELMET, new Float(2));
            put(Item.CHAIN_CHESTPLATE, new Float(5));
            put(Item.CHAIN_LEGGINGS, new Float(4));
            put(Item.CHAIN_BOOTS, new Float(1));
            put(Item.GOLD_HELMET, new Float(2));
            put(Item.GOLD_CHESTPLATE, new Float(5));
            put(Item.GOLD_LEGGINGS, new Float(3));
            put(Item.GOLD_BOOTS, new Float(1));
            put(Item.IRON_HELMET, new Float(2));
            put(Item.IRON_CHESTPLATE, new Float(6));
            put(Item.IRON_LEGGINGS, new Float(5));
            put(Item.IRON_BOOTS, new Float(2));
            put(Item.DIAMOND_HELMET, new Float(3));
            put(Item.DIAMOND_CHESTPLATE, new Float(8));
            put(Item.DIAMOND_LEGGINGS, new Float(6));
            put(Item.DIAMOND_BOOTS, new Float(3));
            put(Item.NETHERITE_HELMET, new Float(3));
            put(Item.NETHERITE_CHESTPLATE, new Float(8));
            put(Item.NETHERITE_LEGGINGS, new Float(6));
            put(Item.NETHERITE_BOOTS, new Float(3));
            put(Item.TURTLE_SHELL, new Float(2));
        }
    };

    public int stayTime;
    public Item[] armor;
    protected int moveTime;
    protected int noRotateTicks;
    protected float moveMultiplier = 1.0f;
    protected Vector3 target;
    protected Entity followTarget;
    protected int attackDelay;
    protected boolean noFallDamage;
    protected Player lastInteract;
    private int airTicks = 300;
    private long leadHolder = -1L;
    private boolean baby;
    private boolean friendly;
    private boolean persistent;
    private int lastDamageTick;
    private int knockBackTime;
    private short inLoveTicks;
    //private int inEndPortal;
    //private int inNetherPortal;
    private short inLoveCooldown;

    public BaseEntity(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    public abstract Vector3 updateMove(int tickDiff);

    public abstract int getKillExperience();

    public boolean isFriendly() {
        return this.friendly;
    }

    public void setFriendly(boolean bool) {
        this.friendly = bool;
    }

    @Deprecated
    public boolean isMovement() {
        return !this.isImmobile();
    }

    @Deprecated
    public void setMovement(boolean value) {
        this.setImmobile(value);
    }

    public boolean isKnockback() {
        return this.knockBackTime > 0;
    }

    public double getSpeed() {
        if (this.isBaby()) {
            return 1.2;
        }
        return 1;
    }

    public Vector3 getTarget() {
        return this.followTarget != null ? this.followTarget : this.target;
    }

    public void setTarget(Entity target) {
        this.followTarget = target;
        this.moveTime = 0;
        this.stayTime = 0;
        this.target = null;
    }

    public void setTarget(Vector3 target) {
        if (target instanceof Entity) {
            this.setTarget((Entity) target);
            return;
        }

        this.followTarget = null;
        this.moveTime = 0;
        this.stayTime = 0;
        this.target = target;
    }

    @Deprecated
    public Vector3 getTargetVector() {
        return getTarget();
    }

    @Deprecated
    public Entity getFollowTarget() {
        Vector3 t = getTarget();
        return t instanceof Entity ? (Entity) t : null;
    }

    @Deprecated
    public void setFollowTarget(Entity target) {
        setTarget(target);
    }

    @Override
    public boolean isBaby() {
        return this.baby;
    }

    public void setBaby(boolean baby) {
        this.baby = baby;
        this.setDataFlag(DATA_FLAGS, DATA_FLAG_BABY, baby);
        if (baby) {
            this.setScale(0.5f);
            this.age = Utils.rand(-2400, -1800);
        } else {
            this.setScale(1.0f);
        }
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        this.persistent = this.namedTag.getBoolean("Persistent");

        if (this.namedTag.getBoolean("Immobile")) {
            this.setImmobile();
        }

        if (this.namedTag.contains("Age")) {
            this.age = this.namedTag.getShort("Age");
        }

        if (this.namedTag.getBoolean("Baby")) {
            this.baby = true;
            this.setDataFlag(DATA_FLAGS, DATA_FLAG_BABY, true);
        }

        if (this.namedTag.contains("InLoveTicks")) {
            this.inLoveTicks = (short) this.namedTag.getShort("InLoveTicks");
        }

        if (this.namedTag.contains("InLoveCooldown")) {
            this.inLoveCooldown = (short) this.namedTag.getShort("InLoveCooldown");
        }
    }

    @Override
    public String getName() {
        return this.hasCustomName() ? this.getNameTag() : this.getClass().getSimpleName();
    }

    public void saveNBT() {
        super.saveNBT();

        if (this.isPersistent()) {
            this.namedTag.putBoolean("Persistent", this.isPersistent());
        } else {
            this.namedTag.remove("Persistent");
        }

        if (this.isImmobile()) {
            this.namedTag.putBoolean("Immobile", this.isImmobile());
        } else {
            this.namedTag.remove("Immobile");
        }

        if (this.isBaby()) {
            this.namedTag.putBoolean("Baby", this.isBaby());
        } else {
            this.namedTag.remove("Baby");
        }

        this.namedTag.putShort("Age", this.age);

        if (this.isInLove()) {
            this.namedTag.putShort("InLoveTicks", this.inLoveTicks);
        } else {
            this.namedTag.remove("InLoveTicks");
        }
        if (this.isInLoveCooldown()) {
            this.namedTag.putShort("InLoveCooldown", this.inLoveCooldown);
        } else {
            this.namedTag.remove("InLoveCooldown");
        }
    }

    public boolean targetOption(EntityCreature creature, double distance) {
        if (this instanceof Monster) {
            if (creature instanceof Player) {
                Player player = (Player) creature;
                return !player.closed && player.spawned && player.isAlive() && (player.isSurvival() || player.isAdventure()) && distance <= 256;
            }
            return creature.isAlive() && !creature.closed && distance <= 256;
        } else if (this instanceof Animal && this.isInLove()) {
            return creature instanceof BaseEntity && ((BaseEntity) creature).isInLove() && creature.isAlive() && !creature.closed && creature.getNetworkId() == this.getNetworkId() && distance <= 256;
        }
        return false;
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        if (age % 20 == 0) {
            if (this.y > 127 && this.level.getDimension() == Level.DIMENSION_NETHER) {
                this.close();
                return false;
            }
        }

        boolean hasUpdate = super.entityBaseTick(tickDiff);

        if (this.canDespawn()) {
            this.close();
            return false;
        }

        if (this instanceof Monster && this.attackDelay < 200) {
            this.attackDelay++;
        }

        if (this.moveTime > 0) {
            this.moveTime -= tickDiff;
        }

        if (this.knockBackTime > 0) {
            this.knockBackTime -= tickDiff;
        }

        if (this.isBaby() && this.age > 0) {
            this.setBaby(false);
        }

        if (this.isInLove()) {
            this.inLoveTicks -= tickDiff;
            if (!this.isBaby() && this.age > 0 && this.age % 20 == 0) {
                for (int i = 0; i < 3; i++) {
                    this.level.addParticle(new HeartParticle(this.add(Utils.rand(-1.0, 1.0), this.getMountedYOffset() + Utils.rand(-1.0, 1.0), Utils.rand(-1.0, 1.0))));
                }
                if (MobPlugin.getInstance().config.allowBreeding) {
                    Entity[] colliding = level.getCollidingEntities(this.boundingBox.grow(0.5f, 0.5f, 0.5f));
                    for (Entity entity : colliding) {
                        if (entity != this && entity != null && this.tryBreedWih(entity)) {
                            break;
                        }
                    }
                }
            }
        } else if (this.isInLoveCooldown()) {
            this.inLoveCooldown -= tickDiff;
        }

        if (this.y > this.highestPosition) {
            this.highestPosition = this.y;
        }

        return hasUpdate;
    }

    @Override
    protected void checkBlockCollision() {
        //boolean netherPortal = false;
        //boolean endPortal = false;
        Block powderSnow = null;

        for (Block block : this.getCollisionBlocks()) {
            /*if (block.getId() == Block.NETHER_PORTAL) {
                netherPortal = true;
                continue;
            } else if (block.getId() == Block.END_PORTAL) {
                endPortal = true;
                continue;
            }*/

            block.onEntityCollide(this);

            if (block.getId() == Block.POWDER_SNOW) {
                powderSnow = block;
            }
        }

        if (powderSnow != null) {
            this.inPowderSnowTicks++;

            if (this.getFreezingDamage() > 0 && this.inPowderSnowTicks >= 140 && server.getTick() % 40 == 0 && (level.getGameRules().getBoolean(GameRule.FREEZE_DAMAGE))) {
                this.attack(new EntityDamageByBlockEvent(powderSnow, this, EntityDamageEvent.DamageCause.CONTACT, this.getFreezingDamage()));
            }
        } else if (this.inPowderSnowTicks != 0) {
            this.inPowderSnowTicks = 0;
        }

        /*if (endPortal) {
            inEndPortal++;
        } else {
            inEndPortal = 0;
        }

        if (inEndPortal == 1) {
            EntityPortalEnterEvent ev = new EntityPortalEnterEvent(this, EntityPortalEnterEvent.PortalType.END);
            this.getServer().getPluginManager().callEvent(ev);
            if (!ev.isCancelled()) {
                //TODO
            }
        }

        if (netherPortal) {
            inNetherPortal++;
        } else {
            inNetherPortal = 0;
        }

        if (inNetherPortal == 80) {
            EntityPortalEnterEvent ev = new EntityPortalEnterEvent(this, EntityPortalEnterEvent.PortalType.NETHER);
            this.getServer().getPluginManager().callEvent(ev);
            if (!ev.isCancelled()) {
                //TODO
            }
        }*/
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        if (this.isKnockback() && source instanceof EntityDamageByEntityEvent && ((EntityDamageByEntityEvent) source).getDamager() instanceof Player) {
            return false;
        }

        if (this.fireProof && (source.getCause() == EntityDamageEvent.DamageCause.FIRE || source.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK || source.getCause() == EntityDamageEvent.DamageCause.LAVA)) {
            return false;
        }

        if (source instanceof EntityDamageByEntityEvent) {
            ((EntityDamageByEntityEvent) source).setKnockBack(0.25f * getKnockbackModifier());
        }

        super.attack(source);

        if (!source.isCancelled()) {
            this.target = null;
            this.stayTime = 0;
            this.lastDamageTick = server.getTick();
        }
        return true;
    }

    /**
     * Modifier for mob knockback resistance
     * @return 1 - vanilla resistance
     */
    protected float getKnockbackModifier() {
        return 1f;
    }

    @Override
    public boolean move(double dx, double dy, double dz) {
        if (dy < -10 || dy > 10) {
            return false;
        }

        if (this.leadHolder != -1L) {
            Entity leadHolder = level.getEntity(this.leadHolder);

            if (leadHolder == null) {
                this.unleash();
            } else {
                double distance = this.distanceSquared(leadHolder);

                if (distance > 100) {
                    this.unleash();
                } else if (distance > 49) {
                    Vector3 toTarget = leadHolder.subtract(this).normalize();
                    toTarget.x *= 0.5;
                    toTarget.y *= 0.5;
                    toTarget.z *= 0.5;

                    this.setMotion(toTarget);

                    dx = toTarget.x;
                    dy = toTarget.y;
                    dz = toTarget.z;
                }
            }
        }

        if (dx == 0 && dz == 0 && dy == 0) {
            return false;
        }

        this.blocksAround = null;

        double movX = dx * moveMultiplier;
        double movY = dy;
        double movZ = dz * moveMultiplier;

        AxisAlignedBB[] list = this.level.getCollisionCubes(this, this.boundingBox.addCoord(dx, dy, dz), false);

        for (AxisAlignedBB bb : list) {
            dx = bb.calculateXOffset(this.boundingBox, dx);
        }
        this.boundingBox.offset(dx, 0, 0);

        for (AxisAlignedBB bb : list) {
            dz = bb.calculateZOffset(this.boundingBox, dz);
        }
        this.boundingBox.offset(0, 0, dz);

        for (AxisAlignedBB bb : list) {
            dy = bb.calculateYOffset(this.boundingBox, dy);
        }
        this.boundingBox.offset(0, dy, 0);

        this.setComponents(this.x + dx, this.y + dy, this.z + dz);
        this.checkChunks();

        this.checkGroundState(movX, movY, movZ, dx, dy, dz);
        this.updateFallState(this.onGround);

        return true;
    }

    /**
     * Get mounted entity y offset. Used to determine the height for heart particle spawning.
     *
     * @return entity height * 0.75
     */
    protected float getMountedYOffset() {
        return getHeight() * 0.75F;
    }

    /**
     * Get a random set of armor
     *
     * @return armor items
     */
    protected Item[] getRandomArmor() {
        Item[] slots = new Item[4];
        Item helmet = Item.get(0);
        Item chestplate = Item.get(0);
        Item leggings = Item.get(0);
        Item boots = Item.get(0);

        switch (Utils.rand(1, 5)) {
            case 1:
                if (Utils.rand(1, 100) < 39) {
                    if (Utils.rand(0, 1) == 0) {
                        helmet = Item.get(Item.LEATHER_CAP, Utils.rand(30, 48), 1);
                    }
                }
                break;
            case 2:
                if (Utils.rand(1, 100) < 50) {
                    if (Utils.rand(0, 1) == 0) {
                        helmet = Item.get(Item.GOLD_HELMET, Utils.rand(40, 70), 1);
                    }
                }
                break;
            case 3:
                if (Utils.rand(1, 100) < 14) {
                    if (Utils.rand(0, 1) == 0) {
                        helmet = Item.get(Item.CHAIN_HELMET, Utils.rand(100, 160), 1);
                    }
                }
                break;
            case 4:
                if (Utils.rand(1, 100) < 3) {
                    if (Utils.rand(0, 1) == 0) {
                        helmet = Item.get(Item.IRON_HELMET, Utils.rand(100, 160), 1);
                    }
                }
                break;
            case 5:
                if (Utils.rand(1, 100) == 100) {
                    if (Utils.rand(0, 1) == 0) {
                        helmet = Item.get(Item.DIAMOND_HELMET, Utils.rand(190, 256), 1);
                    }
                }
                break;
        }

        slots[0] = helmet;

        if (Utils.rand(1, 4) != 1) {
            switch (Utils.rand(1, 5)) {
                case 1:
                    if (Utils.rand(1, 100) < 39) {
                        if (Utils.rand(0, 1) == 0) {
                            chestplate = Item.get(Item.LEATHER_TUNIC, Utils.rand(60, 73), 1);
                        }
                    }
                    break;
                case 2:
                    if (Utils.rand(1, 100) < 50) {
                        if (Utils.rand(0, 1) == 0) {
                            chestplate = Item.get(Item.GOLD_CHESTPLATE, Utils.rand(65, 105), 1);
                        }
                    }
                    break;
                case 3:
                    if (Utils.rand(1, 100) < 14) {
                        if (Utils.rand(0, 1) == 0) {
                            chestplate = Item.get(Item.CHAIN_CHESTPLATE, Utils.rand(170, 233), 1);
                        }
                    }
                    break;
                case 4:
                    if (Utils.rand(1, 100) < 3) {
                        if (Utils.rand(0, 1) == 0) {
                            chestplate = Item.get(Item.IRON_CHESTPLATE, Utils.rand(170, 233), 1);
                        }
                    }
                    break;
                case 5:
                    if (Utils.rand(1, 100) == 100) {
                        if (Utils.rand(0, 1) == 0) {
                            chestplate = Item.get(Item.DIAMOND_CHESTPLATE, Utils.rand(421, 521), 1);
                        }
                    }
                    break;
            }
        }

        slots[1] = chestplate;

        if (Utils.rand(1, 2) == 2) {
            switch (Utils.rand(1, 5)) {
                case 1:
                    if (Utils.rand(1, 100) < 39) {
                        if (Utils.rand(0, 1) == 0) {
                            leggings = Item.get(Item.LEATHER_PANTS, Utils.rand(35, 68), 1);
                        }
                    }
                    break;
                case 2:
                    if (Utils.rand(1, 100) < 50) {
                        if (Utils.rand(0, 1) == 0) {
                            leggings = Item.get(Item.GOLD_LEGGINGS, Utils.rand(50, 98), 1);
                        }
                    }
                    break;
                case 3:
                    if (Utils.rand(1, 100) < 14) {
                        if (Utils.rand(0, 1) == 0) {
                            leggings = Item.get(Item.CHAIN_LEGGINGS, Utils.rand(170, 218), 1);
                        }
                    }
                    break;
                case 4:
                    if (Utils.rand(1, 100) < 3) {
                        if (Utils.rand(0, 1) == 0) {
                            leggings = Item.get(Item.IRON_LEGGINGS, Utils.rand(170, 218), 1);
                        }
                    }
                    break;
                case 5:
                    if (Utils.rand(1, 100) == 100) {
                        if (Utils.rand(0, 1) == 0) {
                            leggings = Item.get(Item.DIAMOND_LEGGINGS, Utils.rand(388, 488), 1);
                        }
                    }
                    break;
            }
        }

        slots[2] = leggings;

        if (Utils.rand(1, 5) < 3) {
            switch (Utils.rand(1, 5)) {
                case 1:
                    if (Utils.rand(1, 100) < 39) {
                        if (Utils.rand(0, 1) == 0) {
                            boots = Item.get(Item.LEATHER_BOOTS, Utils.rand(35, 58), 1);
                        }
                    }
                    break;
                case 2:
                    if (Utils.rand(1, 100) < 50) {
                        if (Utils.rand(0, 1) == 0) {
                            boots = Item.get(Item.GOLD_BOOTS, Utils.rand(50, 86), 1);
                        }
                    }
                    break;
                case 3:
                    if (Utils.rand(1, 100) < 14) {
                        if (Utils.rand(0, 1) == 0) {
                            boots = Item.get(Item.CHAIN_BOOTS, Utils.rand(100, 188), 1);
                        }
                    }
                    break;
                case 4:
                    if (Utils.rand(1, 100) < 3) {
                        if (Utils.rand(0, 1) == 0) {
                            boots = Item.get(Item.IRON_BOOTS, Utils.rand(100, 188), 1);
                        }
                    }
                    break;
                case 5:
                    if (Utils.rand(1, 100) == 100) {
                        if (Utils.rand(0, 1) == 0) {
                            boots = Item.get(Item.DIAMOND_BOOTS, Utils.rand(350, 428), 1);
                        }
                    }
                    break;
            }
        }

        slots[3] = boots;

        return slots;
    }

    /**
     * Increases mob's health according to armor the mob has (temporary workaround until armor damage modifiers are implemented for mobs)
     */
    protected void addArmorExtraHealth() {
        if (this.armor != null && this.armor.length == 4) {
            switch (armor[0].getId()) {
                case Item.LEATHER_CAP:
                    this.addHealth(1);
                    break;
                case Item.GOLD_HELMET:
                case Item.CHAIN_HELMET:
                case Item.IRON_HELMET:
                    this.addHealth(2);
                    break;
                case Item.DIAMOND_HELMET:
                    this.addHealth(3);
                    break;
            }
            switch (armor[1].getId()) {
                case Item.LEATHER_TUNIC:
                    this.addHealth(2);
                    break;
                case Item.GOLD_CHESTPLATE:
                case Item.CHAIN_CHESTPLATE:
                case Item.IRON_CHESTPLATE:
                    this.addHealth(3);
                    break;
                case Item.DIAMOND_CHESTPLATE:
                    this.addHealth(4);
                    break;
            }
            switch (armor[2].getId()) {
                case Item.LEATHER_PANTS:
                    this.addHealth(1);
                    break;
                case Item.GOLD_LEGGINGS:
                case Item.CHAIN_LEGGINGS:
                case Item.IRON_LEGGINGS:
                    this.addHealth(2);
                    break;
                case Item.DIAMOND_LEGGINGS:
                    this.addHealth(3);
                    break;
            }
            switch (armor[3].getId()) {
                case Item.LEATHER_BOOTS:
                    this.addHealth(1);
                    break;
                case Item.GOLD_BOOTS:
                case Item.CHAIN_BOOTS:
                case Item.IRON_BOOTS:
                    this.addHealth(2);
                    break;
                case Item.DIAMOND_BOOTS:
                    this.addHealth(3);
                    break;
            }
        }
    }

    /**
     * Increase the maximum health and health. Used for armored mobs.
     *
     * @param health amount of health to add
     */
    private void addHealth(int health) {
        boolean wasMaxHealth = this.getHealth() == this.getMaxHealth();
        this.setMaxHealth(this.getMaxHealth() + health);
        if (wasMaxHealth) {
            this.setHealth(this.getHealth() + health);
        }
    }

    /**
     * Check whether a mob is allowed to despawn
     *
     * @return can despawn
     */
    protected boolean canDespawn() {
        return this.y < -128 || (MobPlugin.getInstance().config.despawnMobs &&
                !this.persistent && this.age % 100 == 0 && this.riding == null && this.inLoveTicks <= 0 && this.inLoveCooldown <= 0 &&
                !this.isLeashed() && !this.hasCustomName() && server.getTick() - this.lastDamageTick > 600 && // no damage in 30 seconds
                !this.isInTickingRange(9216d) // 96 blocks
        );
    }

    /**
     * How near a player the mob should get before it starts attacking
     *
     * @return distance
     */
    protected int nearbyDistanceMultiplier() {
        return 1;
    }

    @Override
    public int getAirTicks() {
        return this.airTicks;
    }

    @Override
    public void setAirTicks(int ticks) {
        this.airTicks = ticks;
    }

    @Override
    public void addMovement(double x, double y, double z, double yaw, double pitch, double headYaw) {
        MoveEntityAbsolutePacket pk = new MoveEntityAbsolutePacket();
        pk.eid = this.id;
        pk.x = (float) x;
        pk.y = (float) y;
        pk.z = (float) z;
        pk.yaw = (float) yaw;
        pk.headYaw = (float) headYaw;
        pk.pitch = (float) pitch;
        pk.onGround = this.onGround;
        for (Player p : this.hasSpawned.values()) {
            p.dataPacket(pk);
        }
    }

    @Override
    protected void checkGroundState(double movX, double movY, double movZ, double dx, double dy, double dz) {
        if (onGround && movX == 0 && movY == 0 && movZ == 0 && dx == 0 && dy == 0 && dz == 0) {
            return;
        }
        this.isCollidedVertically = movY != dy;
        this.isCollidedHorizontally = (movX != dx || movZ != dz);
        this.isCollided = (this.isCollidedHorizontally || this.isCollidedVertically);
        this.onGround = (movY != dy && movY < 0);
    }

    @Override
    public void resetFallDistance() {
        this.highestPosition = this.y;
    }

    @Override
    public boolean setMotion(Vector3 motion) {
        this.motionX = motion.x;
        this.motionY = motion.y;
        this.motionZ = motion.z;
        if (!this.justCreated) {
            this.updateMovement();
        }
        return true;
    }

    public boolean canTarget(Entity entity) {
        return entity instanceof Player && !entity.closed;
    }

    protected float getFreezingDamage() {
        return 1f;
    }

    /**
     * Get armor defense points for item
     *
     * @param item item id
     * @return defense points
     */
    protected float getArmorPoints(int item) {
        Float points = ARMOR_POINTS.get(item);
        if (points == null) {
            return 0;
        }
        return points;
    }

    /**
     * Play attack animation to viewers
     */
    protected void playAttack() {
        EntityEventPacket pk = new EntityEventPacket();
        pk.eid = this.getId();
        pk.event = EntityEventPacket.ARM_SWING;
        Server.broadcastPacket(this.getViewers().values(), pk);
    }

    @Override
    public void fall(float fallDistance) {
        if (fallDistance > 0.75) {
            if (!this.hasEffect(Effect.SLOW_FALLING)) {
                if (Utils.entityInsideWaterFast(this)) {
                    return;
                }
                int floor = level.getBlockIdAt(this.chunk, getFloorX(), getFloorY() - 1, getFloorZ());
                if (!this.noFallDamage) {
                    float damage = (float) Math.floor(fallDistance - 3 - (this.hasEffect(Effect.JUMP) ? this.getEffect(Effect.JUMP).getAmplifier() + 1 : 0));
                    if (floor == BlockID.HAY_BALE) {
                        damage -= (damage * 0.8f);
                    }
                    if (damage > 0) {
                        this.attack(new EntityDamageEvent(this, EntityDamageEvent.DamageCause.FALL, damage));
                    }
                }
                if (floor == BlockID.FARMLAND) {
                    Vector3 pos = this.down();
                    Block down = this.level.getBlock(this.chunk, pos.getFloorX(), pos.getFloorY(), pos.getFloorZ(), true);
                    Event ev = new EntityInteractEvent(this, down);
                    this.server.getPluginManager().callEvent(ev);
                    if (ev.isCancelled()) {
                        return;
                    }
                    this.level.setBlock(down, Block.get(BlockID.DIRT), false, true);
                }
            }
        }
    }

    /**
     * Override this to allow the mob to swim in lava
     *
     * @param block block id
     * @return can swim
     */
    protected boolean canSwimIn(int block) {
        return block == BlockID.WATER || block == BlockID.STILL_WATER;
    }

    public boolean isInLoveCooldown() {
        return inLoveCooldown > 0;
    }

    protected boolean tryBreedWih(Entity entity) {
        if (entity instanceof BaseEntity && entity.getNetworkId() == this.getNetworkId()) {
            BaseEntity be = (BaseEntity) entity;
            if (be.isInLove() && !be.isBaby() && be.age > 0) {
                Player pl = be.lastInteract;
                be.lastInteract = null;
                this.setInLove(false);
                be.setInLove(false);
                this.inLoveCooldown = 1200;
                be.inLoveCooldown = 1200;
                this.stayTime = 60;
                be.stayTime = 60;
                BaseEntity baby = (BaseEntity) Entity.createEntity(this.getNetworkId(), this);
                baby.setBaby(true);
                baby.setPersistent(true); // TODO: different flag for this?
                baby.spawnToAll();
                if (baby instanceof Cow) {
                    if (pl != null) {
                        pl.awardAchievement("breedCow");
                    }
                }
                if (MobPlugin.getInstance().config.noXpOrbs) {
                    if (pl != null) {
                        pl.addExperience(Utils.rand(1, 7));
                    }
                } else {
                    this.level.dropExpOrb(this, Utils.rand(1, 7));
                }
                return true;
            }
        }
        return false;
    }

    public void setInLove() {
        this.setInLove(true);
    }

    public boolean isInLove() {
        return inLoveTicks > 0;
    }

    public void setInLove(boolean inLove) {
        if (inLove) {
            if (!this.isBaby()) {
                this.inLoveTicks = 600;
                //this.setDataFlag(DATA_FLAGS, DATA_FLAG_INLOVE, true);
            }
            this.setPersistent(true); // TODO: different flag for this?
        } else {
            this.inLoveTicks = 0;
            //this.setDataFlag(DATA_FLAGS, DATA_FLAG_INLOVE, false);
        }
    }

    protected boolean isInTickingRange(double rangeSquared) {
        for (Player player : this.level.getPlayers().values()) {
            // Ignore y so mobs won't stop falling into void unless movement behavior is tweaked for this
            if (Math.pow(player.x - this.x, 2.0) + Math.pow(player.z - this.z, 2.0) < rangeSquared) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void knockBack(Entity attacker, double damage, double x, double z, double base) {
        super.knockBack(attacker, damage, x, z, base);

        this.knockBackTime = 10;
    }

    @Override
    protected boolean applyNameTag(Player player, Item nameTag) {
        String name = nameTag.getCustomName();

        if (!name.isEmpty()) {
            this.namedTag.putString("CustomName", name);
            this.namedTag.putBoolean("CustomNameVisible", true);
            this.setNameTag(name);
            this.setNameTagVisible(true);
            return true; // onInteract: true = decrease count
        }

        return false;
    }

    public boolean isLeashed() {
        return this.leadHolder != -1L;
    }

    public void leash(Entity leadHolder) {
        this.leadHolder = leadHolder.getId();
        this.setDataProperty(new LongEntityData(DATA_LEAD_HOLDER_EID, this.leadHolder));
        this.setDataFlag(DATA_FLAGS, DATA_FLAG_LEASHED, true);
    }

    public void unleash() {
        this.leadHolder = -1L;
        this.setDataProperty(new LongEntityData(DATA_LEAD_HOLDER_EID, this.leadHolder));
        this.setDataFlag(DATA_FLAGS, DATA_FLAG_LEASHED, false);
        this.level.dropItem(this.add(0, 0.5, 0), Item.get(ItemID.LEAD));

        EntityEventPacket pk = new EntityEventPacket();
        pk.eid = this.getId();
        pk.event = EntityEventPacket.REMOVE_LEASH;
        Server.broadcastPacket(this.hasSpawned.values(), pk);
    }

    protected void sendHealthToRider() {
        for (Entity entity : this.passengers) {
            if (entity instanceof Player) {
                UpdateAttributesPacket pk = new UpdateAttributesPacket();
                int max = this.getMaxHealth();
                pk.entries = new Attribute[]{Attribute.getAttribute(Attribute.MAX_HEALTH).setMaxValue(max).setValue(this.health < max ? this.health : max)};
                pk.entityId = this.id;
                ((Player) entity).dataPacket(pk);
            }
        }
    }

    protected boolean shouldMobBurn() {
        if (this.closed || !this.isAlive()) {
            return false;
        }
        if (level.getDimension() != Level.DIMENSION_OVERWORLD || level.isRaining()) {
            return false;
        }
        if (this.fireTicks > 5 || this.age % 5 != 0) {
            return false;
        }
        int time = level.getTime() % Level.TIME_FULL;
        return (time < 12542 || time >= 23460) && !this.isInsideOfWater() && this.canSeeSky();
    }

    public boolean isPersistent() {
        return this.persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    protected boolean seesTarget(Vector3 target) {
        if (target instanceof Entity) {
            Entity entity = (Entity) target;
            if (this.age % 2 == 0) {
                return this.getSeenPercentOverZero(this.add(0, 1.5, 0), entity);
            }
            return this.getSeenPercentOverZero(this.add(0, 0.5, 0), entity);
        }

        return true;
    }

    private boolean getSeenPercentOverZero(Vector3 source, Entity entity) {
        AxisAlignedBB bb = entity.getBoundingBox();

        if (bb.isVectorInside(source)) {
            return true;
        }

        double x = 1 / ((bb.getMaxX() - bb.getMinX()) * 2 + 1);
        double y = 1 / ((bb.getMaxY() - bb.getMinY()) * 2 + 1);
        double z = 1 / ((bb.getMaxZ() - bb.getMinZ()) * 2 + 1);

        double xOffset = (1 - Math.floor(1 / x) * x) / 2;
        double yOffset = (1 - Math.floor(1 / y) * y) / 2;
        double zOffset = (1 - Math.floor(1 / z) * z) / 2;

        for (double i = 0; i <= 1; i += x) {
            for (double j = 0; j <= 1; j += y) {
                for (double k = 0; k <= 1; k += z) {
                    Vector3 target = new Vector3(
                            bb.getMinX() + i * (bb.getMaxX() - bb.getMinX()) + xOffset,
                            bb.getMinY() + j * (bb.getMaxY() - bb.getMinY()) + yOffset,
                            bb.getMinZ() + k * (bb.getMaxZ() - bb.getMinZ()) + zOffset
                    );

                    if (!raycastHit(source, target, this.level, this.chunk)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean raycastHit(Vector3 start, Vector3 end, Level level, FullChunk fullChunk) {
        Vector3 current = new Vector3(start.x, start.y, start.z);
        Vector3 direction = end.subtract(start).normalize();

        double stepX = Utils.sign(direction.getX());
        double stepY = Utils.sign(direction.getY());
        double stepZ = Utils.sign(direction.getZ());

        double tMaxX = Utils.boundary(start.getX(), direction.getX());
        double tMaxY = Utils.boundary(start.getY(), direction.getY());
        double tMaxZ = Utils.boundary(start.getZ(), direction.getZ());

        double tDeltaX = direction.getX() == 0 ? 0 : stepX / direction.getX();
        double tDeltaY = direction.getY() == 0 ? 0 : stepY / direction.getY();
        double tDeltaZ = direction.getZ() == 0 ? 0 : stepZ / direction.getZ();

        double radius = start.distance(end);

        while (true) {
            Block block = level.getBlock(fullChunk, NukkitMath.floorDouble(current.x), NukkitMath.floorDouble(current.y), NukkitMath.floorDouble(current.z), false);

            if ((block.isSolid() || block instanceof BlockAnvil) && block.calculateIntercept(current, end) != null) {
                return true;
            }

            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                if (tMaxX > radius) {
                    break;
                }

                current.x += stepX;
                tMaxX += tDeltaX;
            } else if (tMaxY < tMaxZ) {
                if (tMaxY > radius) {
                    break;
                }

                current.y += stepY;
                tMaxY += tDeltaY;
            } else {
                if (tMaxZ > radius) {
                    break;
                }

                current.z += stepZ;
                tMaxZ += tDeltaZ;
            }
        }

        return false;
    }

    protected boolean canSetTemporalTarget() {
        return this.followTarget == null;
    }

    @Override
    public void setHealth(float health) {
        super.setHealth(health);
        if (!MobPlugin.getInstance().config.showBossBar) {
            return;
        }
        if (this instanceof Boss) {
            BossEventPacket pkBoss = new BossEventPacket();
            pkBoss.bossEid = this.id;
            pkBoss.type = BossEventPacket.TYPE_HEALTH_PERCENT;
            pkBoss.title = this.getName();
            pkBoss.healthPercent = this.getHealth() / this.getRealMaxHealth();
            Server.broadcastPacket(this.getViewers().values(), pkBoss);
        }
    }

    @Override
    public void spawnTo(Player player) {
        super.spawnTo(player);
        if (!MobPlugin.getInstance().config.showBossBar) {
            return;
        }
        if (this instanceof Boss) {
            BossEventPacket pkBoss = new BossEventPacket();
            pkBoss.bossEid = this.id;
            pkBoss.type = BossEventPacket.TYPE_SHOW;
            pkBoss.title = this.getName();
            pkBoss.healthPercent = this.getHealth() / this.getRealMaxHealth();
            player.dataPacket(pkBoss);
        }
    }

    @Override
    public void despawnFrom(Player player) {
        super.despawnFrom(player);
        if (!MobPlugin.getInstance().config.showBossBar) {
            return;
        }
        if (this instanceof Boss) {
            BossEventPacket pkBoss = new BossEventPacket();
            pkBoss.bossEid = this.id;
            pkBoss.type = BossEventPacket.TYPE_HIDE;
            player.dataPacket(pkBoss);
        }
    }
}
