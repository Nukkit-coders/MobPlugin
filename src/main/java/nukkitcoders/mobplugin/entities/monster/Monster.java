package nukkitcoders.mobplugin.entities.monster;

import cn.nukkit.entity.Entity;

public interface Monster {

    void attackEntity(Entity player);

    float getDamage();

    float getDamage(Integer difficulty);

    float getMinDamage();

    float getMinDamage(Integer difficulty);

    float getMaxDamage();

    float getMaxDamage(Integer difficulty);

    @Deprecated
    void setDamage(float damage);

    void setDamage(float[] damage);

    void setDamage(float damage, int difficulty);

    @Deprecated
    void setMinDamage(float damage);

    void setMinDamage(float[] damage);

    void setMinDamage(float damage, int difficulty);

    @Deprecated
    void setMaxDamage(float damage);

    void setMaxDamage(float[] damage);

    void setMaxDamage(float damage, int difficulty);
}
