package nukkitcoders.mobplugin;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.projectile.EntityEgg;
import cn.nukkit.entity.projectile.EntityEnderPearl;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.entity.*;
import cn.nukkit.event.player.PlayerDeathEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.event.server.DataPacketReceiveEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemSkull;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.lang.TranslationContainer;
import cn.nukkit.level.GameRule;
import cn.nukkit.level.Sound;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.SimpleAxisAlignedBB;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.EntityEventPacket;
import cn.nukkit.network.protocol.PlayerAuthInputPacket;
import cn.nukkit.network.protocol.TextPacket;

import nukkitcoders.mobplugin.entities.BaseEntity;
import nukkitcoders.mobplugin.entities.HorseBase;
import nukkitcoders.mobplugin.entities.Tameable;
import nukkitcoders.mobplugin.entities.animal.walking.Chicken;
import nukkitcoders.mobplugin.entities.animal.walking.Llama;
import nukkitcoders.mobplugin.entities.animal.walking.Pig;
import nukkitcoders.mobplugin.entities.animal.walking.Strider;
import nukkitcoders.mobplugin.entities.block.BlockEntitySpawner;
import nukkitcoders.mobplugin.entities.monster.WalkingMonster;
import nukkitcoders.mobplugin.entities.monster.flying.Wither;
import nukkitcoders.mobplugin.entities.monster.walking.*;
import nukkitcoders.mobplugin.event.spawner.SpawnerChangeTypeEvent;
import nukkitcoders.mobplugin.event.spawner.SpawnerCreateEvent;
import nukkitcoders.mobplugin.utils.FastMathLite;
import nukkitcoders.mobplugin.utils.Utils;


import static nukkitcoders.mobplugin.entities.block.BlockEntitySpawner.*;

