package nukkitcoders.mobplugin.entities.spawners;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import nukkitcoders.mobplugin.AutoSpawnTask;
import nukkitcoders.mobplugin.MobPlugin;
import nukkitcoders.mobplugin.entities.BaseEntity;
import nukkitcoders.mobplugin.entities.animal.walking.Mooshroom;
import nukkitcoders.mobplugin.entities.autospawn.AbstractEntitySpawner;
import nukkitcoders.mobplugin.utils.Utils;

public class MooshroomSpawner extends AbstractEntitySpawner {

    public MooshroomSpawner(AutoSpawnTask spawnTask) {
        super(spawnTask);
    }

    public void spawn(Player player, Position pos, Level level) {
        if (Utils.rand(1, 3) != 1) {
            return;
        }
        int biomeId = level.getBiomeId((int) pos.x, (int) pos.z);
        if (biomeId == 14 || biomeId == 15) {
            if (MobPlugin.isAnimalSpawningAllowedByTime(level)) {
                if (level.getBlockIdAt((int) pos.x, (int) pos.y, (int) pos.z) == Block.MYCELIUM) {
                    for (int i = 0; i < Utils.rand(4, 8); i++) {
                        BaseEntity entity = this.spawnTask.createEntity("Mooshroom", pos.add(0.5, 1, 0.5));
                        if (entity == null) return;
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
        return Mooshroom.NETWORK_ID;
    }
}
