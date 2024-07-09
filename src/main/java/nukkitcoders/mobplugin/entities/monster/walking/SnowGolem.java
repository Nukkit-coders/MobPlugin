package nukkitcoders.mobplugin.entities.monster.walking;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.projectile.EntitySnowball;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.ProjectileLaunchEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.level.Sound;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.LevelSoundEventPacket;
import nukkitcoders.mobplugin.entities.monster.WalkingMonster;
import nukkitcoders.mobplugin.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class SnowGolem extends WalkingMonster {

    public static final int NETWORK_ID = 21;

    public boolean sheared = false;

    public SnowGolem(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
        this.setFriendly(true);
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public float getWidth() {
        return 0.7f;
    }

    @Override
    public float getHeight() {
        return 1.9f;
    }

    @Override
    public void initEntity() {
        this.setMaxHealth(4);
        super.initEntity();
        this.noFallDamage = true;

        if (this.namedTag.getBoolean("Sheared")) {
            this.shear(true);
        }
    }

    @Override
    public boolean targetOption(EntityCreature creature, double distance) {
        return (!(creature instanceof Player) || creature.getId() == this.isAngryTo) && creature.isAlive() && distance <= 100;
    }

    @Override
    public void attackEntity(Entity player) {
        if (this.attackDelay > 23 && Utils.rand(1, 32) < 4 && this.distanceSquared(player) <= 55) {
            this.attackDelay = 0;

            double f = 1.2;
            double yaw = this.yaw + Utils.rand(-4.0, 4.0);
            double pitch = this.pitch + Utils.rand(-4.0, 4.0);
            Location location = new Location(this.x + (-Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * 0.5), this.y + this.getEyeHeight(),
                    this.z + (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * 0.5), yaw, pitch, this.level);
            Entity k = Entity.createEntity("Snowball", location, this);
            if (k == null) {
                return;
            }

            EntitySnowball snowball = (EntitySnowball) k;
            double a = -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * f * f;
            double b = -Math.sin(Math.toRadians(pitch)) * f * f;
            double c = Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * f * f;
            snowball.setMotion(new Vector3(a, b,
                    c));

            Vector3 motion = new Vector3(a, b,
                    c).multiply(f);
            snowball.setMotion(motion);

            ProjectileLaunchEvent launch = new ProjectileLaunchEvent(snowball);
            this.server.getPluginManager().callEvent(launch);
            if (launch.isCancelled()) {
                if (this.stayTime > 0 || this.distance(this.target) <= ((this.getWidth()) / 2 + 0.05) * nearbyDistanceMultiplier()) {
                    snowball.close();
                }
            } else {
                snowball.spawnToAll();
                this.level.addSound(this, Sound.MOB_SNOWGOLEM_SHOOT);
            }
        }
    }

    @Override
    public Item[] getDrops() {
        List<Item> drops = new ArrayList<>();

        for (int i = 0; i < Utils.rand(0, 15); i++) {
            drops.add(Item.get(Item.SNOWBALL, 0, 1));
        }

        return drops.toArray(new Item[0]);
    }

    @Override
    public int getKillExperience() {
        return 0;
    }

    @Override
    public String getName() {
        return this.hasCustomName() ? this.getNameTag() : "Snow Golem";
    }

    @Override
    public int nearbyDistanceMultiplier() {
        return 10;
    }

    @Override
    public boolean onInteract(Player player, Item item, Vector3 clickedPos) {
        if (item.getId() == Item.SHEARS && !this.sheared) {
            this.shear(true);
            this.level.addLevelSoundEvent(this, LevelSoundEventPacket.SOUND_SHEAR);
            player.getInventory().getItemInHand().setDamage(item.getDamage() + 1);
            return true;
        }

        return super.onInteract(player, item, clickedPos);
    }

    public void shear(boolean shear) {
        this.sheared = shear;
        this.setDataFlag(DATA_FLAGS, DATA_FLAG_SHEARED, shear);
    }

    public void saveNBT() {
        super.saveNBT();

        this.namedTag.putBoolean("Sheared", this.sheared);
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        if (this.age % 20 == 0 && (this.level.getDimension() == Level.DIMENSION_NETHER || (this.level.isRaining() && this.level.canBlockSeeSky(this)))) {
            this.attack(new EntityDamageEvent(this, EntityDamageEvent.DamageCause.FIRE_TICK, 1));
        }

        return super.entityBaseTick(tickDiff);
    }

    @Override
    public boolean attack(EntityDamageEvent ev) {
        if (super.attack(ev)) {
            if (ev instanceof EntityDamageByEntityEvent) {
                this.isAngryTo = ((EntityDamageByEntityEvent) ev).getDamager().getId();
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean canTarget(Entity entity) {
        return entity.getId() == this.isAngryTo;
    }
}
