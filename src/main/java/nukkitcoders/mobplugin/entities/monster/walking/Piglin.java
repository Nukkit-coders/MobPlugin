package nukkitcoders.mobplugin.entities.monster.walking;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.EntityLiving;
import cn.nukkit.entity.projectile.EntityArrow;
import cn.nukkit.entity.projectile.EntityProjectile;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityShootBowEvent;
import cn.nukkit.event.entity.ProjectileLaunchEvent;
import cn.nukkit.inventory.*;
import cn.nukkit.item.*;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.network.protocol.LevelSoundEventPacket;
import cn.nukkit.network.protocol.MobArmorEquipmentPacket;
import cn.nukkit.network.protocol.MobEquipmentPacket;
import nukkitcoders.mobplugin.entities.monster.WalkingMonster;
import nukkitcoders.mobplugin.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Piglin extends WalkingMonster {

    public final static int NETWORK_ID = 123;

    private Item offhandItem;
    private Item handItem;
    private int angry;
    private boolean angryFlagSet;

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    public Piglin(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public double getSpeed() {
        return 1.1;
    }

    @Override
    public int getKillExperience() {
        return this.isBaby() ? 1 : 5;
    }

    @Override
    protected void initEntity() {
        this.setMaxHealth(16);
        super.initEntity();
        this.setDamage(new float[]{0, 3, 5, 7});

        if (this.namedTag.contains("ItemHand")) {
            this.setHandItem(NBTIO.getItemHelper(this.namedTag.getCompound("ItemHand")));
        } else if (!this.isBaby()) {
            if (Utils.rand(1, 2) == 1) {
                this.setHandItem(Item.get(Item.GOLDEN_SWORD));
                this.setDamage(new float[]{0, 5, 9, 13});
            } else {
                this.setHandItem(Item.get(Item.CROSSBOW));
            }
        }

        if (this.namedTag.contains("ItemOffHand")) {
            this.setOffhandItem(NBTIO.getItemHelper(this.namedTag.getCompound("ItemOffHand")));
        }

        if (!this.namedTag.contains("Armor") || !(this.namedTag.get("Armor") instanceof ListTag)) {
            this.setArmor(getRandomGoldArmor());
            this.namedTag.putList(new ListTag<CompoundTag>("Armor"));
            return;
        }
        ListTag<CompoundTag> armor = this.namedTag.getList("Armor", CompoundTag.class);
        Item[] armorItems = new Item[4];
        for (CompoundTag tag : armor.getAll()) {
            Item item = NBTIO.getItemHelper(tag);
            armorItems[tag.getByte("Slot")] = item;
        }
        this.setArmor(armorItems);
    }

    @Override
    public float getWidth() {
        return 0.6f;
    }

    @Override
    public float getHeight() {
        return 1.95f;
    }

    @Override
    public void attackEntity(Entity player) {
        if (!(player instanceof EntityLiving)) {
            return;
        }

        if (handItem instanceof ItemCrossbow) {
            if (this.attackDelay > 40 && this.distanceSquared(player) <= 256) { // 16 blocks
                if (!this.seesTarget(player)) {
                    return;
                }

                this.attackDelay = 0;

                EntityArrow shot = (EntityArrow) Entity.createEntity("Arrow", this.add(0, this.getEyeHeight(), 0), this);

                if (Utils.hasCollisionBlocks(shot.level, shot, shot.boundingBox)) {
                    shot.close();
                    return;
                }

                EntityShootBowEvent ev = new EntityShootBowEvent(this, Item.get(Item.ARROW, 0, 1), shot, 2);
                this.server.getPluginManager().callEvent(ev);

                shot.setMotion(player.add(Utils.rand(-0.1, 0.1), Utils.rand(-0.1, 0.1) + 0.3, Utils.rand(-0.1, 0.1)).subtract(this).normalize().multiply(ev.getForce()));

                EntityProjectile projectile = ev.getProjectile();
                if (ev.isCancelled()) {
                    projectile.close();
                } else {
                    ProjectileLaunchEvent launch = new ProjectileLaunchEvent(projectile);
                    this.server.getPluginManager().callEvent(launch);
                    if (launch.isCancelled()) {
                        projectile.close();
                    } else {
                        projectile.namedTag.putDouble("damage", 4);
                        projectile.updateRotation();
                        projectile.spawnToAll();
                        ((EntityArrow) projectile).setPickupMode(EntityArrow.PICKUP_NONE);
                        this.level.addLevelSoundEvent(this, LevelSoundEventPacket.SOUND_CROSSBOW_SHOOT);
                    }
                }
            }
        } else {
            if (this.attackDelay > 30 && player.distanceSquared(this) <= 1) {
                this.attackDelay = 0;
                HashMap<EntityDamageEvent.DamageModifier, Float> damage = new HashMap<>();
                damage.put(EntityDamageEvent.DamageModifier.BASE, this.getDamage());

                if (player instanceof Player) {
                    float points = 0;
                    for (Item i : ((Player) player).getInventory().getArmorContents()) {
                        points += this.getArmorPoints(i.getId());
                    }

                    damage.put(EntityDamageEvent.DamageModifier.ARMOR,
                            (float) (damage.getOrDefault(EntityDamageEvent.DamageModifier.ARMOR, 0f) - Math.floor(damage.getOrDefault(EntityDamageEvent.DamageModifier.BASE, 1f) * points * 0.04)));
                }
                player.attack(new EntityDamageByEntityEvent(this, player, EntityDamageEvent.DamageCause.ENTITY_ATTACK, damage));
                this.playAttack();
            }
        }
    }

    public boolean isAngry() {
        return this.angry > 0;
    }

    public void setAngry(int val) {
        this.angry = val;
        this.setDataFlag(DATA_FLAGS, DATA_FLAG_CHARGED, val > 0);
        this.angryFlagSet = val > 0;
    }

    private static boolean isWearingGold(Player p) {
        PlayerInventory i = p.getInventory();
        if (i == null) {
            return false;
        }
        return i.getHelmetFast().getId() == Item.GOLD_HELMET ||
                i.getChestplateFast().getId() == Item.GOLD_CHESTPLATE ||
                i.getLeggingsFast().getId() == Item.GOLD_LEGGINGS ||
                i.getBootsFast().getId() == Item.GOLD_BOOTS;
    }

    @Override
    public boolean attack(EntityDamageEvent ev) {
        super.attack(ev);

        if (!ev.isCancelled() && ev instanceof EntityDamageByEntityEvent) {
            if (((EntityDamageByEntityEvent) ev).getDamager() instanceof Player) {
                this.setAngry(600);
            }
        }

        return true;
    }

    @Override
    public boolean targetOption(EntityCreature creature, double distance) {
        if (this.isAngry() && distance <= 256 && creature instanceof Piglin && !((Piglin) creature).isAngry()) {
            ((Piglin) creature).setAngry(600);
        }
        boolean hasTarget = creature instanceof Player && (this.isAngry() || !isWearingGold((Player) creature)) && targetOptionInternal(creature, distance);
        if (hasTarget) {
            if (!this.angryFlagSet) {
                this.setDataFlag(DATA_FLAGS, DATA_FLAG_CHARGED, true);
                this.angryFlagSet = true;
            }
        } else {
            if (this.angryFlagSet) {
                this.setDataFlag(DATA_FLAGS, DATA_FLAG_CHARGED, false);
                this.angryFlagSet = false;
                this.stayTime = 100;
            }
        }
        return hasTarget;
    }

    @Override
    public void spawnTo(Player player) {
        super.spawnTo(player);

        if (this.offhandItem != null) {
            MobEquipmentPacket pk = new MobEquipmentPacket();
            pk.eid = this.getId();
            pk.inventorySlot = 1;
            pk.item = this.offhandItem;
            player.dataPacket(pk);
        }

        if (this.handItem != null) {
            MobEquipmentPacket pk = new MobEquipmentPacket();
            pk.eid = this.getId();
            pk.hotbarSlot = 1;
            pk.item = this.handItem;
            player.dataPacket(pk);
        }

        if (armor != null) {
            MobArmorEquipmentPacket pk = new MobArmorEquipmentPacket();
            pk.eid = this.getId();
            pk.slots = this.armor;
            player.dataPacket(pk);
        }
    }

    private static Item[] getRandomGoldArmor() {
        Item[] randomArmor = new Item[4];
        if (Utils.rand(1, 4) == 1) {
            randomArmor[0] = Item.get(Item.GOLD_HELMET, Utils.rand(10, 77));
        }
        if (Utils.rand(1, 4) == 1) {
            randomArmor[1] = Item.get(Item.GOLD_CHESTPLATE, Utils.rand(10, 112));
        }
        if (Utils.rand(1, 4) == 1) {
            randomArmor[2] = Item.get(Item.GOLD_LEGGINGS, Utils.rand(10, 105));
        }
        if (Utils.rand(1, 4) == 1) {
            randomArmor[3] = Item.get(Item.GOLD_BOOTS, Utils.rand(10, 91));
        }
        return randomArmor;
    }

    public void setArmor(Item[] armor) {
        this.armor = armor;
        this.spawnToAll();
    }

    public void setHandItem(Item item) {
        this.handItem = item;
        this.spawnToAll();
    }

    public void setOffhandItem(Item item) {
        this.offhandItem = item;
        this.spawnToAll();
    }

    public Item getHandItem() {
        return handItem;
    }

    public Item getOffhandItem() {
        return offhandItem;
    }

    @Override
    public int nearbyDistanceMultiplier() {
        return handItem instanceof ItemCrossbow && (target instanceof EntityLiving || followTarget instanceof EntityLiving) ? 20 : 1;
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        if (this.closed) {
            return false;
        }

        if (this.isAlive() && this.level != null) {
            if (this.angry > 0) {
                if (this.angry == 1) {
                    this.setAngry(0); // Reset flag
                } else {
                    this.angry--;
                }
            }
        }

        return super.entityBaseTick(tickDiff);
    }

    @Override
    public Item[] getDrops() {
        List<Item> drops = new ArrayList<>();

        for (Item item : this.armor) {
            if (item == null) {
                continue;
            }

            if (Utils.rand(1, 200) <= 17) {
                drops.add(item);
            }
        }
        if (Utils.rand(1, 200) <= 17) {
            drops.add(handItem == null ? Item.get(Item.AIR) : handItem);
        }

        drops.add(offhandItem == null ? Item.get(Item.AIR) : offhandItem);
        return drops.toArray(new Item[0]);
    }

    @Override
    public void saveNBT() {
        this.namedTag.putList(new ListTag<CompoundTag>("Armor"));
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null) {
                this.namedTag.getList("Armor", CompoundTag.class).add(NBTIO.putItemHelper(armor[i], i));
            }
        }
        if (handItem != null) {
            this.namedTag.putCompound("ItemHand", NBTIO.putItemHelper(handItem));
        }

        if (offhandItem != null) {
            this.namedTag.putCompound("ItemOffHand", NBTIO.putItemHelper(offhandItem));
        }


        super.saveNBT();
    }

    private boolean targetOptionInternal(EntityCreature creature, double distance) {
        if (creature instanceof Player) {
            Player player = (Player) creature;
            if (!player.closed && player.spawned && player.isAlive() && (player.isSurvival() || player.isAdventure())) {
                PlayerInventory inv = player.getInventory();
                Item helmet;
                if (inv != null && (helmet = inv.getHelmetFast()).getId() == Item.SKULL && helmet.getDamage() == ItemSkull.PIGLIN_HEAD) {
                    return distance <= 64;
                }
                return distance <= 256;
            }
            return false;
        }
        return creature.isAlive() && !creature.closed && distance <= 256;
    }
}
