package mochadoom;

import java.io.File;

/**
 * Where the engine reads and writes its own files (configuration, savegames, translucency map).
 *
 * On a PC the current directory is writable, so the original code used bare relative names. On Android
 * the current directory is read-only, so every such name goes through {@link #path(String)}, which
 * prefixes the app's private folder set with {@link #setDir(File)}.
 */
public final class Storage {
    private static volatile String dir = "";

    private Storage() {}

    /** Sets the writable folder (created if missing). Must be called before the engine is created. */
    public static void setDir(final File folder) {
        folder.mkdirs();
        dir = folder.getAbsolutePath() + File.separator;
    }

    /** Returns [name] inside the writable folder. */
    public static String path(final String name) {
        return dir + name;
    }
}
