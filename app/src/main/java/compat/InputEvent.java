package compat;

/** Máscaras de java.awt.event.InputEvent usadas por g.Signals. Mismos valores que AWT. */
public final class InputEvent {
    private InputEvent() {}

    public static final int ALT_DOWN_MASK = 512;
    public static final int CTRL_DOWN_MASK = 128;
    public static final int META_DOWN_MASK = 256;
    public static final int SHIFT_DOWN_MASK = 64;
}
