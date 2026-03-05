package nukkitcoders.mobplugin.entities.spawners;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import nukkitcoders.mobplugin.AutoSpawnTask;
import nukkitcoders.mobplugin.MobPlugin;
import nukkitcoders.mobplugin.entities.autospawn.AbstractEntitySpawner;
import nukkitcoders.mobplugin.entities.monster.jumping.Slime;
import nukkitcoders.mobplugin.utils.Utils;

public class SlimeSpawner extends AbstractEntitySpawner {

    public SlimeSpawner(AutoSpawnTask spawnTask) {
        super(spawnTask);
    }

    @Override
    public void spawn(Player player, Position pos, Level level) {
        if (Utils.rand()) {
            return;
        }
        if (!MobPlugin.isMobSpawningAllowedByTime(level)) {
            return;
        }
        final int biomeId = level.getBiomeId((int) pos.x, (int) pos.z);
        if (pos.y < 70 && (biomeId == 6 || biomeId == 134)) {
            if (level.getBlockLightAt((int) pos.x, (int) pos.y + 1, (int) pos.z) <= 7) {
                this.spawnTask.createEntity("Slime", pos.add(0.5, 1, 0.5));
            }
        }
    }

    @Override
    public final int getEntityNetworkId() {
        return Slime.NETWORK_ID;
    }
}
