package nukkitcoders.mobplugin.entities.monster.walking;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockBricksStone;
import cn.nukkit.block.BlockMonsterEgg;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityArthropod;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.GameRule;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.ExplodeParticle;
import cn.nukkit.nbt.tag.CompoundTag;
import nukkitcoders.mobplugin.entities.monster.WalkingMonster;
import nukkitcoders.mobplugin.utils.Utils;

import java.util.HashMap;

public class Silverfish extends WalkingMonster implements EntityArthropod {

    public static final int NETWORK_ID = 39;

    public Silverfish(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public float getWidth() {
        return 0.4f;
    }

    @Override
    public float getHeight() {
        return 0.3f;
    }

    @Override
    public double getSpeed() {
        return 1.4;
    }

    @Override
    public void initEntity() {
        this.setMaxHealth(8);
        super.initEntity();

        this.setDamage(new float[] { 0, 1, 1, 1 });
    }

    @Override
    public void attackEntity(Entity player) {
        if (this.attackDelay > 23 && this.distanceSquared(player) < 1) {
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
        }
    }

    @Override
    public int getKillExperience() {
        return 5;
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        boolean update = super.entityBaseTick(tickDiff);
        if (this.closed) {
            return false;
        }
        if (isCollided && followTarget == null && !(target instanceof Entity) && age % 100 == 0 && Utils.rand(0, 5) == 3 && level.getGameRules().getBoolean(GameRule.MOB_GRIEFING)) {
            Block[] blocks = this.level.getCollisionBlocks(this.getBoundingBox().grow(0.1, 0.1, 0.1));
            for (Block b : blocks) {
                int id = b.getId();
                switch (id) {
                    case Block.STONE:
                        this.level.setBlockAt((int) b.x, (int) b.y, (int) b.z, Block.MONSTER_EGG, BlockMonsterEgg.STONE);
                        this.getLevel().addParticle(new ExplodeParticle(b.add(0.5, 1, 0.5)));
                        this.close();
                        break;
                    case Block.COBBLESTONE:
                        this.level.setBlockAt((int) b.x, (int) b.y, (int) b.z, Block.MONSTER_EGG, BlockMonsterEgg.COBBLESTONE);
                        this.getLevel().addParticle(new ExplodeParticle(b.add(0.5, 1, 0.5)));
                        this.close();
                        break;
                    case Block.STONE_BRICK:
                        this.level.setBlockAt((int) b.x, (int) b.y, (int) b.z, Block.MONSTER_EGG,
                                b.getDamage() == BlockBricksStone.NORMAL ? BlockMonsterEgg.STONE_BRICK : b.getDamage() == BlockBricksStone.MOSSY ? BlockMonsterEgg.MOSSY_BRICK : b.getDamage() == BlockBricksStone.CRACKED ? BlockMonsterEgg.CRACKED_BRICK : b.getDamage() == BlockBricksStone.CHISELED ? BlockMonsterEgg.CHISELED_BRICK : 0);
                        this.getLevel().addParticle(new ExplodeParticle(b.add(0.5, 1, 0.5)));
                        this.close();
                        break;
                }
            }

        }
        return update;
    }
}
