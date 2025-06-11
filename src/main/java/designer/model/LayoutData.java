package designer.model;

import java.awt.*;

public class LayoutData {
    public String type;
    public Integer hgap, vgap;
    public Integer alignment;
    public Integer rows, cols;

    public static LayoutData fromLayout(LayoutManager lm) {
        LayoutData data = new LayoutData();

        if (lm == null) {
            data.type = "null";

        } else if (lm instanceof FlowLayout fl) {
            data.type      = "FlowLayout";
            data.hgap      = fl.getHgap();
            data.vgap      = fl.getVgap();
            data.alignment = fl.getAlignment();

        } else if (lm instanceof BorderLayout bl) {
            data.type = "BorderLayout";
            data.hgap = bl.getHgap();
            data.vgap = bl.getVgap();

        } else if (lm instanceof GridLayout gl) {
            data.type = "GridLayout";
            data.rows = gl.getRows();
            data.cols = gl.getColumns();
            data.hgap = gl.getHgap();
            data.vgap = gl.getVgap();

        } else {
            // fallback: record the layout’s class name, no other details
            data.type = lm.getClass().getSimpleName();
        }

        return data;
    }

    public LayoutManager toLayoutManager() {
        if (type == null || "null".equals(type)) {
            return null;
        }
        switch (type) {
            case "FlowLayout":
                // alignment default to CENTER, gaps default to 5
                int align = alignment != null ? alignment : FlowLayout.CENTER;
                int fh = hgap      != null ? hgap      : 5;
                int fv = vgap      != null ? vgap      : 5;
                return new FlowLayout(align, fh, fv);

            case "BorderLayout":
                // gaps default to 0
                int bh = hgap != null ? hgap : 0;
                int bv = vgap != null ? vgap : 0;
                return new BorderLayout(bh, bv);

            case "GridLayout":
                // rows/cols default to 1; gaps default to 0
                int gr = rows != null ? rows : 1;
                int gc = cols != null ? cols : 1;
                int gh = hgap  != null ? hgap  : 0;
                int gv = vgap  != null ? vgap  : 0;
                return new GridLayout(gr, gc, gh, gv);

            default:
                // unknown layout type
                return null;
        }
    }
}