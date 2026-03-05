package nukkitcoders.mobplugin.entities.spawners;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.level.biome.EnumBiome;
import nukkitcoders.mobplugin.AutoSpawnTask;
import nukkitcoders.mobplugin.entities.autospawn.AbstractEntitySpawner;
import nukkitcoders.mobplugin.entities.monster.flying.Ghast;
import nukkitcoders.mobplugin.utils.Utils;

public class GhastSpawner extends AbstractEntitySpawner {

    public GhastSpawner(AutoSpawnTask spawnTask) {
        super(spawnTask);
    }

    @Override
    public void spawn(Player player, Position pos, Level level) {
        if (Utils.rand(1, 100) < 95) {
            return;
        }
        int biome = level.getBiomeId((int) pos.x, (int) pos.z);
        if (biome != EnumBiome.CRIMSON_FOREST.id && biome != EnumBiome.WARPED_FOREST.id) {
            this.spawnTask.createEntity("Ghast", pos.add(0.5, 1, 0.5));
        }
    }

    @Override
    public int getEntityNetworkId() {
        return Ghast.NETWORK_ID;
    }
}
