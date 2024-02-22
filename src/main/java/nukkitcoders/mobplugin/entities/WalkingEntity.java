package nukkitcoders.mobplugin.entities;

import cn.nukkit.block.*;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.BubbleParticle;
import cn.nukkit.math.NukkitMath;
import cn.nukkit.math.Vector2;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import nukkitcoders.mobplugin.RouteFinderThreadPool;
import nukkitcoders.mobplugin.entities.animal.walking.Llama;
import nukkitcoders.mobplugin.entities.animal.walking.Pig;
import nukkitcoders.mobplugin.entities.animal.walking.SkeletonHorse;
import nukkitcoders.mobplugin.entities.monster.walking.Drowned;
import nukkitcoders.mobplugin.entities.monster.walking.IronGolem;
import nukkitcoders.mobplugin.route.RouteFinder;
import nukkitcoders.mobplugin.runnable.RouteFinderSearchTask;
import nukkitcoders.mobplugin.utils.FastMathLite;
import nukkitcoders.mobplugin.utils.Utils;

public abstract class WalkingEntity extends BaseEntity {

    private static final double FLOW_MULTIPLIER = 0.1;
    protected RouteFinder route;

    public WalkingEntity(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    protected void checkTarget() {
        if (this.isKnockback()) {
            return;
        }

        if (this.followTarget != null && !this.followTarget.closed && this.followTarget.isAlive() && targetOption((EntityCreature) this.followTarget, this.distanceSquared(this.followTarget)) && this.target != null) {
            return;
        }

        this.followTarget = null;

        if (!this.passengers.isEmpty() && !(this instanceof Llama) && !(this instanceof Pig)) {
            return;
        }

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
            this.followTarget = creature;
            if (this.route == null && this.passengers.isEmpty()) {
                this.target = creature;
            }
        }

        if (this.followTarget != null) {
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

    protected boolean checkJump(double dx, double dz) {
        if (this.motionY == this.getGravity() * 2) {
            return this.canSwimIn(level.getBlockIdAt(NukkitMath.floorDouble(this.x), (int) this.y, NukkitMath.floorDouble(this.z)));
        } else {
            if (this.canSwimIn(level.getBlockIdAt(NukkitMath.floorDouble(this.x), (int) (this.y + 0.8), NukkitMath.floorDouble(this.z)))) {
                if (!(this instanceof Drowned || this instanceof IronGolem || this instanceof SkeletonHorse) || this.target == null) {
                    this.motionY = this.getGravity() * 2;
                }
                return true;
            }
        }

        if (!this.onGround || this.stayTime > 0) {
            return false;
        }

        Block that = this.getLevel().getBlock(new Vector3(NukkitMath.floorDouble(this.x + dx), (int) this.y, NukkitMath.floorDouble(this.z + dz)));
        Block block = that.getSide(this.getHorizontalFacing());
        Block down;
        if (this.followTarget == null && this.passengers.isEmpty() && !(down = block.down()).isSolid() && !block.isSolid() && !down.down().isSolid()) {
            this.stayTime = 10;
        } else if (!block.canPassThrough() && !(block instanceof BlockFlowable || block.getId() == BlockID.SOUL_SAND) && block.up().canPassThrough() && that.up(2).canPassThrough()) {
            if (block instanceof BlockFence || block instanceof BlockFenceGate) {
                this.motionY = this.getGravity();
            } else if (this.motionY <= this.getGravity() * 4) {
                this.motionY = this.getGravity() * 4;
            } else if (block instanceof BlockStairs) {
                this.motionY = this.getGravity() * 4;
            } else if (this.motionY <= (this.getGravity() * 8)) {
                this.motionY = this.getGravity() * 8;
            } else {
                this.motionY += this.getGravity() * 0.25;
            }
            return true;
        }
        return false;
    }

    public Vector3 updateMove(int tickDiff) {
        if (!this.isInTickingRange()) {
            return null;
        }
        if (!isImmobile()) {
            if (!this.isMovement()) {
                return null;
            }

            if (this.age % 10 == 0 && this.route != null && !this.route.isSearching()) {
                RouteFinderThreadPool.executeRouteFinderThread(new RouteFinderSearchTask(this.route));
                if (this.route.hasNext()) {
                    this.target = this.route.next();
                }
            }

            if (this.isKnockback()) {
                this.move(this.motionX, this.motionY, this.motionZ);
                if (this instanceof Drowned && Utils.entityInsideWaterFast(this)) {
                    this.motionY -= this.getGravity() * 0.3;
                } else {
                    this.motionY -= this.getGravity();
                }
                this.updateMovement();
                return null;
            }

            Block levelBlock = getLevelBlock();
            boolean inWater = levelBlock.getId() == 8 || levelBlock.getId() == 9;
            int downId = level.getBlockIdAt(getFloorX(), getFloorY() - 1, getFloorZ());
            if (inWater && (downId == 0 || downId == 8 || downId == 9 || downId == BlockID.LAVA || downId == BlockID.STILL_LAVA || downId == BlockID.SIGN_POST || downId == BlockID.WALL_SIGN)) {
                onGround = false;
            }
            if (downId == 0 || downId == BlockID.SIGN_POST || downId == BlockID.WALL_SIGN) {
                onGround = false;
            }
            if (this.followTarget != null && !this.followTarget.closed && this.followTarget.isAlive() && this.target != null) {
                double x = this.target.x - this.x;
                double z = this.target.z - this.z;

                double diff = Math.abs(x) + Math.abs(z);
                if (diff == 0 || !inWater && (this.stayTime > 0 || this.distance(this.followTarget) <= (this.getWidth()) / 2 + 0.05)) {
                    this.motionX = 0;
                    this.motionZ = 0;
                } else {
                    if (levelBlock.getId() == 8) {
                        BlockWater blockWater = (BlockWater) levelBlock;
                        Vector3 flowVector = blockWater.getFlowVector();
                        motionX = flowVector.getX() * FLOW_MULTIPLIER;
                        motionZ = flowVector.getZ() * FLOW_MULTIPLIER;
                    } else if (levelBlock.getId() == 9) {
                        this.motionX = this.getSpeed() * moveMultiplier * 0.05 * (x / diff);
                        this.motionZ = this.getSpeed() * moveMultiplier * 0.05 * (z / diff);
                        if (!(this instanceof Drowned || this instanceof IronGolem || this instanceof SkeletonHorse)) {
                            this.level.addParticle(new BubbleParticle(this.add(Utils.rand(-2.0, 2.0), Utils.rand(-0.5, 0), Utils.rand(-2.0, 2.0))));
                        }
                    } else {
                        this.motionX = this.getSpeed() * moveMultiplier * 0.1 * (x / diff);
                        this.motionZ = this.getSpeed() * moveMultiplier * 0.1 * (z / diff);
                    }
                }
                if ((this.passengers.isEmpty() || this instanceof Llama || this instanceof Pig) && (this.stayTime <= 0 || Utils.rand()) && diff != 0) {
                    this.yaw = Math.toDegrees(-FastMathLite.atan2(x / diff, z / diff));
                }
            }

            this.checkTarget();
            if (this.target != null) {
                double x = this.target.x - this.x;
                double z = this.target.z - this.z;

                double diff = Math.abs(x) + Math.abs(z);
                boolean distance = false;
                if (diff == 0 || !inWater && (this.stayTime > 0 || (distance = this.distance(this.target) <= ((this.getWidth()) / 2 + 0.05) * nearbyDistanceMultiplier()))) {
                    this.motionX = 0;
                    this.motionZ = 0;
                } else {
                    if (levelBlock.getId() == 8) {
                        BlockWater blockWater = (BlockWater) levelBlock;
                        Vector3 flowVector = blockWater.getFlowVector();
                        motionX = flowVector.getX() * FLOW_MULTIPLIER;
                        motionZ = flowVector.getZ() * FLOW_MULTIPLIER;
                    } else if (levelBlock.getId() == 9) {
                        this.motionX = this.getSpeed() * moveMultiplier * 0.05 * (x / diff);
                        this.motionZ = this.getSpeed() * moveMultiplier * 0.05 * (z / diff);
                        if (!(this instanceof Drowned || this instanceof IronGolem || this instanceof SkeletonHorse)) {
                            this.level.addParticle(new BubbleParticle(this.add(Utils.rand(-2.0, 2.0), Utils.rand(-0.5, 0), Utils.rand(-2.0, 2.0))));
                        } else if (this.followTarget != null) {
                            double y = this.followTarget.y - this.y;
                            this.motionY = this.getSpeed() * moveMultiplier * 0.05 * (y / (diff + Math.abs(y)));
                        }
                    } else {
                        this.motionX = this.getSpeed() * moveMultiplier * 0.15 * (x / diff);
                        this.motionZ = this.getSpeed() * moveMultiplier * 0.15 * (z / diff);
                    }
                }
                if (!distance && (this.passengers.isEmpty() || this instanceof Llama || this instanceof Pig) && (this.stayTime <= 0 || Utils.rand()) && diff != 0) {
                    this.yaw = Math.toDegrees(-FastMathLite.atan2(x / diff, z / diff));
                }
            }

            double dx = this.motionX;
            double dz = this.motionZ;
            boolean isJump = this.checkJump(dx, dz);
            if (this.stayTime > 0 && !inWater) {
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
                if (this.onGround && !inWater) {
                    this.motionY = 0;
                } else if (this.motionY > -this.getGravity() * 4) {
                    if (!(this.level.getBlock(new Vector3(NukkitMath.floorDouble(this.x), (int) (this.y + 0.8), NukkitMath.floorDouble(this.z))) instanceof BlockLiquid)) {
                        this.motionY -= this.getGravity();
                    }
                } else {
                    if ((this instanceof Drowned || this instanceof IronGolem || this instanceof SkeletonHorse) && inWater && this.motionY < 0) {
                        this.motionY = this.getGravity() * -0.3;
                        this.stayTime = 40;
                    } else {
                        this.motionY -= this.getGravity();
                    }
                }
            }

            this.updateMovement();
            if (this.route != null) {
                if (this.route.hasCurrentNode() && this.route.hasArrivedNode(this)) {
                    if (this.route.hasNext()) {
                        this.target = this.route.next();
                    }
                }
            }
            return this.followTarget != null ? this.followTarget : this.target;
        }
        return null;
    }

    public RouteFinder getRoute() {
        return this.route;
    }

    public void setRoute(RouteFinder route) {
        this.route = route;
    }
}