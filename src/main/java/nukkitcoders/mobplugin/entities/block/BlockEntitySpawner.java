package nukkitcoders.mobplugin.entities.block;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntitySpawnable;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.entity.CreatureSpawnEvent;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.NukkitRandom;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ShortTag;
import cn.nukkit.utils.Config;
import nukkitcoders.mobplugin.MobPlugin;
import nukkitcoders.mobplugin.entities.BaseEntity;
import nukkitcoders.mobplugin.entities.monster.Monster;
import nukkitcoders.mobplugin.utils.Utils;

import java.util.ArrayList;

public class BlockEntitySpawner extends BlockEntitySpawnable {

    private int entityId;
    private int spawnRange;
    private int maxNearbyEntities;
    private int requiredPlayerRange;

    private int delay = 0;

    private int minSpawnDelay;
    private int maxSpawnDelay;

    private int minSpawnCount;
    private int maxSpawnCount;

    private NukkitRandom nukkitRandom = new NukkitRandom();

    public static final String TAG_ID = "id";
    public static final String TAG_X = "x";
    public static final String TAG_Y = "y";
    public static final String TAG_Z = "z";
    public static final String TAG_ENTITY_ID = "EntityId";
    public static final String TAG_SPAWN_RANGE = "SpawnRange";
    public static final String TAG_MIN_SPAWN_DELAY = "MinSpawnDelay";
    public static final String TAG_MAX_SPAWN_DELAY = "MaxSpawnDelay";
    public static final String TAG_MAX_NEARBY_ENTITIES = "MaxNearbyEntities";
    public static final String TAG_REQUIRED_PLAYER_RANGE = "RequiredPlayerRange";
    public static final String TAG_MINIMUM_SPAWN_COUNT = "MinimumSpawnerCount";
    public static final String TAG_MAXIMUM_SPAWN_COUNT = "MaximumSpawnerCount";

    public static final short SPAWN_RANGE = (short) MobPlugin.getInstance().config.spawnerRange;
    public static final short MINIMUM_SPAWN_COUNT = (short) MobPlugin.getInstance().config.minSpawnerSpawnCount;
    public static final short MAXIMUM_SPAWN_COUNT = (short) MobPlugin.getInstance().config.maxSpawnerSpawnCount;
    public static final short MIN_SPAWN_DELAY = 200;
    public static final short MAX_SPAWN_DELAY = 5000;
    public static final short MAX_NEARBY_ENTITIES = 20;
    public static final short REQUIRED_PLAYER_RANGE = 16;

    public BlockEntitySpawner(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
        this.entityId = this.namedTag.getInt(TAG_ENTITY_ID);
    }

    @Override
    protected void initBlockEntity() {
        if (!this.namedTag.contains(TAG_SPAWN_RANGE) || !(this.namedTag.get(TAG_SPAWN_RANGE) instanceof ShortTag)) {
            this.namedTag.putShort(TAG_SPAWN_RANGE, SPAWN_RANGE);
        }

        if (!this.namedTag.contains(TAG_MIN_SPAWN_DELAY) || !(this.namedTag.get(TAG_MIN_SPAWN_DELAY) instanceof ShortTag)) {
            this.namedTag.putShort(TAG_MIN_SPAWN_DELAY, MIN_SPAWN_DELAY);
        }

        if (!this.namedTag.contains(TAG_MAX_SPAWN_DELAY) || !(this.namedTag.get(TAG_MAX_SPAWN_DELAY) instanceof ShortTag)) {
            this.namedTag.putShort(TAG_MAX_SPAWN_DELAY, MAX_SPAWN_DELAY);
        }

        if (!this.namedTag.contains(TAG_MAX_NEARBY_ENTITIES) || !(this.namedTag.get(TAG_MAX_NEARBY_ENTITIES) instanceof ShortTag)) {
            this.namedTag.putShort(TAG_MAX_NEARBY_ENTITIES, MAX_NEARBY_ENTITIES);
        }

        if (!this.namedTag.contains(TAG_REQUIRED_PLAYER_RANGE) || !(this.namedTag.get(TAG_REQUIRED_PLAYER_RANGE) instanceof ShortTag)) {
            this.namedTag.putShort(TAG_REQUIRED_PLAYER_RANGE, REQUIRED_PLAYER_RANGE);
        }

        if (!this.namedTag.contains(TAG_MINIMUM_SPAWN_COUNT) || !(this.namedTag.get(TAG_MINIMUM_SPAWN_COUNT) instanceof ShortTag)) {
            this.namedTag.putShort(TAG_MINIMUM_SPAWN_COUNT, MINIMUM_SPAWN_COUNT);
        }

        if (!this.namedTag.contains(TAG_MAXIMUM_SPAWN_COUNT) || !(this.namedTag.get(TAG_MAXIMUM_SPAWN_COUNT) instanceof ShortTag)) {
            this.namedTag.putShort(TAG_MAXIMUM_SPAWN_COUNT, MAXIMUM_SPAWN_COUNT);
        }

        this.spawnRange = this.namedTag.getShort(TAG_SPAWN_RANGE);
        this.minSpawnDelay = this.namedTag.getShort(TAG_MIN_SPAWN_DELAY);
        this.maxSpawnDelay = this.namedTag.getShort(TAG_MAX_SPAWN_DELAY);
        this.maxNearbyEntities = this.namedTag.getShort(TAG_MAX_NEARBY_ENTITIES);
        this.requiredPlayerRange = this.namedTag.getShort(TAG_REQUIRED_PLAYER_RANGE);
        this.minSpawnCount = this.namedTag.getShort(TAG_MINIMUM_SPAWN_COUNT);
        this.maxSpawnCount = this.namedTag.getShort(TAG_MAXIMUM_SPAWN_COUNT);

        this.scheduleUpdate();
        super.initBlockEntity();
    }

