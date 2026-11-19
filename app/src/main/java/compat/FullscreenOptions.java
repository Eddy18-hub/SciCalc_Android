package compat;

/**
 * Android port: the original awt.FullscreenOptions drove AWT full-screen switching. Only the three
 * enums are kept, because m.Settings uses them as default values for (now unused) config entries.
 */
public interface FullscreenOptions {
    enum InterpolationMode { Nearest, Bilinear, Biqubic }

    enum StretchMode { Centre, Stretch, Fit, Aspect_4_3 }

    enum FullMode { Best, Native }
}
