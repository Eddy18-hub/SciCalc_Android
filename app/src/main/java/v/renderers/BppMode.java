/*
 * Copyright (C) 2017 Good Sign
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package v.renderers;

import doom.CVarManager;
import doom.CommandVariable;
import doom.DoomMain;
import mochadoom.Engine;
import java.util.function.Function;
import m.Settings;
import rr.SceneRenderer;
import v.DoomGraphicSystem;

/**
 * This class helps to choose proper components for bit depth
 * selected in config or through use of command line arguments
 */
public enum BppMode {
    // Android port: only the 8-bit indexed renderer exists.
    Indexed(5, IndexedRenderer::new, BppMode::SceneGen_8);
    
    public final int lightBits;
    final RenderGen<?, ?> renderGen;
    final ScenerGen<?, ?> scenerGen;

    private <T, V> BppMode(int lightBits, RenderGen<T, V> renderGen, ScenerGen<T, V> scenerGen) {
        this.lightBits = lightBits;
        this.renderGen = renderGen;
        this.scenerGen = scenerGen;
    }
    
    @SuppressWarnings("unchecked")
	public <T, V> DoomGraphicSystem<T, V> graphics(RendererFactory.WithWadLoader<T, V> rf) {
        return ((RenderGen<T, V>) renderGen).apply(rf);
    }

    @SuppressWarnings("unchecked")
	public <T, V> SceneRenderer<T, V> sceneRenderer(DoomMain<T, V> DOOM) {
        return ((ScenerGen<T, V>) scenerGen).apply(DOOM);
    }

    public static BppMode chooseBppMode(CVarManager CVM) {
        return Indexed;
    }
    
    @SuppressWarnings("unchecked")
	private static <T, V> SceneRenderer<T, V> SceneGen_8(DoomMain<T, V> DOOM) {
        return (SceneRenderer<T, V>) SceneRendererMode.getMode().indexedGen.apply((DoomMain<byte[], byte[]>) DOOM);
    }
    
    interface ScenerGen<T, V> extends Function<DoomMain<T, V>, SceneRenderer<T, V>> {}
    interface RenderGen<T, V> extends Function<RendererFactory.WithWadLoader<T, V>, SoftwareGraphicsSystem<T, V>> {}
}
