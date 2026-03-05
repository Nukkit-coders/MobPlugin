package nukkitcoders.mobplugin.entities.autospawn;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockLava;
import cn.nukkit.entity.mob.EntityPhantom;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.math.NukkitMath;
import nukkitcoders.mobplugin.AutoSpawnTask;
import nukkitcoders.mobplugin.MobPlugin;
import nukkitcoders.mobplugin.entities.animal.walking.Strider;
import nukkitcoders.mobplugin.utils.FastMathLite;
import nukkitcoders.mobplugin.utils.Utils;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author <a href="mailto:kniffman@googlemail.com">Michael Gertz</a>
 */
public abstract class AbstractEntitySpawner implements IEntitySpawner {

    protected AutoSpawnTask spawnTask;

    private final boolean isMonsterSpawner;

    public AbstractEntitySpawner(AutoSpawnTask spawnTask) {
        this.spawnTask = spawnTask;

        this.isMonsterSpawner = Utils.monstersList.contains(this.getEntityNetworkId());
    }

    @Override
    public void spawn() {
        for (Player player : Server.getInstance().getOnlinePlayers().values()) {
            if (isSpawningAllowed(player)) {
                spawnTo(player);
            }
        }
    }

    private void spawnTo(Player player) {
        Level level = player.getLevel();

        if (this.spawnTask.entitySpawnAllowed(level, this.getEntityNetworkId(), player)) {
            Position pos = new Position(player.getFloorX(), player.getFloorY(), player.getFloorZ(), level);

            if (this.getEntityNetworkId() == EntityPhantom.NETWORK_ID) {
                // Other checks are done in the spawner class
                pos.x = pos.x + Utils.rand(-2, 2);
                pos.y = pos.y + Utils.rand(20, 34);
                pos.z = pos.z + Utils.rand(-2, 2);
                spawn(player, pos, level);
            } else {
                ThreadLocalRandom random = ThreadLocalRandom.current();

                double r = 24.0 + 20.0 * random.nextDouble(); // Between min 24 and max 44 blocks from player
                double theta = 6.283185307179586 * random.nextDouble(); // 2pi

                pos.x += NukkitMath.ceilDouble(r * FastMathLite.cos(theta));
                pos.z += NukkitMath.ceilDouble(r * FastMathLite.sin(theta));

                if (!level.isChunkLoaded((int) pos.x >> 4, (int) pos.z >> 4) || !level.isChunkGenerated((int) pos.x >> 4, (int) pos.z >> 4)) {
                    return;
                }

                if (MobPlugin.getInstance().config.spawnNoSpawningArea > 0 && level.getSpawnLocation().distance(pos) < MobPlugin.getInstance().config.spawnNoSpawningArea) {
                    return;
                }

                if (this.isMonsterSpawner) {
                    int biome = level.getBiomeId((int) pos.x, (int) pos.z);
                    if (biome == 14 || biome == 15) {
                        return; // Hostile mobs don't spawn on mushroom island
                    }
                }

                pos.y = AutoSpawnTask.getSafeYCoord(level, pos);

                if (this.isWaterMob()) {
                    pos.y--;
                }

                if (pos.y <= level.getMinBlockY() || pos.y > level.getMaxBlockY()) {
                    return;
                }

                if (isTooNearOfPlayer(pos)) {
                    return;
                }

                Block block = level.getBlock(pos, false);
                if (this.getEntityNetworkId() == Strider.NETWORK_ID) {
                    if (!(block instanceof BlockLava)) {
                        return;
                    }
                } else {
                    if (block.getId() == Block.BROWN_MUSHROOM_BLOCK || block.getId() == Block.RED_MUSHROOM_BLOCK) { // Mushrooms aren't transparent but shouldn't have mobs spawned on them
                        return;
                    }

                    if (block.isTransparent() && block.getId() != Block.SNOW_LAYER) { // Snow layer is an exception
                        if ((block.getId() != Block.WATER && block.getId() != Block.STILL_WATER) || !this.isWaterMob()) { // Water mobs can spawn in water
                            return;
                        }
                    }
                }

                spawn(player, pos, level);
            }
        }
    }

    private boolean isSpawningAllowed(Player player) {
        if (player.isSpectator()) {
            return false;
        }
        if (!MobPlugin.isSpawningAllowedByLevel(player.getLevel())) {
            return false;
        }
        return !this.isMonsterSpawner || Server.getInstance().getDifficulty() != 0;
    }

    private static boolean isTooNearOfPlayer(Position pos) {
        for (Player p : pos.getLevel().getPlayers().values()) {
            if (p.distanceSquared(pos) < 576) { // 24 blocks
                return true;
            }
        }
        return false;
    }
}
