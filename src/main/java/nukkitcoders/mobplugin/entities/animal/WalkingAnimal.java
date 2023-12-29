package nukkitcoders.mobplugin.entities.animal;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import nukkitcoders.mobplugin.entities.WalkingEntity;
import nukkitcoders.mobplugin.utils.Utils;

public abstract class WalkingAnimal extends WalkingEntity implements Animal {

    private int panicTicks = 0;

    public WalkingAnimal(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
        this.route = null;
    }

    @Override
    protected void initEntity() {
        super.initEntity();
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
                this.doPanic(false);
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

    public int getPanicTicks() {
        return this.panicTicks;
    }

    public void doPanic(boolean panic) {
        if (panic) {
            int time = Utils.rand(60, 100);
            this.panicTicks = time;
            this.stayTime = 0;
            this.moveTime = time;
            this.moveMultiplier = 1.8f;
        } else {
            this.panicTicks = 0;
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

    @Override
    public boolean canTarget(Entity entity) {
        return (this.isInLove() || entity instanceof Player) && this.getPanicTicks() <= 0;
    }
}
