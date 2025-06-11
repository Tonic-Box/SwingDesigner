package designer.model;

import java.awt.*;

public class SizeData {
    public SizeData() {}
    public SizeData(Dimension dimension) {
        this.width = dimension.width;
        this.height = dimension.height;
    }
    public int width, height;

    public Dimension toDimension() {
        return new Dimension(width, height);
    }
}