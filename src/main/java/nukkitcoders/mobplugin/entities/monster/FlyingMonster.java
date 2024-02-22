package nukkitcoders.mobplugin.entities.monster;

import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import nukkitcoders.mobplugin.entities.FlyingEntity;
import nukkitcoders.mobplugin.utils.Utils;

public abstract class FlyingMonster extends FlyingEntity implements Monster {

    protected float[] minDamage;

    protected float[] maxDamage;

    protected boolean canAttack = true;

    public FlyingMonster(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public void setFollowTarget(Entity target) {
        this.setFollowTarget(target, true);
    }

    public void setFollowTarget(Entity target, boolean attack) {
        super.setFollowTarget(target);
        this.canAttack = attack;
    }

    public float getDamage() {
        return getDamage(null);
    }

    public float getDamage(Integer difficulty) {
        return (float) Utils.rand(this.getMinDamage(difficulty), this.getMaxDamage(difficulty));
    }

    public float getMinDamage() {
        return getMinDamage(null);
    }

    public float getMinDamage(Integer difficulty) {
        if (difficulty == null || difficulty > 3 || difficulty < 0) {
            difficulty = Server.getInstance().getDifficulty();
        }
        return this.minDamage[difficulty];
    }

    public float getMaxDamage() {
        return getMaxDamage(null);
    }

    public float getMaxDamage(Integer difficulty) {
        if (difficulty == null || difficulty > 3 || difficulty < 0) {
            difficulty = Server.getInstance().getDifficulty();
        }
        return this.maxDamage[difficulty];
    }

    public void setDamage(float damage) {
        this.setDamage(damage, Server.getInstance().getDifficulty());
    }

    public void setDamage(float damage, int difficulty) {
        if (difficulty >= 1 && difficulty <= 3) {
            this.minDamage[difficulty] = damage;
            this.maxDamage[difficulty] = damage;
        }
    }

    public void setDamage(float[] damage) {
        if (damage.length != 4) {
            throw new IllegalArgumentException("Invalid damage array length");
        }

        if (minDamage == null || minDamage.length < 4) {
            minDamage = new float[]{0, 0, 0, 0};
        }

        if (maxDamage == null || maxDamage.length < 4) {
            maxDamage = new float[]{0, 0, 0, 0};
        }

        for (int i = 0; i < 4; i++) {
            this.minDamage[i] = damage[i];
            this.maxDamage[i] = damage[i];
        }
    }

    public void setMinDamage(float[] damage) {
        if (damage.length != 4) {
            throw new IllegalArgumentException("Invalid damage array length");
        }

        for (int i = 0; i < 4; i++) {
            this.setMinDamage(damage[i], i);
        }
    }

    public void setMinDamage(float damage) {
        this.setMinDamage(damage, Server.getInstance().getDifficulty());
    }

    public void setMinDamage(float damage, int difficulty) {
        if (difficulty >= 1 && difficulty <= 3) {
            this.minDamage[difficulty] = Math.min(damage, this.getMaxDamage(difficulty));
        }
    }

    public void setMaxDamage(float[] damage) {
        if (damage.length != 4) {
            throw new IllegalArgumentException("Invalid damage array length");
        }

        for (int i = 0; i < 4; i++) {
            this.setMaxDamage(damage[i], i);
        }
    }

    public void setMaxDamage(float damage) {
        this.setMaxDamage(damage, Server.getInstance().getDifficulty());
    }

    public void setMaxDamage(float damage, int difficulty) {
        if (difficulty >= 1 && difficulty <= 3) {
            this.maxDamage[difficulty] = Math.max(damage, this.getMinDamage(difficulty));
        }
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.server.getDifficulty() < 1) {
            this.close();
            return false;
        }

        if (!this.isAlive()) {
            if (++this.deadTicks >= 23) {
                this.close();
                return false;
            }
            return true;
        }

        int tickDiff = currentTick - this.lastUpdate;
        this.lastUpdate = currentTick;
        this.entityBaseTick(tickDiff);

        Vector3 target = this.updateMove(tickDiff);
        if (target instanceof Entity) {
            Entity entity = (Entity) target;
            if (!entity.closed && (target != this.followTarget || this.canAttack)) {
                this.attackEntity(entity);
            }
        }
        return true;
    }
}
