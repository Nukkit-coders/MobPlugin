package nukkitcoders.mobplugin.entities.spawners;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.level.biome.EnumBiome;
import nukkitcoders.mobplugin.AutoSpawnTask;
import nukkitcoders.mobplugin.entities.BaseEntity;
import nukkitcoders.mobplugin.entities.autospawn.AbstractEntitySpawner;
import nukkitcoders.mobplugin.entities.monster.walking.Hoglin;
import nukkitcoders.mobplugin.utils.Utils;

public class HoglinSpawner extends AbstractEntitySpawner {

    public HoglinSpawner(AutoSpawnTask spawnTask) {
        super(spawnTask);
    }

    @Override
    public void spawn(Player player, Position pos, Level level) {
        if (Utils.rand(1, 5) != 1) {
            return;
        }
        int biome = level.getBiomeId((int) pos.x, (int) pos.z);
        if (biome == EnumBiome.CRIMSON_FOREST.id) {
            int blockId = level.getBlockIdAt((int) pos.x, (int) pos.y, (int) pos.z);
            if (blockId != Block.BLOCK_NETHER_WART_BLOCK) {
                for (int i = 0; i < 4; i++) {
                    BaseEntity entity = this.spawnTask.createEntity("Hoglin", pos.add(0.5, 1, 0.5));
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

    @Override
    public final int getEntityNetworkId() {
        return Hoglin.NETWORK_ID;
    }
}
