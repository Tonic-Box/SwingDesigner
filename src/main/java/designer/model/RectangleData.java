package designer.model;

import java.awt.*;

public class RectangleData {
    public RectangleData(Rectangle rectangle)
    {
        this.x = rectangle.x;
        this.y = rectangle.y;
        this.width = rectangle.width;
        this.height = rectangle.height;
    }
    public RectangleData() {}

    public Rectangle toRectangle() {
        return new Rectangle(x, y, width, height);
    }
    public int x, y, width, height;
}