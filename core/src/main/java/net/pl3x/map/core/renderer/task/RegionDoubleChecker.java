package net.pl3x.map.core.renderer.task;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.pl3x.map.core.Pl3xMap;
import net.pl3x.map.core.log.Logger;
import net.pl3x.map.core.markers.Point;
import net.pl3x.map.core.util.Mathf;
import org.checkerframework.checker.nullness.qual.NonNull;

public class RegionDoubleChecker implements Runnable {
    private final Executor executor;
    private CompletableFuture<@NonNull Void> future;

    private boolean running;

    public RegionDoubleChecker() {
        this.executor = Pl3xMap.ThreadFactory.createService("Pl3xMap-DoubleChecker");
    }

    public void start(long delay) {
        if (this.future != null) {
            Logger.debug("Region double checker executor already scheduled!");
            return;
        }
        Logger.debug("Scheduled region double checker executor");
        this.future = CompletableFuture.runAsync(() -> {
            // wait...
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ignore) {
            }

            Logger.debug("Region double checker starting run at " + System.currentTimeMillis());

            // run the task
            run();

            Logger.debug("Region double checker finished run at " + System.currentTimeMillis());

            // rinse and repeat
            this.future = null;
            start(30000L);
        }, this.executor);
    }

    public void stop() {
        if (this.future != null) {
            boolean result = this.future.cancel(true);
            this.future = null;
            Logger.debug("Stopped region double checker executor: " + result);
        }
    }

    @Override
    public void run() {
        if (this.running) {
            // this task is already running
            Logger.debug("Region processor already running!");
            return;
        }

        // consider task as running
        this.running = true;

        //

        try {
            Pl3xMap.api().getWorldRegistry().forEach(world -> {
                Collection<Path> files = world.getRegionFiles();
                Collection<Point> modifiedRegions = new HashSet<>();
                for (Path file : files) {
                    try {
                        String[] split = file.getFileName().toString().split("\\.");
                        int rX = Integer.parseInt(split[1]);
                        int rZ = Integer.parseInt(split[2]);
                        if (!world.visibleRegion(rX, rZ)) {
                            Logger.debug("Skipping region outside of visible areas: " + file.getFileName());
                            continue;
                        }
                        long state = world.getRegionModifiedState().get(Mathf.asLong(rX, rZ));
                        long modified = Files.getLastModifiedTime(file).toMillis();

                        if (state >= modified) {
                            //Logger.debug("Skipping unmodified region: " + file.getFileName());
                            continue;
                        }

                        Logger.debug("Found modified region: " + file.getFileName());
                        modifiedRegions.add(Point.of(rX, rZ));
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
                Pl3xMap.api().getRegionProcessor().addRegions(world, modifiedRegions);
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }

        //

        this.running = false;
    }
}