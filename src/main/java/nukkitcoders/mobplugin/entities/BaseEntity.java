package nukkitcoders.mobplugin.entities;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityAgeable;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.MoveEntityAbsolutePacket;
import cn.nukkit.network.protocol.SetEntityMotionPacket;
import nukkitcoders.mobplugin.MobPlugin;
import nukkitcoders.mobplugin.entities.monster.Monster;
import nukkitcoders.mobplugin.entities.monster.flying.EnderDragon;
import nukkitcoders.mobplugin.utils.Utils;
import org.apache.commons.math3.util.FastMath;

import java.util.concurrent.ThreadLocalRandom;

public abstract class BaseEntity extends EntityCreature implements EntityAgeable {

    public int stayTime = 0;
    protected int moveTime = 0;
    private int airTicks = 0;
    protected float moveMultiplier = 1.0f;
    protected Vector3 target = null;
    protected Entity followTarget = null;
    private boolean baby = false;
    private boolean movement = true;
    private boolean friendly = false;
    protected int attackDelay = 0;
    public Item[] armor;

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

    public boolean isMovement() {
        return this.movement;
    }

    public boolean isKnockback() {
        return this.attackTime > 0;
    }

    public void setFriendly(boolean bool) {
        this.friendly = bool;
    }

    public void setMovement(boolean value) {
        this.movement = value;
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

    public Vector3 getTargetVector() {
        if (this.followTarget != null) {
            return this.followTarget;
        } else if (this.target instanceof Entity) {
            return this.target;
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

    @Override
    public boolean isBaby() {
        return this.baby;
    }

    public void setBaby(boolean baby) {
        this.baby = baby;
        this.setDataFlag(DATA_FLAGS, DATA_FLAG_BABY, baby);
        if (baby) {
            this.setScale(0.5f);
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
            this.setBaby(this.namedTag.getBoolean("Baby"));
        }
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    public void saveNBT() {
        super.saveNBT();

        this.namedTag.putBoolean("Baby", this.isBaby());
        this.namedTag.putBoolean("Movement", this.isMovement());
        this.namedTag.putShort("Age", this.age);
    }

    public boolean targetOption(EntityCreature creature, double distance) {
        if (this instanceof Monster) {
            if (creature instanceof Player) {
                Player player = (Player) creature;
                return !player.closed && player.spawned && player.isAlive() && (player.isSurvival() || player.isAdventure()) && distance <= 100;
            }
            return creature.isAlive() && !creature.closed && distance <= 100;
        }
        return false;
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        super.entityBaseTick(tickDiff);

        if (this.canDespawn()) {
            if (MobPlugin.getInstance().config.killOnDespawn) {
                this.kill();
            } else {
                this.close();
            }
        }

        if (this instanceof Monster && this.attackDelay < 200) {
            this.attackDelay++;
        }

        return true;
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

    @Override
    public boolean onInteract(Player player, Item item) {
        if (item.getId() == Item.NAME_TAG) {
            if (item.hasCustomName() && !(this instanceof EnderDragon)) {
                this.setNameTag(item.getCustomName());
                this.setNameTagVisible(true);
                player.getInventory().decreaseCount(player.getInventory().getHeldItemIndex());
                return true;
            }
        }
        return false;
    }

    protected float getMountedYOffset() {
        return getHeight() * 0.75F;
    }

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
                        helmet = Item.get(Item.LEATHER_CAP, 0, 1);
                        this.addHealth(1);
                    }
                }
                break;
            case 2:
                if (Utils.rand(1, 100) < 50) {
                    if (Utils.rand(0, 1) == 0) {
                        helmet = Item.get(Item.GOLD_HELMET, 0, 1);
                        this.addHealth(1);
                    }
                }
                break;
            case 3:
                if (Utils.rand(1, 100) < 14) {
                    if (Utils.rand(0, 1) == 0) {
                        helmet = Item.get(Item.CHAIN_HELMET, 0, 1);
                        this.addHealth(1);
                    }
                }
                break;
            case 4:
                if (Utils.rand(1, 100) < 3) {
                    if (Utils.rand(0, 1) == 0) {
                        helmet = Item.get(Item.IRON_HELMET, 0, 1);
                        this.addHealth(1);
                    }
                }
                break;
            case 5:
                if (Utils.rand(1, 100) == 100) {
                    if (Utils.rand(0, 1) == 0) {
                        helmet = Item.get(Item.DIAMOND_HELMET, 0, 1);
                        this.addHealth(2);
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
                            chestplate = Item.get(Item.LEATHER_TUNIC, 0, 1);
                            this.addHealth(1);
                        }
                    }
                    break;
                case 2:
                    if (Utils.rand(1, 100) < 50) {
                        if (Utils.rand(0, 1) == 0) {
                            chestplate = Item.get(Item.GOLD_CHESTPLATE, 0, 1);
                            this.addHealth(1);
                        }
                    }
                    break;
                case 3:
                    if (Utils.rand(1, 100) < 14) {
                        if (Utils.rand(0, 1) == 0) {
                            chestplate = Item.get(Item.CHAIN_CHESTPLATE, 0, 1);
                            this.addHealth(1);
                        }
                    }
                    break;
                case 4:
                    if (Utils.rand(1, 100) < 3) {
                        if (Utils.rand(0, 1) == 0) {
                            chestplate = Item.get(Item.IRON_CHESTPLATE, 0, 1);
                            this.addHealth(2);
                        }
                    }
                    break;
                case 5:
                    if (Utils.rand(1, 100) == 100) {
                        if (Utils.rand(0, 1) == 0) {
                            chestplate = Item.get(Item.DIAMOND_CHESTPLATE, 0, 1);
                            this.addHealth(3);
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
                            leggings = Item.get(Item.LEATHER_PANTS, 0, 1);
                            this.addHealth(1);
                        }
                    }
                    break;
                case 2:
                    if (Utils.rand(1, 100) < 50) {
                        if (Utils.rand(0, 1) == 0) {
                            leggings = Item.get(Item.GOLD_LEGGINGS, 0, 1);
                            this.addHealth(1);
                        }
                    }
                    break;
                case 3:
                    if (Utils.rand(1, 100) < 14) {
                        if (Utils.rand(0, 1) == 0) {
                            leggings = Item.get(Item.CHAIN_LEGGINGS, 0, 1);
                            this.addHealth(1);
                        }
                    }
                    break;
                case 4:
                    if (Utils.rand(1, 100) < 3) {
                        if (Utils.rand(0, 1) == 0) {
                            leggings = Item.get(Item.IRON_LEGGINGS, 0, 1);
                            this.addHealth(1);
                        }
                    }
                    break;
                case 5:
                    if (Utils.rand(1, 100) == 100) {
                        if (Utils.rand(0, 1) == 0) {
                            leggings = Item.get(Item.DIAMOND_LEGGINGS, 0, 1);
                            this.addHealth(2);
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
                            boots = Item.get(Item.LEATHER_BOOTS, 0, 1);
                            this.addHealth(1);
                        }
                    }
                    break;
                case 2:
                    if (Utils.rand(1, 100) < 50) {
                        if (Utils.rand(0, 1) == 0) {
                            boots = Item.get(Item.GOLD_BOOTS, 0, 1);
                            this.addHealth(1);
                        }
                    }
                    break;
                case 3:
                    if (Utils.rand(1, 100) < 14) {
                        if (Utils.rand(0, 1) == 0) {
                            boots = Item.get(Item.CHAIN_BOOTS, 0, 1);
                            this.addHealth(1);
                        }
                    }
                    break;
                case 4:
                    if (Utils.rand(1, 100) < 3) {
                        if (Utils.rand(0, 1) == 0) {
                            boots = Item.get(Item.IRON_BOOTS, 0, 1);
                            this.addHealth(1);
                        }
                    }
                    break;
                case 5:
                    if (Utils.rand(1, 100) == 100) {
                        if (Utils.rand(0, 1) == 0) {
                            boots = Item.get(Item.DIAMOND_BOOTS, 0, 1);
                            this.addHealth(2);
                        }
                    }
                    break;
            }
        }

        slots[3] = boots;

        return slots;
    }

    private void addHealth(int health) {
        this.setMaxHealth(this.getMaxHealth() + health);
        this.setHealth(this.getHealth() + health);
    }

    public boolean canDespawn() {
        int despawnTicks = MobPlugin.getInstance().config.despawnTicks;
        return despawnTicks > 0 && this.age > despawnTicks && !this.hasCustomName() && !(this instanceof Boss);
    }

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
            p.batchDataPacket(pk);
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
            p.batchDataPacket(pk);
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
        double y = Math.sin(-(FastMath.toRadians(pitch)));
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
}
