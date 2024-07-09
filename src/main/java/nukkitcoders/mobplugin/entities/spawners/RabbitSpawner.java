package nukkitcoders.mobplugin.entities.spawners;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import nukkitcoders.mobplugin.AutoSpawnTask;
import nukkitcoders.mobplugin.MobPlugin;
import nukkitcoders.mobplugin.entities.animal.jumping.Rabbit;
import nukkitcoders.mobplugin.entities.autospawn.AbstractEntitySpawner;
import nukkitcoders.mobplugin.utils.Utils;

public class RabbitSpawner extends AbstractEntitySpawner {

    public RabbitSpawner(AutoSpawnTask spawnTask) {
        super(spawnTask);
    }

    @Override
    public void spawn(Player player, Position pos, Level level) {
        if (Utils.rand(1, 3) != 1) {
            return;
        }
        int blockId = level.getBlockIdAt((int) pos.x, (int) pos.y, (int) pos.z);
        if (blockId == Block.GRASS || blockId == Block.SNOW_LAYER || blockId == Block.SAND) {
            final int biomeId = level.getBiomeId((int) pos.x, (int) pos.z);
            if (biomeId == 2 || biomeId == 130 || biomeId == 30 || biomeId == 5 || biomeId == 12 || biomeId == 26 || biomeId == 11) {
                if (MobPlugin.isAnimalSpawningAllowedByTime(level)) {
                    for (int i = 0; i < Utils.rand(1, 3); i++) {
                        this.spawnTask.createEntity("Rabbit", pos.add(0.5, 1, 0.5));
                    }
                }
            }
        }
    }

    @Override
    public final int getEntityNetworkId() {
        return Rabbit.NETWORK_ID;
    }
}
