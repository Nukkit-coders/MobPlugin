package nukkitcoders.mobplugin.entities.spawners;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import nukkitcoders.mobplugin.AutoSpawnTask;
import nukkitcoders.mobplugin.MobPlugin;
import nukkitcoders.mobplugin.entities.BaseEntity;
import nukkitcoders.mobplugin.entities.autospawn.AbstractEntitySpawner;
import nukkitcoders.mobplugin.entities.monster.walking.Husk;
import nukkitcoders.mobplugin.utils.Utils;

public class HuskSpawner extends AbstractEntitySpawner {

    public HuskSpawner(AutoSpawnTask spawnTask) {
        super(spawnTask);
    }

    @Override
    public void spawn(Player player, Position pos, Level level) {
        if (!MobPlugin.isMobSpawningAllowedByTime(level)) {
            return;
        }
        final int biomeId = level.getBiomeId((int) pos.x, (int) pos.z);
        if (biomeId == 2 || biomeId == 130) {
            if (level.getBlockLightAt((int) pos.x, (int) pos.y + 1, (int) pos.z) == 0 && level.canBlockSeeSky(new Vector3((int) pos.x, (int) pos.y + 1, (int) pos.z))) {
                BaseEntity entity = this.spawnTask.createEntity("Husk", pos.add(0.5, 1, 0.5));
                if (entity == null) {
                    return;
                }
                if (Utils.rand(1, 20) == 1) {
                    entity.setBaby(true);
                }
            }
        }
    }

    @Override
    public final int getEntityNetworkId() {
        return Husk.NETWORK_ID;
    }
}
