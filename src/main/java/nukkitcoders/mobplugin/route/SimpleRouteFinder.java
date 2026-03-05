package nukkitcoders.mobplugin.route;

import nukkitcoders.mobplugin.entities.WalkingEntity;

/**
 * @author zzz1999 @ MobPlugin
 */
public class SimpleRouteFinder extends RouteFinder {

    public SimpleRouteFinder(WalkingEntity entity) {
        super(entity);
    }

    @Override
    public void search() {
        this.resetNodes();
        this.addNode(new Node(this.destination));
    }
}