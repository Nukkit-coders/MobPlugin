package nukkitcoders.mobplugin.entities.spawners;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import nukkitcoders.mobplugin.AutoSpawnTask;
import nukkitcoders.mobplugin.MobPlugin;
import nukkitcoders.mobplugin.entities.animal.flying.Bat;
import nukkitcoders.mobplugin.entities.autospawn.AbstractEntitySpawner;

/**
 * @author <a href="mailto:kniffman@googlemail.com">Michael Gertz</a>
 */
public class BatSpawner extends AbstractEntitySpawner {

    public BatSpawner(AutoSpawnTask spawnTask) {
        super(spawnTask);
    }

    public void spawn(Player player, Position pos, Level level) {
        if (level.getBlockLightAt((int) pos.x, (int) pos.y + 1, (int) pos.z) <= 3) {
            if (!level.canBlockSeeSky(pos)) {
                if (MobPlugin.isAnimalSpawningAllowedByTime(level)) {
                    this.spawnTask.createEntity("Bat", pos.add(0.5, 1, 0.5));
                }
            }
        }
    }

    @Override
    public int getEntityNetworkId() {
        return Bat.NETWORK_ID;
    }
}
