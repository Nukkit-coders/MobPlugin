package nukkitcoders.mobplugin.entities.spawners;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import nukkitcoders.mobplugin.AutoSpawnTask;
import nukkitcoders.mobplugin.entities.autospawn.AbstractEntitySpawner;
import nukkitcoders.mobplugin.entities.monster.walking.WitherSkeleton;

public class WitherSkeletonSpawner extends AbstractEntitySpawner {

    public WitherSkeletonSpawner(AutoSpawnTask spawnTask) {
        super(spawnTask);
    }

    @Override
    public void spawn(Player player, Position pos, Level level) {
        if (level.getBlockIdAt((int) pos.x, (int) pos.y, (int) pos.z) == Block.NETHER_BRICKS) {
            this.spawnTask.createEntity("WitherSkeleton", pos.add(0.5, 1, 0.5));
        }
    }

    @Override
    public final int getEntityNetworkId() {
        return WitherSkeleton.NETWORK_ID;
    }
}
