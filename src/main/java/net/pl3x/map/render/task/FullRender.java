package net.pl3x.map.render.task;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.pl3x.map.configuration.Lang;
import net.pl3x.map.logger.Logger;
import net.pl3x.map.render.iterator.RegionSpiralIterator;
import net.pl3x.map.render.iterator.coordinate.Coordinate;
import net.pl3x.map.render.iterator.coordinate.RegionCoordinate;
import net.pl3x.map.render.progress.Progress;
import net.pl3x.map.render.queue.ScanRegion;
import net.pl3x.map.util.FileUtil;
import net.pl3x.map.world.MapWorld;
import org.bukkit.Bukkit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FullRender extends AbstractRender {
    private int maxRadius = 0;
    private long timeStarted;

    public FullRender(MapWorld mapWorld, Audience starter) {
        super(mapWorld, "FullRender", starter);
    }

    @Override
    public void render() {
        this.timeStarted = System.currentTimeMillis();

        Lang.send(getStarter(), Lang.COMMAND_FULLRENDER_OBTAINING_REGIONS);

        List<RegionCoordinate> regionFiles = new ArrayList<>();
        List<Path> files = FileUtil.getRegionFiles(getWorld().getLevel());
        for (Path path : files) {
            if (isCancelled()) {
                return;
            }

            if (path.toFile().length() == 0) {
                continue;
            }

            String filename = path.getFileName().toString();
            String[] split = filename.split("\\.");
            int x, z;
            try {
                x = Integer.parseInt(split[1]);
                z = Integer.parseInt(split[2]);
            } catch (NumberFormatException e) {
                Logger.warn(Lang.COMMAND_FULLRENDER_ERROR_PARSING_REGION_FILE
                        .replace("<path>", path.toString())
                        .replace("<filename>", filename));
                e.printStackTrace();
                continue;
            }

            RegionCoordinate region = new RegionCoordinate(x, z);
            regionFiles.add(region);

            this.maxRadius = Math.max(Math.max(this.maxRadius, Math.abs(x)), Math.abs(z));
        }

        RegionSpiralIterator spiral = new RegionSpiralIterator(
                Coordinate.blockToRegion(getCenterX()),
                Coordinate.blockToRegion(getCenterZ()),
                this.maxRadius);

        Map<RegionCoordinate, ScanRegion> tasks = new LinkedHashMap<>();

        int failsafe = 0;
        while (spiral.hasNext()) {
            if (isCancelled()) {
                return;
            }

            if (failsafe > 500000) {
                // we scanned over half a million non-existent regions straight
                // quit the spiral and add the remaining regions to the end
                regionFiles.forEach(region -> tasks.put(region, new ScanRegion(this, region)));
                break;
            }

            RegionCoordinate region = spiral.next();
            if (regionFiles.remove(region)) {
                tasks.put(region, new ScanRegion(this, region));
                failsafe = 0;
            } else {
                failsafe++;
            }
        }

        getProgress().setTotalRegions(tasks.size());
        getProgress().setTotalChunks(getProgress().getTotalRegions() * 32L * 32L);

        Lang.send(getStarter(), Lang.COMMAND_FULLRENDER_FOUND_TOTAL_REGIONS
                .replace("<total>", Long.toString(getProgress().getTotalRegions())));

        Lang.send(getStarter(), Lang.COMMAND_FULLRENDER_USE_STATUS_FOR_PROGRESS);

        tasks.forEach((region, task) -> ThreadManager.INSTANCE.getRenderExecutor().submit(task));
    }

    @Override
    public void onStart() {
        Component component = Lang.parse(Lang.COMMAND_FULLRENDER_STARTING,
                Placeholder.unparsed("world", getWorld().getName()));
        Lang.send(getStarter(), component);
        if (!getStarter().equals(Bukkit.getConsoleSender())) {
            Lang.send(Bukkit.getConsoleSender(), component);
        }
    }

    @Override
    public void onFinish() {
        long timeEnded = System.currentTimeMillis();
        String elapsed = Progress.formatMilliseconds(timeEnded - this.timeStarted);
        Component component = Lang.parse(Lang.COMMAND_FULLRENDER_FINISHED,
                Placeholder.unparsed("world", getWorld().getName()),
                Placeholder.parsed("elapsed", elapsed));
        Lang.send(getStarter(), component);
        if (!getStarter().equals(Bukkit.getConsoleSender())) {
            Lang.send(Bukkit.getConsoleSender(), component);
        }
    }

    @Override
    public void onCancel() {
        Component component = Lang.parse(Lang.COMMAND_FULLRENDER_CANCELLED,
                Placeholder.unparsed("world", getWorld().getName()));
        Lang.send(getStarter(), component);
        if (!getStarter().equals(Bukkit.getConsoleSender())) {
            Lang.send(Bukkit.getConsoleSender(), component);
        }
    }
}