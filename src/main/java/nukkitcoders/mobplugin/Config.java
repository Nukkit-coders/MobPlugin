package nukkitcoders.mobplugin;

public class Config {

    public cn.nukkit.utils.Config pluginConfig;

    public int spawnDelay;
    public int despawnTicks;
    public int spawnerRange;
    public int endEndermanSpawnRate;
    public int maxSpawnerSpawnCount;
    public int minSpawnerSpawnCount;
    public boolean noXpOrbs;
    public boolean noSpawnEggWasting;
    public boolean killOnDespawn;
    public boolean spawnersEnabled;
    public boolean checkTamedEntityAttack;
    public boolean creeperExplodeBlocks;

    Config(MobPlugin plugin) {
        plugin.saveDefaultConfig();
        pluginConfig = plugin.getConfig();
    }

    boolean init(MobPlugin plugin) {
        int ver = 16;

        if (pluginConfig.getInt("config-version") != ver) {
            if (pluginConfig.getInt("config-version") == 15) {
                pluginConfig.set("other.minimum-spawner-count", 1);
                pluginConfig.set("other.maximum-spawner-count", 4);
                pluginConfig.set("entities.entity-creation-disabled", "exampledworld1, exampleworld2");
                pluginConfig.set("other.creeper-explode-blocks", true);
            } else if (pluginConfig.getInt("config-version") == 14) {
                pluginConfig.set("entities.entity-creation-disabled", "exampledworld1, exampleworld2");
                pluginConfig.set("other.creeper-explode-blocks", true);
                pluginConfig.set("other.check-tamed-entity-attack", true);
                pluginConfig.set("other.minimum-spawner-count", 1);
                pluginConfig.set("other.maximum-spawner-count", 4);
            } else if (pluginConfig.getInt("config-version") == 13) {
                pluginConfig.set("entities.entity-creation-disabled", "exampledworld1, exampleworld2");
                pluginConfig.set("other.creeper-explode-blocks", true);
                pluginConfig.set("autospawn.piglin", 0);
                pluginConfig.set("other.minimum-spawner-count", 1);
                pluginConfig.set("other.maximum-spawner-count", 4);
            } else if (pluginConfig.getInt("config-version") == 12) {
                pluginConfig.set("entities.entity-creation-disabled", "exampledworld1, exampleworld2");
                pluginConfig.set("autospawn.fox", 0);
                pluginConfig.set("autospawn.panda", 0);
                pluginConfig.set("autospawn.drowned", 0);
                pluginConfig.set("autospawn.piglin", 0);
                pluginConfig.set("other.creeper-explode-blocks", true);
                pluginConfig.set("other.check-tamed-entity-attack", true);
                pluginConfig.set("other.minimum-spawner-count", 1);
                pluginConfig.set("other.maximum-spawner-count", 4);
            } else if (pluginConfig.getInt("config-version") == 11) {
                pluginConfig.set("other.creeper-explode-blocks", true);
                pluginConfig.set("entities.entity-creation-disabled", "exampledworld1, exampleworld2");
                pluginConfig.set("other.spawners-enabled", true);
                pluginConfig.set("other.end-enderman-spawning", 10);
                pluginConfig.set("autospawn.fox", 0);
                pluginConfig.set("autospawn.panda", 0);
                pluginConfig.set("autospawn.drowned", 0);
                pluginConfig.set("autospawn.piglin", 0);
                pluginConfig.set("other.check-tamed-entity-attack", true);
                pluginConfig.set("other.minimum-spawner-count", 1);
                pluginConfig.set("other.maximum-spawner-count", 4);
            } else if (pluginConfig.getInt("config-version") == 10) {
                pluginConfig.set("other.creeper-explode-blocks", true);
                pluginConfig.set("entities.entity-creation-disabled", "exampledworld1, exampleworld2");
                pluginConfig.set("other.kill-mobs-on-despawn", false);
                pluginConfig.set("other.spawners-enabled", true);
                pluginConfig.set("other.end-enderman-spawning", 10);
                pluginConfig.set("autospawn.fox", 0);
                pluginConfig.set("autospawn.panda", 0);
                pluginConfig.set("autospawn.drowned", 0);
                pluginConfig.set("autospawn.piglin", 0);
                pluginConfig.set("other.check-tamed-entity-attack", true);
                pluginConfig.set("other.minimum-spawner-count", 1);
                pluginConfig.set("other.maximum-spawner-count", 4);
            } else if (pluginConfig.getInt("config-version") == 9) {
                pluginConfig.set("entities.entity-creation-disabled", "exampledworld1, exampleworld2");
                pluginConfig.set("other.creeper-explode-blocks", true);
                pluginConfig.set("other.spawn-no-spawning-area", -1);
                pluginConfig.set("other.kill-mobs-on-despawn", false);
                pluginConfig.set("other.spawners-enabled", true);
                pluginConfig.set("other.end-enderman-spawning", 10);
                pluginConfig.set("autospawn.fox", 0);
                pluginConfig.set("autospawn.panda", 0);
                pluginConfig.set("autospawn.drowned", 0);
                pluginConfig.set("autospawn.piglin", 0);
                pluginConfig.set("other.check-tamed-entity-attack", true);
                pluginConfig.set("other.minimum-spawner-count", 1);
                pluginConfig.set("other.maximum-spawner-count", 4);
            } else {
                plugin.getLogger().warning("MobPlugin's config file is outdated. Please delete the old config.");
                plugin.getLogger().error("Config error. The plugin will be disabled.");
                plugin.getServer().getPluginManager().disablePlugin(plugin);
                return false;
            }

            pluginConfig.set("config-version", ver);
            pluginConfig.save();
            plugin.getLogger().notice("Config file updated to version " + ver);
        }

        spawnDelay = pluginConfig.getInt("entities.autospawn-ticks") >> 1;
        noXpOrbs = pluginConfig.getBoolean("other.use-no-xp-orbs");
        noSpawnEggWasting = pluginConfig.getBoolean("other.do-not-waste-spawn-eggs");
        despawnTicks = pluginConfig.getInt("entities.despawn-ticks");
        spawnerRange = pluginConfig.getInt("other.spawner-spawn-range");
        killOnDespawn = pluginConfig.getBoolean("other.kill-mobs-on-despawn");
        endEndermanSpawnRate = pluginConfig.getInt("other.end-enderman-spawning");
        spawnersEnabled = pluginConfig.getBoolean("other.spawners-enabled");
        checkTamedEntityAttack = pluginConfig.getBoolean("other.check-tamed-entity-attack");
        maxSpawnerSpawnCount = pluginConfig.getInt("other.maximum-spawner-count");
        minSpawnerSpawnCount = pluginConfig.getInt("other.minimum-spawner-count");
        creeperExplodeBlocks = pluginConfig.getBoolean("other.creeper-explode-blocks");
        return true;
    }
}
