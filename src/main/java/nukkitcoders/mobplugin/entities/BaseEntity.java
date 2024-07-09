package nukkitcoders.mobplugin.entities;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityAgeable;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.EntityRideable;
import cn.nukkit.entity.data.EntityData;
import cn.nukkit.event.Event;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityInteractEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.HeartParticle;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.EntityEventPacket;
import cn.nukkit.network.protocol.MoveEntityAbsolutePacket;
import cn.nukkit.network.protocol.SetEntityMotionPacket;
import cn.nukkit.potion.Effect;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import nukkitcoders.mobplugin.MobPlugin;
import nukkitcoders.mobplugin.entities.animal.Animal;
import nukkitcoders.mobplugin.entities.animal.walking.Cow;
import nukkitcoders.mobplugin.entities.monster.Monster;
import nukkitcoders.mobplugin.utils.FastMathLite;
import nukkitcoders.mobplugin.utils.Utils;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

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
    protected float moveMultiplier = 1.0f;
    protected Vector3 target;
    protected Entity followTarget;
    protected int attackDelay;
    protected boolean noFallDamage;
    protected Player lastInteract;
    private int airTicks;
    private boolean baby;
    private boolean movement = true;
    private boolean friendly;
    private int knockBackTime;
    private short inLoveTicks;
    //private int inEndPortal;
    //private int inNetherPortal;
    private short inLoveCooldown;

    public BaseEntity(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);

        this.setHealth(this.getMaxHealth());
        this.setAirTicks(300);
    }

    public abstract Vector3 updateMove(int tickDiff);

    public abstract int getKillExperience();

    public boolean isFriendly() {
        return this.friendly;
    }

    public void setFriendly(boolean bool) {
        this.friendly = bool;
    }

    public boolean isMovement() {
        return this.movement;
    }

    public void setMovement(boolean value) {
        this.movement = value;
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
        return this.target;
    }

    public void setTarget(Vector3 vec) {
        this.target = vec;
    }

    public Entity getFollowTarget() {
        if (this.followTarget != null) {
            return this.followTarget;
        } else if (this.target instanceof Entity) {
            return (Entity) this.target;
        } else {
            return null;
        }
    }

    public void setFollowTarget(Entity target) {
        this.followTarget = target;
        this.moveTime = 0;
        this.stayTime = 0;
        this.target = null;
    }

    public Vector3 getTargetVector() {
        if (this.followTarget != null) {
            return this.followTarget;
        } else if (this.target instanceof Entity) {
            return this.target;
        } else {
            return null;
        }
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

        if (this.namedTag.contains("Movement")) {
            this.setMovement(this.namedTag.getBoolean("Movement"));
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

        this.namedTag.putBoolean("Baby", this.isBaby());
        this.namedTag.putBoolean("Movement", this.isMovement());
        this.namedTag.putShort("Age", this.age);
        this.namedTag.putShort("InLoveTicks", this.inLoveTicks);
        this.namedTag.putShort("InLoveCooldown", this.inLoveCooldown);
    }

    public boolean targetOption(EntityCreature creature, double distance) {
        if (this instanceof Monster) {
            if (creature instanceof Player) {
                Player player = (Player) creature;
                return !player.closed && player.spawned && player.isAlive() && (player.isSurvival() || player.isAdventure()) && distance <= 100;
            }
            return creature.isAlive() && !creature.closed && distance <= 100;
        } else if (this instanceof Animal && this.isInLove()) {
            return creature instanceof BaseEntity && ((BaseEntity) creature).isInLove() && creature.isAlive() && !creature.closed && creature.getNetworkId() == this.getNetworkId() && distance <= 100;
        }
        return false;
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        if (this.canDespawn()) {
            if (MobPlugin.getInstance().config.killOnDespawn) {
                this.kill();
                return true;
            } else {
                this.close();
                return false;
            }
        }

        boolean hasUpdate = super.entityBaseTick(tickDiff);

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

        return hasUpdate;
    }

    @Override
    protected void checkBlockCollision() {
        //boolean netherPortal = false;
        //boolean endPortal = false;

        for (Block block : this.getCollisionBlocks()) {
            /*if (block.getId() == Block.NETHER_PORTAL) {
                netherPortal = true;
                continue;
            } else if (block.getId() == Block.END_PORTAL) {
                endPortal = true;
                continue;
            }*/

            block.onEntityCollide(this);
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
            ((EntityDamageByEntityEvent) source).setKnockBack(0.25f);
        }

        super.attack(source);

        this.target = null;
        this.stayTime = 0;
        return true;
    }

    @Override
    public boolean move(double dx, double dy, double dz) {
        if (dy < -10 || dy > 10) {
            return false;
        }

        if (dx == 0 && dz == 0 && dy == 0) {
            return false;
        }

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
    public boolean canDespawn() {
        int despawnTicks = MobPlugin.getInstance().config.despawnTicks;
        return despawnTicks > 0 && this.age > despawnTicks && !this.hasCustomName() && !(this instanceof Boss);
    }

    /**
     * How near a player the mob should get before it starts attacking
     *
     * @return distance
     */
    public int nearbyDistanceMultiplier() {
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
    public void addMotion(double motionX, double motionY, double motionZ) {
        SetEntityMotionPacket pk = new SetEntityMotionPacket();
        pk.eid = this.id;
        pk.motionX = (float) motionX;
        pk.motionY = (float) motionY;
        pk.motionZ = (float) motionZ;
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

    public static void setProjectileMotion(Entity projectile, double pitch, double yawR, double pitchR, double speed) {
        double verticalMultiplier = Math.cos(pitchR);
        double x = verticalMultiplier * Math.sin(-yawR);
        double z = verticalMultiplier * Math.cos(yawR);
        double y = Math.sin(-(FastMathLite.toRadians(pitch)));
        double magnitude = Math.sqrt(x * x + y * y + z * z);
        if (magnitude > 0) {
            x += (x * (speed - magnitude)) / magnitude;
            y += (y * (speed - magnitude)) / magnitude;
            z += (z * (speed - magnitude)) / magnitude;
        }
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        x += rand.nextGaussian() * 0.007499999832361937 * 6;
        y += rand.nextGaussian() * 0.007499999832361937 * 6;
        z += rand.nextGaussian() * 0.007499999832361937 * 6;
        projectile.setMotion(new Vector3(x, y, z));
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
        return entity instanceof Player;
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

    @Override
    public boolean setDataProperty(EntityData data, boolean send) {
        if (!Objects.equals(data, this.dataProperties.get(data.getId()))) {
            this.dataProperties.put(data);
            if (send && (data.getId() != DATA_HEALTH || this instanceof EntityRideable || this instanceof Boss)) {
                this.sendData(this.hasSpawned.values().toArray(new Player[0]), this.dataProperties);
            }
            return true;
        }
        return false;
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
                Block down = this.level.getBlock(this.down());
                if (!this.noFallDamage) {
                    float damage = (float) Math.floor(fallDistance - 3 - (this.hasEffect(Effect.JUMP) ? this.getEffect(Effect.JUMP).getAmplifier() + 1 : 0));
                    if (down.getId() == BlockID.HAY_BALE) {
                        damage -= (damage * 0.8f);
                    }
                    if (damage > 0) {
                        this.attack(new EntityDamageEvent(this, EntityDamageEvent.DamageCause.FALL, damage));
                    }
                }
                if (down.getId() == BlockID.FARMLAND) {
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
                baby.spawnToAll();
                if (baby instanceof Cow) {
                    if (pl != null) {
                        pl.awardAchievement("breedCow");
                    }
                }
                if (!MobPlugin.getInstance().config.noXpOrbs) {
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
        } else {
            this.inLoveTicks = 0;
            //this.setDataFlag(DATA_FLAGS, DATA_FLAG_INLOVE, false);
        }
    }

    protected boolean isInTickingRange() {
        for (Player player : this.level.getPlayers().values()) {
            if (player.distanceSquared(this) < 6400) { // 80 blocks
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
}
