package nukkitcoders.mobplugin.entities.monster.flying;

import cn.nukkit.Player;
import cn.nukkit.entity.Attribute;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.item.EntityEndCrystal;
import cn.nukkit.event.entity.ProjectileLaunchEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Location;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.AddEntityPacket;
import cn.nukkit.network.protocol.BossEventPacket;
import cn.nukkit.network.protocol.DataPacket;
import nukkitcoders.mobplugin.MobPlugin;
import nukkitcoders.mobplugin.entities.Boss;
import nukkitcoders.mobplugin.entities.monster.FlyingMonster;
import nukkitcoders.mobplugin.entities.projectile.EntityEnderCharge;
import nukkitcoders.mobplugin.utils.Utils;

public class EnderDragon extends FlyingMonster implements Boss {

    public static final int NETWORK_ID = 53;

    public EnderDragon(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public float getWidth() {
        return 16f;
    }

    @Override
    public float getHeight() {
        return 8f;
    }

    @Override
    public void initEntity() {
        this.setMaxHealth(200);
        super.initEntity();
        this.setHealth(200);

        this.fireProof = true;
        this.setDataFlag(DATA_FLAGS, DATA_FLAG_FIRE_IMMUNE, true);
    }

    @Override
    public int getKillExperience() {
        if (!MobPlugin.getInstance().config.noXpOrbs) {
            for (int i = 0; i < 167; ) {
                this.level.dropExpOrb(this, 3);
                i++;
            }
        }
        return 0;
    }

    @Override
    public boolean targetOption(EntityCreature creature, double distance) {
        if (creature instanceof Player) {
            Player player = (Player) creature;
            return player.spawned && player.isAlive() && !player.closed && (player.isSurvival() || player.isAdventure()) && distance <= 800 && distance > 50;
        }
        return creature.isAlive() && !creature.closed && distance <= 800 && distance > 50;
    }

    @Override
    public void attackEntity(Entity player) {
        if (this.attackDelay > 60 && Utils.rand(1, 5) < 3 && this.distanceSquared(player) <= 90000) {
            this.attackDelay = 0;
            double f = 1.1;
            double yaw = this.yaw + Utils.rand(-4.0, 4.0);
            double pitch = this.pitch + Utils.rand(-4.0, 4.0);
            Entity k = Entity.createEntity("EnderCharge", new Location(this.x + this.getLocation().getDirectionVector().multiply(5.0).x, this.y, this.z + this.getDirectionVector().multiply(5.0).z, this.level), this);
            if (!(k instanceof EntityEnderCharge)) {
                return;
            }
            EntityEnderCharge charge = (EntityEnderCharge) k;
            charge.setMotion(new Vector3(-Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * f * f, -Math.sin(Math.toRadians(pitch)) * f * f,
                    Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * f * f));
            ProjectileLaunchEvent launch = new ProjectileLaunchEvent(charge);
            this.server.getPluginManager().callEvent(launch);
            if (launch.isCancelled()) {
                charge.close();
            } else {
                charge.spawnToAll();
            }
        }
    }

    @Override
    public String getName() {
        return this.hasCustomName() ? this.getNameTag() : "Ender Dragon";
    }

    @Override
    protected DataPacket createAddEntityPacket() {
        AddEntityPacket addEntity = new AddEntityPacket();
        addEntity.type = this.getNetworkId();
        addEntity.entityUniqueId = this.getId();
        addEntity.entityRuntimeId = this.getId();
        addEntity.yaw = (float) this.yaw;
        addEntity.headYaw = (float) this.yaw;
        addEntity.pitch = (float) this.pitch;
        addEntity.x = (float) this.x;
        addEntity.y = (float) this.y;
        addEntity.z = (float) this.z;
        addEntity.speedX = (float) this.motionX;
        addEntity.speedY = (float) this.motionY;
        addEntity.speedZ = (float) this.motionZ;
        addEntity.metadata = this.dataProperties;
        addEntity.attributes = new Attribute[]{Attribute.getAttribute(Attribute.MAX_HEALTH).setMaxValue(200).setValue(200)};
        return addEntity;
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        if (tickDiff % 2 == 0) {
            for (Entity e : this.getLevel().getEntities()) {
                if (e instanceof EntityEndCrystal) {
                    if (e.distanceSquared(this) <= 32) {
                        float health = this.getHealth();
                        if (!(health > this.getMaxHealth()) && health != 0) {
                            this.setHealth(health + 0.2f);
                        }
                    }
                }
            }
        }

        return super.entityBaseTick(tickDiff);
    }

    @Override
    public void spawnTo(Player player) {
        super.spawnTo(player);
        if (!MobPlugin.getInstance().config.showBossBar) {
            return;
        }
        BossEventPacket pkBoss = new BossEventPacket();
        pkBoss.bossEid = this.id;
        pkBoss.type = BossEventPacket.TYPE_SHOW;
        pkBoss.title = this.getName();
        pkBoss.healthPercent = this.health / 100;
        player.dataPacket(pkBoss);
    }

    @Override
    protected boolean isInTickingRange() {
        return true;
    }

    @Override
    protected boolean applyNameTag(Player player, Item item) {
        return false;
    }
}