    @Override
    public boolean onUpdate() {
        if (this.closed || !MobPlugin.getInstance().config.spawnersEnabled) {
            return false;
        }

        if (this.delay++ >= Utils.rand(this.minSpawnDelay, this.maxSpawnDelay)) {
            this.delay = 0;

            ArrayList<Entity> list = new ArrayList<>();
            boolean isValid = false;
            for (Entity entity : this.level.getEntities()) {
                if (entity instanceof Player || entity instanceof BaseEntity) {
                    if (entity.distance(this) <= this.requiredPlayerRange) {
                        if (entity instanceof Player) {
                            isValid = true;
                        }
                        list.add(entity);
                    }
                }
            }

            int amountToSpawn = minSpawnCount + nukkitRandom.nextBoundedInt(maxSpawnCount);
            for(int i = 0; i < amountToSpawn; i++) {
                if (isValid && list.size() <= this.maxNearbyEntities) {
                    Position pos = new Position
                            (
                                    this.x + Utils.rand(-this.spawnRange, this.spawnRange),
                                    this.y,
                                    this.z + Utils.rand(-this.spawnRange, this.spawnRange),
                                    this.level
                            );
                    Block block = level.getBlock(pos);
                    //Mobs shouldn't spawn in walls and they shouldn't retry to
                    if (
                            block.getId() != 0 && block.getId() != BlockID.SIGN_POST && block.getId() != BlockID.WALL_SIGN &&
                            block.getId() != BlockID.STILL_WATER && block.getId() != BlockID.WATER &&
                            block.getId() != BlockID.LAVA && block.getId() != BlockID.STILL_LAVA
                    ) {
                        continue;
                    }

                    CreatureSpawnEvent ev = new CreatureSpawnEvent(this.entityId, pos, new CompoundTag(), CreatureSpawnEvent.SpawnReason.SPAWNER);
                    level.getServer().getPluginManager().callEvent(ev);

                    if (ev.isCancelled()) {
                        continue;
                    }

                    Entity entity = Entity.createEntity(this.entityId, pos);
                    if (entity != null) {
                        if (entity instanceof Monster && this.level.getBlockLightAt((int) x, (int) y, (int) z) > 3) {
                            entity.close();
                            continue;
                        }
                        entity.spawnToAll();
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void saveNBT() {
        super.saveNBT();

        this.namedTag.putInt(TAG_ENTITY_ID, this.entityId);
        this.namedTag.putString(TAG_ID, "MobSpawner");
        this.namedTag.putShort(TAG_SPAWN_RANGE, this.spawnRange);
        this.namedTag.putShort(TAG_MIN_SPAWN_DELAY, this.minSpawnDelay);
        this.namedTag.putShort(TAG_MAX_SPAWN_DELAY, this.maxSpawnDelay);
        this.namedTag.putShort(TAG_MAX_NEARBY_ENTITIES, this.maxNearbyEntities);
        this.namedTag.putShort(TAG_REQUIRED_PLAYER_RANGE, this.requiredPlayerRange);
        this.namedTag.putShort(TAG_MINIMUM_SPAWN_COUNT, this.minSpawnCount);
        this.namedTag.putShort(TAG_MAXIMUM_SPAWN_COUNT, this.maxSpawnCount);
    }

    @Override
    public CompoundTag getSpawnCompound() {
        return new CompoundTag()
                .putString(TAG_ID, BlockEntity.MOB_SPAWNER)
                .putInt(TAG_ENTITY_ID, this.entityId)
                .putInt(TAG_X, (int) this.x)
                .putInt(TAG_Y, (int) this.y)
                .putInt(TAG_Z, (int) this.z);
    }

    @Override
    public boolean isBlockEntityValid() {
        return this.getBlock().getId() == BlockID.MONSTER_SPAWNER;
    }

    public int getSpawnEntityType() {
        return this.entityId;
    }

    public void setSpawnEntityType(int entityId) {
        this.entityId = entityId;
        this.spawnToAll();
    }

    public void setMinSpawnDelay(int minDelay) {
        if (minDelay > this.maxSpawnDelay) {
            return;
        }

        this.minSpawnDelay = minDelay;
    }

    public void setMaxSpawnDelay(int maxDelay) {
        if (this.minSpawnDelay > maxDelay) {
            return;
        }

        this.maxSpawnDelay = maxDelay;
    }

    public void setSpawnDelay(int minDelay, int maxDelay) {
        if (minDelay > maxDelay) {
            return;
        }

        this.minSpawnDelay = minDelay;
        this.maxSpawnDelay = maxDelay;
    }

    public void setRequiredPlayerRange(int range) {
        this.requiredPlayerRange = range;
    }

    public void setMaxNearbyEntities(int count) {
        this.maxNearbyEntities = count;
    }

}
