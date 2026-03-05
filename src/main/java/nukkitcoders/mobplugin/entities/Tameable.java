package nukkitcoders.mobplugin.entities;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;

/**
 * Interface that is implemented in tameable entities
 *
 * @author <a href="mailto:kniffman@googlemail.com">Michael Gertz</a>
 */
public interface Tameable {

    String NAMED_TAG_OWNER_UUID = "OwnerUUID";

    String NAMED_TAG_SITTING = "Sitting";

    Player getOwner();

    boolean hasOwner();

    void setOwner(Player player);

    String getOwnerUUID();

    void setOwnerUUID(String uuid);

    boolean isSitting();

    void setSitting(boolean sitting);

    default boolean isOwner(Entity entity) {
        return entity instanceof Player && ((Player) entity).getUniqueId().toString().equals(this.getOwnerUUID());
    }
}
