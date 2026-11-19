package v.renderers;

/**
 * Android port: the only renderer. Keeps the classic 8-bit indexed screens and converts the
 * foreground screen to ARGB through the current palette/gamma table when a frame is requested.
 * (The original BufferedRenderer built java.awt BufferedImages, which Android does not have.)
 */
class IndexedRenderer extends SoftwareIndexedVideoRenderer {

    IndexedRenderer(RendererFactory.WithWadLoader<byte[], byte[]> rf) {
        super(rf);
    }

    @Override
    public void forcePalette() {
        // Nothing to rebuild: the ARGB table is chosen from usegamma/usepalette when a frame is written.
    }

    @Override
    public void writeArgb(int[] dst) {
        final byte[] fg = screens.get(DoomScreen.FG);
        final int[] table = cmaps[usegamma][usepalette];
        final int n = Math.min(fg.length, dst.length);
        for (int i = 0; i < n; i++) {
            dst[i] = table[fg[i] & 0xFF];
        }
    }
}
