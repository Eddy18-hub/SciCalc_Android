package mochadoom;

/**
 * Android port: the original engine calls System.exit() to quit (or on fatal errors), which would
 * kill the whole app. Instead it throws {@link Quit}, which the game thread catches to stop cleanly.
 * It extends Error, not Exception, so the engine's many {@code catch (Exception e)} blocks don't swallow it.
 */
public final class Exit {
    private Exit() {}

    public static final class Quit extends Error {
        public final int code;

        Quit(int code, String message) {
            super(message, null, false, false); // no stack trace: it is control flow, not a bug
            this.code = code;
        }

        /** True if the game ended normally (menu "quit"), false on a fatal engine error. */
        public boolean isNormal() {
            return code == 0;
        }
    }

    public static void now(int code) {
        throw new Quit(code, code == 0 ? "quit" : "exit code " + code);
    }

    public static void now(int code, String message) {
        throw new Quit(code, message);
    }
}
