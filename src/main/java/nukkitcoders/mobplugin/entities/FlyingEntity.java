package nukkitcoders.mobplugin.entities;

import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector2;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import nukkitcoders.mobplugin.MobPlugin;
import nukkitcoders.mobplugin.utils.FastMathLite;
import nukkitcoders.mobplugin.utils.Utils;

public abstract class FlyingEntity extends BaseEntity {

    protected Vector3 followBlock; // Bee

    public FlyingEntity(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
        this.noFallDamage = true;
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

            for (Entity entity : this.getLevel().getEntities()) {
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

        int x, z;
        if (this.stayTime > 0) {
            if (Utils.rand(1, 100) > 5) {
                return;
            }

            x = Utils.rand(10, 30);
            z = Utils.rand(10, 30);
            this.target = this.add(Utils.rand() ? x : -x, -0.1, Utils.rand() ? z : -z);
        } else if (Utils.rand(1, 100) == 1) {
            x = Utils.rand(10, 30);
            z = Utils.rand(10, 30);
            this.stayTime = Utils.rand(200, 400);
            this.target = this.add(Utils.rand() ? x : -x, -0.1, Utils.rand() ? z : -z);
        } else if (this.moveTime <= 0 || this.target == null) {
            x = Utils.rand(20, 100);
            z = Utils.rand(20, 100);
            this.stayTime = 0;
            this.moveTime = Utils.rand(80, 200);
            this.target = this.add(Utils.rand() ? x : -x, -0.1, Utils.rand() ? z : -z);
        }
    }

    @Override
    public Vector3 updateMove(int tickDiff) {
        if (!this.isInTickingRange(MobPlugin.getInstance().config.entityActivationRange)) {
            return null;
        }

        if (!this.isImmobile()) {
            if (this.isKnockback()) {
                if (this.riding == null) {
                    this.move(this.motionX, this.motionY, this.motionZ);
                    this.updateMovement();
                }
                return this.followTarget != null ? this.followTarget : this.followBlock != null ? this.followBlock : this.target;
            }

            if (this.followTarget != null && !this.followTarget.closed && this.followTarget.isAlive()) {
                double x = this.followTarget.x - this.x;
                double y = this.followTarget.y - this.y;
                double z = this.followTarget.z - this.z;

                double diff = Math.abs(x) + Math.abs(z);
                if (this.riding != null || diff <= 0.001 || this.stayTime > 0 || this.distance(this.followTarget) <= (this.getWidth() / 2 + 0.3) * nearbyDistanceMultiplier()) {
                    this.motionX = 0;
                    this.motionZ = 0;
                    if (this.riding == null) {
                        this.motionY = this.getSpeed() * 0.01 * y;
                    }
                } else {
                    this.motionX = this.getSpeed() * 0.15 * (x / diff);
                    this.motionY = this.getSpeed() * 0.27 * (y / diff);
                    this.motionZ = this.getSpeed() * 0.15 * (z / diff);
                }
                if (this.noRotateTicks <= 0 && (this.stayTime <= 0 || Utils.rand()) && diff > 0.001) {
                    this.yaw = (FastMathLite.toDegrees(-FastMathLite.atan2(x / diff, z / diff)));
                }

                if (this.stayTime <= 0 && this.motionY == 0 && (Math.abs(motionX) > 0 || Math.abs(motionZ) > 0) &&
                        (Block.solid[this.level.getBlockIdAt(this.chunk, this.getFloorX(), this.getFloorY() - 1, this.getFloorZ())])) {
                    this.motionY = 0.05;
                }
                this.move(this.motionX, this.motionY, this.motionZ);
                this.updateMovement();
                return this.followTarget;
            }

            if (this.followBlock != null) {
                double x = this.followBlock.x - this.x;
                double y = this.followBlock.y - this.y;
                double z = this.followBlock.z - this.z;

                double diff = Math.abs(x) + Math.abs(z);
                if (this.riding != null || diff <= 0.001 || this.stayTime > 0 || this.distance(this.followBlock) <= (this.getWidth() / 2 + 0.3)) {
                    this.motionX = 0;
                    this.motionZ = 0;
                    if (this.riding != null) {
                        this.motionY = this.getSpeed() * 0.01 * y;
                    }
                } else {
                    this.motionX = this.getSpeed() * 0.15 * (x / diff);
                    this.motionY = this.getSpeed() * 0.27 * (y / diff);
                    this.motionZ = this.getSpeed() * 0.15 * (z / diff);
                }
                if (this.noRotateTicks <= 0 && (this.stayTime <= 0 || Utils.rand()) && diff > 0.001) {
                    this.yaw = (FastMathLite.toDegrees(-FastMathLite.atan2(x / diff, z / diff)));
                }

                if (this.stayTime <= 0 && this.motionY == 0 && (Math.abs(motionX) > 0 || Math.abs(motionZ) > 0) &&
                        (Block.solid[this.level.getBlockIdAt(this.chunk, this.getFloorX(), this.getFloorY() - 1, this.getFloorZ())])) {
                    this.motionY = 0.05;
                }
                this.move(this.motionX, this.motionY, this.motionZ);
                this.updateMovement();
                return this.followBlock;
            }

            Vector3 before = this.target;
            this.checkTarget();
            if (this.target instanceof Entity || before != this.target) {
                double x = this.target.x - this.x;
                double y = this.target.y - this.y;
                double z = this.target.z - this.z;

                double diff = Math.abs(x) + Math.abs(z);
                if (this.riding != null || diff <= 0.001 || this.stayTime > 0 || this.distance(this.target) <= (this.getWidth() / 2 + 0.3) * nearbyDistanceMultiplier()) {
                    this.motionX = 0;
                    this.motionZ = 0;
                    if (this.riding == null) {
                        this.motionY = this.getSpeed() * 0.01 * y;
                    }
                } else {
                    this.motionX = this.getSpeed() * 0.15 * (x / diff);
                    this.motionY = this.getSpeed() * 0.27 * (y / diff);
                    this.motionZ = this.getSpeed() * 0.15 * (z / diff);
                }
                if (this.noRotateTicks <= 0 && (this.stayTime <= 0 || Utils.rand()) && diff > 0.001) {
                    this.yaw = (FastMathLite.toDegrees(-FastMathLite.atan2(x / diff, z / diff)));
                }
            }

            if (this.target != null && this.stayTime <= 0 && this.motionY == 0 && (Math.abs(motionX) > 0 || Math.abs(motionZ) > 0) && distanceSquared(target) > 1) {
                this.motionY = 0.05;
            }

            double dx = this.motionX;
            double dy = this.motionY;
            double dz = this.motionZ;
            if (this.stayTime > 0) {
                this.stayTime -= tickDiff;
                this.move(0, dy, 0);
            } else {
                Vector2 be = new Vector2(this.x + dx, this.z + dz);
                this.move(dx, dy, dz);
                Vector2 af = new Vector2(this.x, this.z);

                if (be.x != af.x || be.y != af.y) {
                    this.moveTime -= 90;
                }
            }

            this.updateMovement();
            return this.target;
        }
        return null;
    }
}
