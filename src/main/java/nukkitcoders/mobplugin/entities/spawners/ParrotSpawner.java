package nukkitcoders.mobplugin.entities.spawners;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import nukkitcoders.mobplugin.AutoSpawnTask;
import nukkitcoders.mobplugin.MobPlugin;
import nukkitcoders.mobplugin.entities.animal.flying.Parrot;
import nukkitcoders.mobplugin.entities.autospawn.AbstractEntitySpawner;
import nukkitcoders.mobplugin.utils.Utils;

public class ParrotSpawner extends AbstractEntitySpawner {

    public ParrotSpawner(AutoSpawnTask spawnTask) {
        super(spawnTask);
    }

    public void spawn(Player player, Position pos, Level level) {
        if (Utils.rand(1, 10) <= 4) {
            return;
        }
        if (pos.y < 70) {
            return;
        }
        if (!MobPlugin.isAnimalSpawningAllowedByTime(level)) {
            return;
        }
        final int biomeId = level.getBiomeId((int) pos.x, (int) pos.z);
        if (biomeId == 21 || biomeId == 149 || biomeId == 23 || biomeId == 151 || biomeId == 48 || biomeId == 49) {
            final int blockId = level.getBlockIdAt((int) pos.x, (int) pos.y, (int) pos.z);
            if (blockId == Block.GRASS || blockId == Block.LEAVES || blockId == Block.WOOD) {
                for (int i = 0; i < Utils.rand(1, 2); i++) {
                    this.spawnTask.createEntity("Parrot", pos.add(0.5, 1, 0.5));
                }
            }
        }
    }

    @Override
    public final int getEntityNetworkId() {
        return Parrot.NETWORK_ID;
    }
}
