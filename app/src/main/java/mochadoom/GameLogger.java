package mochadoom;

import java.text.MessageFormat;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Minimal logger that replaces java.util.logging.Logger inside the engine.
 *
 * Android's Logger does not have the Supplier-based overloads (fine(Supplier), log(Level, Supplier)...)
 * the original engine uses, which made the game crash at start-up with NoSuchMethodError. This class
 * offers the overloads the engine needs and writes WARNING and above to System.err (which Android
 * forwards to Logcat); lower levels are dropped without even evaluating their message Suppliers.
 */
public final class GameLogger {
    private final String name;

    GameLogger(final String name) {
        this.name = name;
    }

    private static boolean enabled(final Level level) {
        return level.intValue() >= Level.WARNING.intValue();
    }

    private void print(final Level level, final String message, final Throwable thrown) {
        System.err.println("[" + level.getName() + "] " + name + ": " + message);
        if (thrown != null) {
            thrown.printStackTrace();
        }
    }

    public void log(final Level level, final String msg) {
        if (enabled(level)) {
            print(level, msg, null);
        }
    }

    public void log(final Level level, final String msg, final Object param) {
        if (enabled(level)) {
            print(level, format(msg, param), null);
        }
    }

    public void log(final Level level, final String msg, final Object[] params) {
        if (enabled(level)) {
            print(level, format(msg, params), null);
        }
    }

    public void log(final Level level, final String msg, final Throwable thrown) {
        if (enabled(level)) {
            print(level, msg == null ? "" : msg, thrown);
        }
    }

    public void log(final Level level, final Supplier<String> msg) {
        if (enabled(level)) {
            print(level, msg.get(), null);
        }
    }

    public void log(final Level level, final Throwable thrown, final Supplier<String> msg) {
        if (enabled(level)) {
            print(level, msg.get(), thrown);
        }
    }

    public void fine(final String msg) {
        log(Level.FINE, msg);
    }

    public void fine(final Supplier<String> msg) {
        log(Level.FINE, msg);
    }

    public void finer(final String msg) {
        log(Level.FINER, msg);
    }

    public void finer(final Supplier<String> msg) {
        log(Level.FINER, msg);
    }

    private static String format(final String pattern, final Object... args) {
        try {
            return MessageFormat.format(pattern, args);
        } catch (final IllegalArgumentException e) {
            return pattern;
        }
    }
}
