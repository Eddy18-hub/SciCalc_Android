package mochadoom;

import java.util.concurrent.ConcurrentHashMap;

/** Factory of the engine's loggers (see {@link GameLogger}). */
public final class Loggers {
    private static final ConcurrentHashMap<String, GameLogger> LOGGERS = new ConcurrentHashMap<>();

    public static GameLogger getLogger(final String className) {
        return LOGGERS.computeIfAbsent(className, GameLogger::new);
    }

    private Loggers() {}
}