public class EventListener implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void EntityDeathEvent(EntityDeathEvent ev) {
        if (ev.getEntity() instanceof EntityCreature) {
            this.handleExperienceOrb(ev.getEntity());
            this.handleTamedEntityDeathMessage(ev.getEntity());
            this.handleAttackedEntityAngry(ev.getEntity());
            if (ev.getEntity() instanceof BaseEntity && ev.getEntity().getLevel().getGameRules().getBoolean(GameRule.DO_MOB_LOOT)) {
                BaseEntity baseEntity = (BaseEntity) ev.getEntity();
                if (!(baseEntity.getLastDamageCause() instanceof EntityDamageByEntityEvent)) {
                    return;
                }
                Entity damager = ((EntityDamageByEntityEvent) baseEntity.getLastDamageCause()).getDamager();
                if (damager instanceof Creeper && damager != baseEntity && baseEntity.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                    if (((Creeper) damager).isPowered()) {
                        Item skull = Utils.getMobHead(baseEntity.getNetworkId());
                        if (skull != null) {
                            baseEntity.getLevel().dropItem(baseEntity, skull);
                        }
                    }
                } else if (baseEntity instanceof Creeper && (damager instanceof Skeleton || damager instanceof Stray) && baseEntity.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
                    baseEntity.getLevel().dropItem(baseEntity, Item.get(Utils.rand(500, 511), 0, 1));
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void PlayerDeathEvent(PlayerDeathEvent ev) {
        this.handleAttackedEntityAngry(ev.getEntity());
    }

    private void handleExperienceOrb(Entity entity) {
        if (!(entity instanceof BaseEntity)) {
            return;
        }

        BaseEntity baseEntity = (BaseEntity) entity;

        if (!(baseEntity.getLastDamageCause() instanceof EntityDamageByEntityEvent)) {
            return;
        }

        Entity damager = ((EntityDamageByEntityEvent) baseEntity.getLastDamageCause()).getDamager();
        if (!(damager instanceof Player)) {
            return;
        }
        int killExperience = baseEntity.getKillExperience();
        if (killExperience > 0) {
            if (MobPlugin.getInstance().config.noXpOrbs) {
                ((Player) damager).addExperience(killExperience);
            } else {
                damager.getLevel().dropExpOrb(baseEntity, killExperience);
            }
        }
    }

    private void handleTamedEntityDeathMessage(Entity entity) {
        if (!(entity instanceof BaseEntity)) {
            return;
        }

        BaseEntity baseEntity = (BaseEntity) entity;

        if (baseEntity instanceof Tameable) {
            if (!((Tameable) baseEntity).hasOwner()) {
                return;
            }

            if (((Tameable) baseEntity).getOwner() == null) {
                return;
            }

            // TODO: More detailed death messages
            String killedEntity;
            if (baseEntity instanceof Wolf) {
                killedEntity = "%entity.wolf.name";
            } else {
                killedEntity = baseEntity.getName();
            }

            TranslationContainer deathMessage = new TranslationContainer("death.attack.generic", killedEntity);
            if (baseEntity.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
                Entity damageEntity = ((EntityDamageByEntityEvent) baseEntity.getLastDamageCause()).getDamager();
                if (damageEntity instanceof Player) {
                    deathMessage = new TranslationContainer("death.attack.player", killedEntity, damageEntity.getName());
                } else {
                    deathMessage = new TranslationContainer("death.attack.mob", killedEntity, damageEntity.getName());
                }
            }

            TextPacket tameDeathMessage = new TextPacket();
            tameDeathMessage.type = TextPacket.TYPE_TRANSLATION;
            tameDeathMessage.message = deathMessage.getText();
            tameDeathMessage.parameters = deathMessage.getParameters();
            tameDeathMessage.isLocalized = true;
            ((Tameable) baseEntity).getOwner().dataPacket(tameDeathMessage);
        }
    }

    private void handleAttackedEntityAngry(Entity entity) {
        if (!(entity.getLastDamageCause() instanceof EntityDamageByEntityEvent)) {
            return;
        }

        Entity damager = ((EntityDamageByEntityEvent) entity.getLastDamageCause()).getDamager();
        if (damager instanceof Wolf) {
            ((Wolf) damager).isAngryTo = -1L;
            ((Wolf) damager).setAngry(false);
        } else if (damager instanceof IronGolem || damager instanceof SnowGolem) {
            ((WalkingMonster) damager).isAngryTo = -1L;
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void PlayerInteractEvent(PlayerInteractEvent ev) {
        if (ev.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK || ev.getFace() == null) {
            return;
        }

        Player player = ev.getPlayer();
        if (player.isAdventure()) {
            return;
        }

        Item item = ev.getItem();
        Block target = ev.getBlock();

        if (item.getId() == Item.SPAWN_EGG && target.getId() == Block.MONSTER_SPAWNER) {
            BlockEntity blockEntity = target.getLevel().getBlockEntity(target);
            if (blockEntity instanceof BlockEntitySpawner) {
                SpawnerChangeTypeEvent event = new SpawnerChangeTypeEvent((BlockEntitySpawner) blockEntity, ev.getBlock(), ev.getPlayer(), ((BlockEntitySpawner) blockEntity).getSpawnEntityType(), item.getDamage());
                Server.getInstance().getPluginManager().callEvent(event);
                if (((BlockEntitySpawner) blockEntity).getSpawnEntityType() == item.getDamage()) {
                    if (MobPlugin.getInstance().config.noSpawnEggWasting) {
                        event.setCancelled(true);
                        return;
                    }
                }

                if (event.isCancelled()) {
                    return;
                }
                ((BlockEntitySpawner) blockEntity).setSpawnEntityType(item.getDamage());
                ev.setCancelled(true);
            } else {
                SpawnerCreateEvent event = new SpawnerCreateEvent(ev.getPlayer(), ev.getBlock(), item.getDamage());
                Server.getInstance().getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return;
                }
                ev.setCancelled(true);
                if (blockEntity != null) {
                    blockEntity.close();
                }
                CompoundTag nbt = new CompoundTag()
                        .putString(TAG_ID, BlockEntity.MOB_SPAWNER)
                        .putInt(TAG_ENTITY_ID, item.getDamage())
                        .putInt(TAG_X, (int) target.x)
                        .putInt(TAG_Y, (int) target.y)
                        .putInt(TAG_Z, (int) target.z);

                BlockEntitySpawner entitySpawner = new BlockEntitySpawner(target.getLevel().getChunk((int) target.x >> 4, (int) target.z >> 4), nbt);
                entitySpawner.spawnToAll();
            }

            if (!player.isCreative()) {
                player.getInventory().decreaseCount(player.getInventory().getHeldItemIndex());
            }
        } else if (MobPlugin.isEntityCreationAllowed(target.getLevel())) {
            Block block = target.getSide(ev.getFace());
            Block originalBlock = block;

            if (item.getId() == Item.JACK_O_LANTERN || item.getId() == Item.PUMPKIN || item.getId() == -155) {
                Block down = block.getSide(BlockFace.DOWN);
                if (down.getId() == Item.SNOW_BLOCK && block.getSide(BlockFace.DOWN, 2).getId() == Item.SNOW_BLOCK) {
                    block.getLevel().setBlock(target, Block.get(BlockID.AIR));
                    block.getLevel().setBlock(target.add(0, -1, 0), Block.get(BlockID.AIR));

                    CreatureSpawnEvent creatureSpawnEvent = new CreatureSpawnEvent(SnowGolem.NETWORK_ID, target.add(0.5, -1, 0.5), new CompoundTag(), CreatureSpawnEvent.SpawnReason.BUILD_SNOWMAN);
                    Server.getInstance().getPluginManager().callEvent(creatureSpawnEvent);

                    if (creatureSpawnEvent.isCancelled()) {
                        return;
                    }

                    Entity.createEntity("SnowGolem", creatureSpawnEvent.getPosition()).spawnToAll();

                    ev.setCancelled(true);

                    if (!player.isCreative()) {
                        item.setCount(item.getCount() - 1);
                        player.getInventory().setItemInHand(item);
                    }
                } else if (down.getId() == Item.IRON_BLOCK && block.getSide(BlockFace.DOWN, 2).getId() == Item.IRON_BLOCK) {
                    block = down;
                    Block first, second = null;
                    if ((first = block.getSide(BlockFace.EAST)).getId() == Item.IRON_BLOCK && (second = block.getSide(BlockFace.WEST)).getId() == Item.IRON_BLOCK) {
                        block.getLevel().setBlock(first, Block.get(BlockID.AIR));
                        block.getLevel().setBlock(second, Block.get(BlockID.AIR));
                    } else if ((first = block.getSide(BlockFace.NORTH)).getId() == Item.IRON_BLOCK && (second = block.getSide(BlockFace.SOUTH)).getId() == Item.IRON_BLOCK) {
                        block.getLevel().setBlock(first, Block.get(BlockID.AIR));
                        block.getLevel().setBlock(second, Block.get(BlockID.AIR));
                    }

                    if (second != null) {
                        block.getLevel().setBlock(block, Block.get(BlockID.AIR));
                        block.getLevel().setBlock(block.add(0, -1, 0), Block.get(BlockID.AIR));

                        CreatureSpawnEvent creatureSpawnEvent = new CreatureSpawnEvent(IronGolem.NETWORK_ID, block.add(0.5, -1, 0.5), new CompoundTag(), CreatureSpawnEvent.SpawnReason.BUILD_IRONGOLEM);
                        Server.getInstance().getPluginManager().callEvent(creatureSpawnEvent);

                        if (creatureSpawnEvent.isCancelled()) {
                            return;
                        }

                        Entity.createEntity("IronGolem", creatureSpawnEvent.getPosition()).spawnToAll();

                        ev.setCancelled(true);

                        if (!player.isCreative()) {
                            item.setCount(item.getCount() - 1);
                            player.getInventory().setItemInHand(item);
                        }
                    }
                }
            } else if (item.getId() == Item.SKULL && item.getDamage() == ItemSkull.WITHER_SKELETON_SKULL) {
                Block down = block.getSide(BlockFace.DOWN);

                if (down.getId() == Item.SOUL_SAND) {
                    if (block.getSide(BlockFace.DOWN, 2).getId() == Item.SOUL_SAND) {
                        Block first, second;

                        if (((first = block.getSide(BlockFace.EAST)).getId() == Item.SKULL_BLOCK && first.toItem().getDamage() == ItemSkull.WITHER_SKELETON_SKULL) && ((second = block.getSide(BlockFace.WEST)).getId() == Item.SKULL_BLOCK && second.toItem().getDamage() == ItemSkull.WITHER_SKELETON_SKULL) ||
                                ((first = block.getSide(BlockFace.NORTH)).getId() == Item.SKULL_BLOCK && first.toItem().getDamage() == ItemSkull.WITHER_SKELETON_SKULL) && ((second = block.getSide(BlockFace.SOUTH)).getId() == Item.SKULL_BLOCK && second.toItem().getDamage() == ItemSkull.WITHER_SKELETON_SKULL)) {

                            block = down;

                            Block first2, second2;
                            if ((first2 = block.getSide(BlockFace.EAST)).getId() == Item.SOUL_SAND && (second2 = block.getSide(BlockFace.WEST)).getId() == Item.SOUL_SAND || (first2 = block.getSide(BlockFace.NORTH)).getId() == Item.SOUL_SAND && (second2 = block.getSide(BlockFace.SOUTH)).getId() == Item.SOUL_SAND) {

                                block.getLevel().setBlock(first, Block.get(BlockID.AIR));
                                block.getLevel().setBlock(second, Block.get(BlockID.AIR));
                                block.getLevel().setBlock(first2, Block.get(BlockID.AIR));
                                block.getLevel().setBlock(second2, Block.get(BlockID.AIR));
                                block.getLevel().setBlock(block, Block.get(BlockID.AIR));
                                block.getLevel().setBlock(block.add(0, -1, 0), Block.get(BlockID.AIR));

                                CreatureSpawnEvent creatureSpawnEvent = new CreatureSpawnEvent(Wither.NETWORK_ID, block.add(0.5, -1, 0.5), new CompoundTag(), CreatureSpawnEvent.SpawnReason.BUILD_WITHER);
                                Server.getInstance().getPluginManager().callEvent(creatureSpawnEvent);

                                if (creatureSpawnEvent.isCancelled()) {
                                    return;
                                }

                                if (!player.isCreative()) {
                                    item.setCount(item.getCount() - 1);
                                    player.getInventory().setItemInHand(item);
                                }

                                Entity.createEntity("Wither", creatureSpawnEvent.getPosition()).spawnToAll();
                                player.level.addSound(creatureSpawnEvent.getPosition(), Sound.MOB_WITHER_SPAWN);
                                player.awardAchievement("spawnWither");

                                ev.setCancelled(true);
                            }
                        }
                    } else {
                        for (BlockFace side : BlockFace.Plane.HORIZONTAL) {
                            block = originalBlock.getSide(side);

                            if (block.getId() == Item.SKULL_BLOCK && block.toItem().getDamage() == ItemSkull.WITHER_SKELETON_SKULL &&
                                    (down = block.getSide(BlockFace.DOWN)).getId() == Item.SOUL_SAND &&
                                    block.getSide(BlockFace.DOWN, 2).getId() == Item.SOUL_SAND) {

                                Block first, second;

                                if ((((first = block.getSide(BlockFace.EAST)).getId() == Item.SKULL_BLOCK && first.toItem().getDamage() == ItemSkull.WITHER_SKELETON_SKULL) || first.getLocation().equals(originalBlock.getLocation())) &&
                                        (((second = block.getSide(BlockFace.WEST)).getId() == Item.SKULL_BLOCK && second.toItem().getDamage() == ItemSkull.WITHER_SKELETON_SKULL) || first.getLocation().equals(originalBlock.getLocation())) ||
                                        (((first = block.getSide(BlockFace.NORTH)).getId() == Item.SKULL_BLOCK && first.toItem().getDamage() == ItemSkull.WITHER_SKELETON_SKULL) || first.getLocation().equals(originalBlock.getLocation())) &&
                                                (((second = block.getSide(BlockFace.SOUTH)).getId() == Item.SKULL_BLOCK && second.toItem().getDamage() == ItemSkull.WITHER_SKELETON_SKULL) || first.getLocation().equals(originalBlock.getLocation()))) {

                                    block = down;

                                    Block first2, second2;
                                    if ((first2 = block.getSide(BlockFace.EAST)).getId() == Item.SOUL_SAND && (second2 = block.getSide(BlockFace.WEST)).getId() == Item.SOUL_SAND || (first2 = block.getSide(BlockFace.NORTH)).getId() == Item.SOUL_SAND && (second2 = block.getSide(BlockFace.SOUTH)).getId() == Item.SOUL_SAND) {

                                        block.getLevel().setBlock(first, Block.get(BlockID.AIR));
                                        block.getLevel().setBlock(second, Block.get(BlockID.AIR));
                                        block.getLevel().setBlock(first2, Block.get(BlockID.AIR));
                                        block.getLevel().setBlock(second2, Block.get(BlockID.AIR));
                                        block.getLevel().setBlock(block, Block.get(BlockID.AIR));
                                        block.getLevel().setBlock(block.add(0, -1, 0), Block.get(BlockID.AIR));
                                        block.getLevel().setBlock(block.add(0, 1, 0), Block.get(BlockID.AIR));

                                        CreatureSpawnEvent creatureSpawnEvent = new CreatureSpawnEvent(Wither.NETWORK_ID, block.add(0.5, -1, 0.5), new CompoundTag(), CreatureSpawnEvent.SpawnReason.BUILD_WITHER);
                                        Server.getInstance().getPluginManager().callEvent(creatureSpawnEvent);

                                        if (creatureSpawnEvent.isCancelled()) {
                                            return;
                                        }

                                        if (!player.isCreative()) {
                                            item.setCount(item.getCount() - 1);
                                            player.getInventory().setItemInHand(item);
                                        }

                                        Entity.createEntity("Wither", creatureSpawnEvent.getPosition()).spawnToAll();
                                        player.level.addSound(creatureSpawnEvent.getPosition(), Sound.MOB_WITHER_SPAWN);
                                        player.awardAchievement("spawnWither");

                                        ev.setCancelled(true);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void BlockBreakEvent(BlockBreakEvent ev) {
        Block block = ev.getBlock();
        if ((block.getId() == Block.MONSTER_EGG) && Utils.rand(1, 5) == 1 && !ev.getItem().hasEnchantment(Enchantment.ID_SILK_TOUCH) && block.level.getBlockLightAt((int) block.x, (int) block.y, (int) block.z) < 12) {
            Silverfish entity = (Silverfish) Entity.createEntity("Silverfish", block.add(0.5, 0, 0.5));
            if (entity == null) {
                return;
            }
            entity.spawnToAll();
            EntityEventPacket pk = new EntityEventPacket();
            pk.eid = entity.getId();
            pk.event = EntityEventPacket.SILVERFISH_SPAWN_ANIMATION;
            entity.level.addChunkPacket(entity.getChunkX(), entity.getChunkZ(), pk);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void ProjectileHitEvent(ProjectileHitEvent ev) {
        if (ev.getEntity() instanceof EntityEgg) {
            if (Utils.rand(1, 20) == 5) {
                Chicken entity = (Chicken) Entity.createEntity("Chicken", ev.getEntity().add(0.5, 1, 0.5));
                if (entity != null) {
                    entity.spawnToAll();
                    entity.setBaby(true);
                }
            }
        }

        if (ev.getEntity() instanceof EntityEnderPearl) {
            if (Utils.rand(1, 20) == 5) {
                Entity entity = Entity.createEntity("Endermite", ev.getEntity().add(0.5, 1, 0.5));
                if (entity != null) {
                    entity.spawnToAll();
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void DataPacketReceiveEvent(DataPacketReceiveEvent ev) {
        if (ev.getPacket() instanceof PlayerAuthInputPacket) {
            Player p = ev.getPlayer();
            if (!p.locallyInitialized) {
                return;
            }
            PlayerAuthInputPacket pk = (PlayerAuthInputPacket) ev.getPacket();
            double inputX = pk.getMotion().getX();
            double inputY = pk.getMotion().getY();
            if (inputX >= -1.0 && inputX <= 1.0 && inputY >= -1.0 && inputY <= 1.0) {
                if (p.riding instanceof HorseBase && !(p.riding instanceof Llama)) {
                    ((HorseBase) p.riding).onPlayerInput(p, inputX, inputY);
                } else if (p.riding instanceof Pig) {
                    ((Pig) p.riding).onPlayerInput(p, inputX, inputY);
                } else if (p.riding instanceof Strider) {
                    ((Strider) p.riding).onPlayerInput(p, inputX, inputY);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void PlayerMoveEvent(PlayerMoveEvent ev) {
        Player player = ev.getPlayer();
        if (player.ticksLived % 20 == 0) {
            AxisAlignedBB aab = new SimpleAxisAlignedBB(
                    player.getX() - 0.6f,
                    player.getY() + 1.45f,
                    player.getZ() - 0.6f,
                    player.getX() + 0.6f,
                    player.getY() + 2.9f,
                    player.getZ() + 0.6f
            );

            for (int i = 0; i < 8; i++) {
                aab.offset(-FastMathLite.sin(player.getYaw() * Math.PI / 180) * i, i * (Math.tan(player.getPitch() * -3.141592653589793 / 180)), FastMathLite.cos(player.getYaw() * Math.PI / 180) * i);
                Entity[] entities = player.getLevel().getCollidingEntities(aab);
                for (Entity e : entities) {
                    if (e instanceof Enderman) {
                        ((Enderman) e).stareToAngry();
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void EntityDamageByEntityEvent(EntityDamageByEntityEvent ev) {
        if (!MobPlugin.getInstance().config.checkTamedEntityAttack) {
            return;
        }

        if (ev.getEntity() instanceof Player) {
            for (Entity entity : ev.getEntity().getLevel().getNearbyEntities(ev.getEntity().getBoundingBox().grow(17, 17, 17), ev.getEntity())) {
                if (entity instanceof Wolf) {
                    if (((Wolf) entity).hasOwner()) {
                        ((Wolf) entity).isAngryTo = ev.getDamager().getId();
                        ((Wolf) entity).setAngry(true);
                    }
                }
            }
        } else if (ev.getDamager() instanceof Player) {
            for (Entity entity : ev.getDamager().getLevel().getNearbyEntities(ev.getDamager().getBoundingBox().grow(17, 17, 17), ev.getDamager())) {
                if (entity.getId() == ev.getEntity().getId()) {
                    return;
                }

                if (entity instanceof Wolf) {
                    if (((Wolf) entity).hasOwner()) {
                        if (((Wolf) entity).getOwner().equals(ev.getDamager())) {
                            ((Wolf) entity).isAngryTo = ev.getEntity().getId();
                            ((Wolf) entity).setAngry(true);
                        }
                    }
                }
            }
        }
    }
}
