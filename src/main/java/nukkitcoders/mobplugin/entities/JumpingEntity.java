package nukkitcoders.mobplugin.entities;

import cn.nukkit.block.Block;
import cn.nukkit.block.BlockLiquid;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.BubbleParticle;
import cn.nukkit.math.NukkitMath;
import cn.nukkit.math.Vector2;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import nukkitcoders.mobplugin.MobPlugin;
import nukkitcoders.mobplugin.utils.FastMathLite;
import nukkitcoders.mobplugin.utils.Utils;

public abstract class JumpingEntity extends BaseEntity {

    public JumpingEntity(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    protected void checkTarget() {
        if (this.isKnockback()) {
            return;
        }

        if (this.followTarget != null && !this.followTarget.closed && this.followTarget.isAlive()) {
            return;
        }

        Vector3 target = this.target;
        if (!(target instanceof EntityCreature) || !this.targetOption((EntityCreature) target, this.distanceSquared(target))) {
            double near = Integer.MAX_VALUE;

            for (Entity entity : this.getViewers().values()) {
                if (entity == this || !(entity instanceof EntityCreature) || entity.closed || !this.canTarget(entity)) {
                    continue;
                }

                EntityCreature creature = (EntityCreature) entity;
                if (creature instanceof BaseEntity && ((BaseEntity) creature).isFriendly() == this.isFriendly()) {
                    continue;
                }

                double distance = this.distanceSquared(creature);
                if (distance > near || !this.targetOption(creature, distance)) {
                    continue;
                }
                near = distance;

                this.stayTime = 0;
                this.moveTime = 0;
                this.target = creature;
            }
        }

        if (this.target instanceof EntityCreature && !((EntityCreature) this.target).closed && ((EntityCreature) this.target).isAlive() && this.targetOption((EntityCreature) this.target, this.distanceSquared(this.target))) {
            return;
        }

        if (this.stayTime > 0) {
            if (Utils.rand(1, 100) != 1) {
                return;
            }
            this.target = this.add(Utils.rand(-30, 30), Utils.rand(-20.0, 20.0) / 10, Utils.rand(-30, 30));
        } else if (Utils.rand(1, 100) == 1) {
            this.stayTime = Utils.rand(200, 400);
            this.target = this.add(Utils.rand(-30, 30), Utils.rand(-20.0, 20.0) / 10, Utils.rand(-30, 30));
        } else if (this.moveTime <= 0 || this.target == null) {
            this.stayTime = 0;
            this.moveTime = Utils.rand(80, 200);
            double tx = this.x;
            double tz = this.z;
            int attempts = 0;
            boolean inWater = true;
            while (attempts++ < 10 && inWater) {
                tx = this.x + Utils.rand(-30, 30);
                tz = this.z + Utils.rand(-30, 30);
                int txFloor = NukkitMath.floorDouble(tx);
                int tzFloor = NukkitMath.floorDouble(tz);
                FullChunk chunk1 = level.getChunkIfLoaded(txFloor >> 4, tzFloor >> 4);
                if (chunk1 != null) {
                    int yy = chunk1.getHighestBlockAt(txFloor & 15, tzFloor & 15, true);
                    if (yy >= level.getMinBlockY() && yy <= level.getMaxBlockY()) {
                        inWater = Block.isWater(chunk.getBlockId(txFloor & 15, yy, tzFloor & 15, Block.LAYER_NORMAL));
                    } else {
                        inWater = false;
                    }
                } else {
                    inWater = false;
                }
            }
            this.target = new Vector3(tx, this.y + Utils.rand(-20.0, 20.0) / 10, tz);
        }
    }

    protected boolean checkJump() {
        if (this.motionY == this.getGravity() * 2 && this.canSwimIn(level.getBlockIdAt(this.chunk, this.getFloorX(), this.getFloorY(), this.getFloorZ()))) {
            return true;
        } else {
            if (this.canSwimIn(level.getBlockIdAt(this.chunk, NukkitMath.floorDouble(this.x), (int) (this.y + 0.8), NukkitMath.floorDouble(this.z)))) {
                this.motionY = this.getGravity() * 2;
                return true;
            }
        }

        if (!this.onGround) {
            return false;
        }

        if (this.motionX > 0 || this.motionZ > 0) {
            this.motionY = this.getJumpStrength();
        }

        return false;
    }

    protected double getJumpStrength() {
        return 0.38;
    }

    public Vector3 updateMove(int tickDiff) {
        if (!this.isInTickingRange(MobPlugin.getInstance().config.entityActivationRange)) {
            return null;
        }
        if (!this.isImmobile()) {
            if (this.isKnockback()) {
                if (this.riding == null) {
                    this.move(this.motionX, this.motionY, this.motionZ);
                    this.motionY -= this.getGravity();
                    this.updateMovement();
                }
                return this.followTarget != null ? this.followTarget : this.target;
            }

            if (this.followTarget != null && !this.followTarget.closed && this.followTarget.isAlive()) {
                double x = this.followTarget.x - this.x;
                double z = this.followTarget.z - this.z;

                double diff = Math.abs(x) + Math.abs(z);
                if (this.riding != null || diff <= 0.001 || this.stayTime > 0 || this.distance(this.followTarget) <= (this.getWidth() / 2 + 0.3) * nearbyDistanceMultiplier()) {
                    if (!this.isInsideOfWater()) {
                        this.motionX = 0;
                        this.motionZ = 0;
                    }
                } else {
                    if (Utils.entityInsideWaterFast(this)) {
                        this.motionX = this.getSpeed() * 0.05 * (x / diff);
                        this.motionZ = this.getSpeed() * 0.05 * (z / diff);
                        this.level.addParticle(new BubbleParticle(this.add(Utils.rand(-2.0, 2.0), Utils.rand(-0.5, 0), Utils.rand(-2.0, 2.0))));
                    } else {
                        this.motionX = this.getSpeed() * 0.1 * (x / diff);
                        this.motionZ = this.getSpeed() * 0.1 * (z / diff);
                    }
                }
                if (this.noRotateTicks <= 0 && (this.stayTime <= 0 || Utils.rand()) && diff > 0.001) {
                    this.yaw = Math.toDegrees(-FastMathLite.atan2(x / diff, z / diff));
                }
                return this.followTarget;
            }

            Vector3 before = this.target;
            this.checkTarget();
            if (this.target instanceof Entity || before != this.target) {
                double x = this.target.x - this.x;
                double z = this.target.z - this.z;

                double diff = Math.abs(x) + Math.abs(z);
                if (this.riding != null || diff <= 0.001 || this.stayTime > 0 || this.distance(this.target) <= (this.getWidth() / 2 + 0.3) * nearbyDistanceMultiplier()) {
                    if (!this.isInsideOfWater()) {
                        this.motionX = 0;
                        this.motionZ = 0;
                    }
                } else {
                    if (Utils.entityInsideWaterFast(this)) {
                        this.motionX = this.getSpeed() * 0.05 * (x / diff);
                        this.motionZ = this.getSpeed() * 0.05 * (z / diff);
                        this.level.addParticle(new BubbleParticle(this.add(Utils.rand(-2.0, 2.0), Utils.rand(-0.5, 0), Utils.rand(-2.0, 2.0))));
                    } else {
                        this.motionX = this.getSpeed() * 0.15 * (x / diff);
                        this.motionZ = this.getSpeed() * 0.15 * (z / diff);
                    }
                }
                if (this.noRotateTicks <= 0 && (this.stayTime <= 0 || Utils.rand()) && diff > 0.001) {
                    this.yaw = Math.toDegrees(-FastMathLite.atan2(x / diff, z / diff));
                }
            }

            double dx = this.motionX;
            double dz = this.motionZ;
            boolean isJump = this.checkJump();
            if (this.stayTime > 0) {
                this.stayTime -= tickDiff;
                this.move(0, this.motionY, 0);
            } else {
                Vector2 be = new Vector2(this.x + dx, this.z + dz);
                this.move(dx, this.motionY, dz);
                Vector2 af = new Vector2(this.x, this.z);

                if ((be.x != af.x || be.y != af.y) && !isJump) {
                    this.moveTime -= 90;
                }
            }

            if (!isJump) {
                if (this.onGround) {
                    this.motionY = 0;
                } else if (this.motionY > -this.getGravity() * 4) {
                    if (!(this.level.getBlock(this.chunk, NukkitMath.floorDouble(this.x), (int) (this.y + 0.8), NukkitMath.floorDouble(this.z), true) instanceof BlockLiquid)) {
                        this.motionY -= this.getGravity();
                    }
                } else {
                    this.motionY -= this.getGravity();
                }
            }
            this.updateMovement();
            return this.target;
        }
        return null;
    }
}
