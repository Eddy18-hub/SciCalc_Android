package compat;

/**
 * Sustituto mínimo de java.awt.Rectangle (Android no incluye AWT): solo lo que usa el motor.
 */
public class Rectangle {
    public int x;
    public int y;
    public int width;
    public int height;

    public Rectangle() {}

    public Rectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Rectangle(int width, int height) {
        this(0, 0, width, height);
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public String toString() {
        return "Rectangle[x=" + x + ",y=" + y + ",width=" + width + ",height=" + height + "]";
    }
}
