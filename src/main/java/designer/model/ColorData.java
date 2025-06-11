package designer.model;

import java.awt.*;

public class ColorData {
    public ColorData() {}
    public ColorData(Color color) {
        this.r = color.getRed();
        this.g = color.getGreen();
        this.b = color.getBlue();
        this.a = color.getAlpha();
    }
    public int r, g, b, a;

    public Color toColor() {
        return new Color(r, g, b, a);
    }
}