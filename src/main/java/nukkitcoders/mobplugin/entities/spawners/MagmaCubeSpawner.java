package nukkitcoders.mobplugin.entities.spawners;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.level.biome.EnumBiome;
import nukkitcoders.mobplugin.AutoSpawnTask;
import nukkitcoders.mobplugin.entities.autospawn.AbstractEntitySpawner;
import nukkitcoders.mobplugin.entities.monster.jumping.MagmaCube;

public class MagmaCubeSpawner extends AbstractEntitySpawner {

    public MagmaCubeSpawner(AutoSpawnTask spawnTask) {
        super(spawnTask);
    }

    @Override
    public void spawn(Player player, Position pos, Level level) {
        int biome = level.getBiomeId((int) pos.x, (int) pos.z);
        if (biome != EnumBiome.SOULSAND_VALLEY.id && biome != EnumBiome.CRIMSON_FOREST.id && biome != EnumBiome.WARPED_FOREST.id) {
            this.spawnTask.createEntity("MagmaCube", pos.add(0.5, 1, 0.5));
        }
    }

    @Override
    public final int getEntityNetworkId() {
        return MagmaCube.NETWORK_ID;
    }
}
