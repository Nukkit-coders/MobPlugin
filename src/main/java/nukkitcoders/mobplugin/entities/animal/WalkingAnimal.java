package nukkitcoders.mobplugin.entities.animal;

import cn.nukkit.Player;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.HeartParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import nukkitcoders.mobplugin.entities.WalkingEntity;
import nukkitcoders.mobplugin.utils.Utils;

public abstract class WalkingAnimal extends WalkingEntity implements Animal {

    protected int inLoveTicks = 0;

    private int panicTicks = 0;

    public WalkingAnimal(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
        this.route = null;
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        boolean hasUpdate = super.entityBaseTick(tickDiff);

        if (this.isInLove()) {
            this.inLoveTicks -= tickDiff;
            if (this.age % 20 == 0) {
                for (int i = 0; i < 3; i++) {
                    this.level.addParticle(new HeartParticle(this.add(Utils.rand(-1.0,1.0),this.getMountedYOffset()+ Utils.rand(-1.0,1.0),Utils.rand(-1.0,1.0))));
                }
            }
        }

        return hasUpdate;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }
        if (!this.isAlive()) {
            if (++this.deadTicks >= 23) {
                this.close();
                return false;
            }
            return true;
        }

        if (this.panicTicks > 0) {
            this.panicTicks--;
            if (panicTicks == 0) {
                doPanic(false);
            }
        }

        int tickDiff = currentTick - this.lastUpdate;
        this.lastUpdate = currentTick;
        this.entityBaseTick(tickDiff);

        Vector3 target = this.updateMove(tickDiff);
        if (target instanceof Player) {
            if (this.distanceSquared(target) <= 2) {
                this.x = this.lastX;
                this.y = this.lastY;
                this.z = this.lastZ;
            }
        }
        return true;
    }

    public void setInLove() {
        this.inLoveTicks = 600;
        this.setDataFlag(DATA_FLAGS, DATA_FLAG_INLOVE);
    }

    public boolean isInLove() {
        return inLoveTicks > 0;
    }

    public boolean isBreedingItem(Item item) {
        return item != null && item.getId() == Item.WHEAT;
    }

    public void doPanic(boolean panic) {
        if (panic) {
            int time = Utils.rand(60, 100);
            this.panicTicks = time;
            this.stayTime = 0;
            this.moveTime = time;
            this.moveMultiplier = 1.8f;
        } else {
            this.moveMultiplier = 1.0f;
        }
    }

    @Override
    public boolean attack(EntityDamageEvent ev) {
        boolean result = super.attack(ev);

        if (result && !ev.isCancelled()) {
            this.doPanic(true);
        }

        return result;
    }
}
