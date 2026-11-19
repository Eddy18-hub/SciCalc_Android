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

package mochadoom;

import doom.CVarManager;
import doom.ConfigManager;
import doom.DoomMain;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;

/**
 * Mocha Doom engine entry point, adapted for Android.
 *
 * The original opened an AWT window and registered mouse/keyboard listeners here. This version has
 * no window: whoever embeds the engine (the Android app) provides a {@link FrameSink} that receives
 * every finished frame, and sends input with {@link #postKey(g.Signals.ScanCode, boolean)}.
 */
public class Engine {
    private static volatile Engine instance;

    /** Receives each finished frame (called from the game thread). */
    public interface FrameSink {
        /** @param engine the running engine, to read pixels with {@link Engine#writeFrame(int[])}. */
        void onFrame(Engine engine);
    }

    private static volatile FrameSink frameSink;

    /**
     * Creates the engine (loads the WAD, initialises every subsystem) but does not start the game loop.
     * Command-line style arguments are accepted, e.g. {@code "-iwad", "/path/freedoom1.wad"}.
     */
    public static synchronized Engine create(final FrameSink sink, final String... argv) throws IOException {
        if (instance != null) {
            throw new IllegalStateException("The engine can only be created once per process");
        }
        frameSink = sink;
        return new Engine(argv);
    }

    public final CVarManager cvm;
    public final ConfigManager cm;
    public final DoomMain<?, ?> DOOM;

    private Engine(final String... argv) throws IOException {
        instance = this;

        // reads command line arguments
        this.cvm = new CVarManager(Arrays.asList(argv));

        // reads default.cfg and mochadoom.cfg (if present)
        this.cm = new ConfigManager();

        // initializes everything else (WAD, renderer, menu...)
        this.DOOM = new DoomMain<>();
    }

    /** Runs the game loop. Does not return until the game quits ({@link Exit.Quit}) or fails. */
    public void run() throws IOException {
        DOOM.setupLoop();
    }

    private static final Object PAUSE_LOCK = new Object();
    private static boolean paused;
    private static boolean stopRequested;

    /**
     * Pauses or resumes the game. The game thread blocks at the next page flip while paused, so a paused
     * game uses no CPU. Safe to call from any thread.
     */
    public static void setPaused(final boolean value) {
        synchronized (PAUSE_LOCK) {
            paused = value;
            PAUSE_LOCK.notifyAll();
        }
    }

    /** Asks the game loop to end at the next page flip (it then exits with {@link Exit.Quit}). */
    public static void requestStop() {
        synchronized (PAUSE_LOCK) {
            stopRequested = true;
            PAUSE_LOCK.notifyAll();
        }
    }

    /** Called by the game loop each time a frame is complete (page flip). */
    public static void updateFrame() {
        final FrameSink sink = frameSink;
        if (sink != null && instance != null) {
            sink.onFrame(instance);
        }
        synchronized (PAUSE_LOCK) {
            while (paused && !stopRequested) {
                try {
                    PAUSE_LOCK.wait();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    stopRequested = true;
                }
            }
            if (stopRequested) {
                Exit.now(0);
            }
        }
    }

    public int getScreenWidth() {
        return DOOM.graphicSystem.getScreenWidth();
    }

    public int getScreenHeight() {
        return DOOM.graphicSystem.getScreenHeight();
    }

    /** Writes the current frame as ARGB into [dst] (at least width*height ints). */
    public void writeFrame(final int[] dst) {
        DOOM.graphicSystem.writeArgb(dst);
    }

    /** Sends a key press or release to the game. */
    public void postKey(final g.Signals.ScanCode key, final boolean pressed) {
        DOOM.PostEvent(pressed ? key.doomEventDown : key.doomEventUp);
    }

    public static Engine getEngine() {
        final Engine local = Engine.instance;
        if (local == null) {
            // Some static initialisers (config, command-line flags) can run before create() finishes.
            throw new IllegalStateException("Engine not created yet");
        }
        return local;
    }

    public static CVarManager getCVM() {
        return getEngine().cvm;
    }

    public static ConfigManager getConfig() {
        return getEngine().cm;
    }
}
