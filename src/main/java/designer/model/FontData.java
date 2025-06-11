package designer.model;

import java.awt.*;

public class FontData {
    public FontData() {}
    public FontData(Font font) {
        this.name = font.getName();
        this.style = font.getStyle();
        this.size = font.getSize();
    }
    public String name;
    public int    style;
    public int    size;

    public Font toFont() {
        return new Font(name, style, size);
    }
}