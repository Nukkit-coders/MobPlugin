package nukkitcoders.mobplugin.entities.spawners;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import nukkitcoders.mobplugin.AutoSpawnTask;
import nukkitcoders.mobplugin.MobPlugin;
import nukkitcoders.mobplugin.entities.BaseEntity;
import nukkitcoders.mobplugin.entities.animal.walking.PolarBear;
import nukkitcoders.mobplugin.entities.autospawn.AbstractEntitySpawner;
import nukkitcoders.mobplugin.utils.Utils;

public class PolarBearSpawner extends AbstractEntitySpawner {

    public PolarBearSpawner(AutoSpawnTask spawnTask) {
        super(spawnTask);
    }

    @Override
    public void spawn(Player player, Position pos, Level level) {
        if (Utils.rand(1, 3) != 1) {
            return;
        }
        if (level.getBiomeId((int) pos.x, (int) pos.z) == 12) {
            if (MobPlugin.isAnimalSpawningAllowedByTime(level)) {
                int blockId = level.getBlockIdAt((int) pos.x, (int) pos.y, (int) pos.z);
                if (blockId == Block.GRASS || blockId == Block.SNOW_LAYER) {
                    for (int i = 0; i < Utils.rand(1, 2); i++) {
                        BaseEntity entity = this.spawnTask.createEntity("PolarBear", pos.add(0.5, 1, 0.5));
                        if (entity == null) {
                            return;
                        }
                        if (Utils.rand(1, 20) == 1) {
                            entity.setBaby(true);
                        }
                    }
                }
            }
        }
    }

    @Override
    public final int getEntityNetworkId() {
        return PolarBear.NETWORK_ID;
    }
}
