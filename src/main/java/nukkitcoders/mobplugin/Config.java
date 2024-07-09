package nukkitcoders.mobplugin;

import java.util.*;

public class Config {

    final cn.nukkit.utils.Config pluginConfig;

    public int spawnDelay;
    public int despawnTicks;
    public int spawnerRange;
    public int spawnerMinSpawnCount;
    public int spawnerMaxSpawnCount;
    public int spawnerMinDelay;
    public int spawnerMaxDelay;
    public int spawnerMaxNearby;
    public int spawnerRequiredPlayerRange;
    public int spawnNoSpawningArea;
    public int endEndermanSpawnRate;
    public boolean noXpOrbs;
    public boolean noSpawnEggWasting;
    public boolean killOnDespawn;
    public boolean spawnersEnabled;
    public boolean checkTamedEntityAttack;
    public boolean creeperExplodeBlocks;
    public boolean allowBreeding;
    public boolean showBossBar;
    public Set<String> mobSpawningDisabledWorlds;
    public Set<String> mobCreationDisabledWorlds;

    Config(MobPlugin plugin) {
        plugin.saveDefaultConfig();
        pluginConfig = plugin.getConfig();
    }

    boolean init(MobPlugin plugin) {
        int ver = 18;
        int current = pluginConfig.getInt("config-version");

        if (current != ver) {
            if (current < 9) {
                plugin.getLogger().warning("MobPlugin's config file is outdated. Please delete the old config.");
                plugin.getLogger().error("Config error. The plugin will be disabled.");
                plugin.getServer().getPluginManager().disablePlugin(plugin);
                return false;
            }

            if (current < 18) {
                pluginConfig.set("autospawn.llama", 0);
                pluginConfig.set("autospawn.strider", 0);
            }

            if (current < 17) {
                pluginConfig.set("other.allow-breeding", true);
                pluginConfig.set("other.show-boss-bar", true);
            }

            if (current < 16) {
                pluginConfig.set("spawners.enabled", true);
                pluginConfig.set("spawners.spawn-range", 4);
                pluginConfig.set("spawners.minimum-spawn-count", 1);
                pluginConfig.set("spawners.maximum-spawn-count", 4);
                pluginConfig.set("spawners.minimum-delay", 200);
                pluginConfig.set("spawners.maximum-delay", 5000);
                pluginConfig.set("spawners.maximum-nearby-entities", 16);
                pluginConfig.set("spawners.required-player-range", 16);
                pluginConfig.set("spawners.do-not-waste-spawn-eggs", false);
                pluginConfig.set("other.worlds-entity-creation-disabled", "exampledworld1, exampleworld2");
                pluginConfig.set("other.creeper-explode-blocks", true);
                pluginConfig.set("autospawn.hoglin", 0);
            }

            if (current < 15) {
                pluginConfig.set("other.check-tamed-entity-attack", true);
            }

            if (current < 14) {
                pluginConfig.set("autospawn.piglin", 0);
            }

            if (current < 13) {
                pluginConfig.set("autospawn.fox", 0);
                pluginConfig.set("autospawn.panda", 0);
                pluginConfig.set("autospawn.drowned", 0);
            }

            if (current < 12) {
                pluginConfig.set("other.end-enderman-spawning", 10);
            }

            if (current < 11) {
                pluginConfig.set("other.kill-mobs-on-despawn", false);
            }

            if (current < 10) {
                pluginConfig.set("other.spawn-no-spawning-area", -1);
            }

            pluginConfig.set("config-version", ver);
            pluginConfig.save();
            plugin.getLogger().notice("Config file updated to version " + ver);
        }

        //entities
        spawnDelay = pluginConfig.getInt("entities.autospawn-ticks") >> 1; // The task runs double the speed but spawns only either monsters or animals
        despawnTicks = pluginConfig.getInt("entities.despawn-ticks");
        mobSpawningDisabledWorlds = loadStringListAsSet("entities.worlds-spawning-disabled");

        //spawners
        spawnersEnabled = pluginConfig.getBoolean("spawners.enabled");
        spawnerRange = pluginConfig.getInt("spawners.spawn-range");
        spawnerMinSpawnCount = pluginConfig.getInt("spawners.minimum-spawn-count");
        spawnerMaxSpawnCount = pluginConfig.getInt("spawners.maximum-spawn-count");
        spawnerMinDelay = pluginConfig.getInt("spawners.minimum-delay");
        spawnerMaxDelay = pluginConfig.getInt("spawners.maximum-delay");
        spawnerMaxNearby = pluginConfig.getInt("spawners.maximum-nearby-entities");
        spawnerRequiredPlayerRange = pluginConfig.getInt("spawners.required-player-range");
        noSpawnEggWasting = pluginConfig.getBoolean("spawners.do-not-waste-spawn-eggs");

        //other
        noXpOrbs = pluginConfig.getBoolean("other.use-no-xp-orbs");
        spawnNoSpawningArea = pluginConfig.getInt("other.spawn-no-spawning-area");
        killOnDespawn = pluginConfig.getBoolean("other.kill-mobs-on-despawn");
        endEndermanSpawnRate = pluginConfig.getInt("other.end-enderman-spawning");
        checkTamedEntityAttack = pluginConfig.getBoolean("other.check-tamed-entity-attack");
        creeperExplodeBlocks = pluginConfig.getBoolean("other.creeper-explode-blocks");
        mobCreationDisabledWorlds = loadStringListAsSet("other.worlds-entity-creation-disabled");
        allowBreeding = pluginConfig.getBoolean("other.allow-breeding");
        showBossBar = pluginConfig.getBoolean("other.show-boss-bar");
        return true;
    }

    private Set<String> loadStringListAsSet(String key) {
        Set<String> list = new HashSet<>();
        String input = pluginConfig.getString(key).toLowerCase();
        if (!input.trim().isEmpty()) {
            StringTokenizer tokenizer = new StringTokenizer(input, ", ");
            while (tokenizer.hasMoreTokens()) {
                list.add(tokenizer.nextToken());
            }
        }
        return list;
    }
}
