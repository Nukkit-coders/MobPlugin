package nukkitcoders.mobplugin;

import nukkitcoders.mobplugin.runnable.RouteFinderSearchTask;

import java.util.concurrent.*;

/**
 * @author zzz1999 @ MobPlugin
 */
public class RouteFinderThreadPool {

    private static volatile boolean running = true;

    private static final ThreadPoolExecutor executor =
            new ThreadPoolExecutor(
                    1,
                    Runtime.getRuntime().availableProcessors(),
                    1, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(),
                    new ThreadPoolExecutor.AbortPolicy()
            );

    public static void executeRouteFinderThread(RouteFinderSearchTask t) {
        if (running) {
            executor.execute(t);
        }
    }

    public static void shutdown() {
        running = false;
        executor.shutdownNow();
    }
}
