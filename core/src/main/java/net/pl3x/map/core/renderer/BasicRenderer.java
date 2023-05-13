/*
 * MIT License
 *
 * Copyright (c) 2020-2023 William Blake Galbreath
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.pl3x.map.core.renderer;

import net.pl3x.map.core.markers.Point;
import net.pl3x.map.core.renderer.task.RegionScanTask;
import net.pl3x.map.core.world.Chunk;
import net.pl3x.map.core.world.Region;
import org.jetbrains.annotations.NotNull;

public final class BasicRenderer extends Renderer {
    //private TileImage lightImage;

    public BasicRenderer(@NotNull RegionScanTask task, @NotNull Builder builder) {
        super(task, builder);
    }

    @Override
    public void allocateData(@NotNull Point region) {
        super.allocateData(region);
        //this.lightImage = new TileImage("light", getWorld(), region);
    }

    @Override
    public void saveData(@NotNull Point region) {
        super.saveData(region);
        //this.lightImage.saveToDisk();
    }

    @Override
    public void scanBlock(@NotNull Region region, @NotNull Chunk chunk, Chunk.@NotNull BlockData data, int blockX, int blockZ) {
        int pixelColor = basicPixelColor(region, data, blockX, blockZ);
        getTileImage().setPixel(blockX, blockZ, pixelColor);

        // get light level right above this block
        //int lightPixel = calculateLight(chunk, data.getFluidState(), blockX, data.getBlockY(), blockZ, data.getFluidY(), pixelColor);
        //this.lightImage.setPixel(blockX, blockZ, lightPixel);
    }
}
