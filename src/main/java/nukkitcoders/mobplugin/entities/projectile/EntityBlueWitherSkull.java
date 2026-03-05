package nukkitcoders.mobplugin.entities.projectile;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityExplosive;
import cn.nukkit.event.entity.EntityExplosionPrimeEvent;
import cn.nukkit.level.GameRule;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.SmokeParticle;
import cn.nukkit.nbt.tag.CompoundTag;
import nukkitcoders.mobplugin.utils.Utils;
import nukkitcoders.mobplugin.utils.WitherSkullExplosion;

public class EntityBlueWitherSkull extends EntityWitherSkull implements EntityExplosive {

    public static final int NETWORK_ID = 91;

    private boolean canExplode;

    public EntityBlueWitherSkull(FullChunk chunk, CompoundTag nbt) {
        this(chunk, nbt, null);
    }

    public EntityBlueWitherSkull(FullChunk chunk, CompoundTag nbt, Entity shootingEntity) {
        super(chunk, nbt, shootingEntity);
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    public void setExplode(boolean bool) {
        this.canExplode = bool;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        if (this.age > 1200 || this.isCollided || this.hadCollision) {
            if (this.canExplode) {
                this.explode();
            } else {
                this.close();
            }
        } else if (this.age % 4 == 0) {
            this.level.addParticle(new SmokeParticle(this.add(this.getWidth() / 2 + Utils.rand(-100.0, 100.0) / 500, this.getHeight() / 2 + Utils.rand(-100.0, 100.0) / 500, this.getWidth() / 2 + Utils.rand(-100.0, 100.0) / 500)));
        }

        super.onUpdate(currentTick);
        return !this.closed;
    }

    @Override
    public void explode() {
        if (this.closed) {
            return;
        }
        this.close();

        EntityExplosionPrimeEvent ev = new EntityExplosionPrimeEvent(this, 2); // bigger explosion works better for block breaking
        this.server.getPluginManager().callEvent(ev);

        if (!ev.isCancelled()) {
            WitherSkullExplosion explosion = new WitherSkullExplosion(this, (float) ev.getForce(), this.shootingEntity);
            if (ev.isBlockBreaking() && this.level.getGameRules().getBoolean(GameRule.MOB_GRIEFING)) {
                explosion.explodeA();
            }

            explosion.explodeB();
        }
    }
}
