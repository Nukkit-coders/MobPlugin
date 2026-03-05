package nukkitcoders.mobplugin.entities.spawners;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import nukkitcoders.mobplugin.AutoSpawnTask;
import nukkitcoders.mobplugin.entities.animal.walking.Strider;
import nukkitcoders.mobplugin.entities.autospawn.AbstractEntitySpawner;
import nukkitcoders.mobplugin.utils.Utils;

public class StriderSpawner extends AbstractEntitySpawner {

    public StriderSpawner(AutoSpawnTask spawnTask) {
        super(spawnTask);
    }

    @Override
    public void spawn(Player player, Position pos, Level level) {
        if (Utils.rand(1, 5) != 1) {
            return;
        }
        this.spawnTask.createEntity("Strider", pos.add(0.5, 1, 0.5));
    }

    @Override
    public final int getEntityNetworkId() {
        return Strider.NETWORK_ID;
    }
}
