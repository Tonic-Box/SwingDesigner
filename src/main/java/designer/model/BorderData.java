package designer.model;

import javax.swing.border.*;
import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BorderData {
    public static BorderData fromBorder(Border border) {
        if (border == null) return null;
        BorderData data = new BorderData();

        if (border instanceof EmptyBorder eb) {
            data.type = "EmptyBorder";
            Insets in = eb.getBorderInsets();
            Map<String,Object> props = new HashMap<>();
            props.put("top",    in.top);
            props.put("left",   in.left);
            props.put("bottom", in.bottom);
            props.put("right",  in.right);
            data.properties = props;

        } else if (border instanceof LineBorder lb) {
            data.type = "LineBorder";
            Map<String,Object> props = new HashMap<>();
            props.put("thickness", lb.getThickness());
            props.put("colorRGB",   lb.getLineColor().getRGB());
            data.properties = props;

        } else if (border instanceof MatteBorder mb) {
            data.type = "MatteBorder";
            Insets in = mb.getBorderInsets();
            Map<String,Object> props = new HashMap<>();
            props.put("top",    in.top);
            props.put("left",   in.left);
            props.put("bottom", in.bottom);
            props.put("right",  in.right);
            Color c = mb.getMatteColor();
            props.put("colorRGB", c != null ? c.getRGB() : null);
            data.properties = props;

        } else if (border instanceof TitledBorder tb) {
            data.type = "TitledBorder";
            Map<String,Object> props = new HashMap<>();
            props.put("title",             tb.getTitle());
            props.put("titlePosition",     tb.getTitlePosition());
            props.put("titleJustification",tb.getTitleJustification());
            Color tc = tb.getTitleColor();
            props.put("colorRGB",          tc != null ? tc.getRGB() : null);
            Font f = tb.getTitleFont();
            if (f != null) {
                props.put("fontName",  f.getFontName());
                props.put("fontStyle", f.getStyle());
                props.put("fontSize",  f.getSize());
            }
            data.properties = props;

        } else if (border instanceof EtchedBorder eb2) {
            data.type = "EtchedBorder";
            Map<String,Object> props = new HashMap<>();
            props.put("etchType", eb2.getEtchType());
            props.put("highlightRGB", eb2.getHighlightColor().getRGB());
            props.put("shadowRGB",    eb2.getShadowColor().getRGB());
            data.properties = props;

        } else if (border instanceof CompoundBorder cb) {
            data.type = "CompoundBorder";
            Map<String,Object> props = new HashMap<>();
            props.put("outside", fromBorder(cb.getOutsideBorder()));
            props.put("inside",  fromBorder(cb.getInsideBorder()));
            data.properties = props;

        } else {
            // fallback for other border types
            data.type       = border.getClass().getSimpleName();
            data.properties = Collections.emptyMap();
        }

        return data;
    }
    public String type;
    public Map<String, Object> properties;

    public Border toBorder() {
        switch (type) {
            case "EmptyBorder" -> {
                int top    = getInt("top");
                int left   = getInt("left");
                int bottom = getInt("bottom");
                int right  = getInt("right");
                return new EmptyBorder(top, left, bottom, right);
            }
            case "LineBorder" -> {
                int thickness = getInt("thickness");
                Color color   = new Color(getInt("colorRGB"), true);
                return new LineBorder(color, thickness);
            }
            case "MatteBorder" -> {
                int top    = getInt("top");
                int left   = getInt("left");
                int bottom = getInt("bottom");
                int right  = getInt("right");
                Color color = new Color(getInt("colorRGB"), true);
                return new MatteBorder(top, left, bottom, right, color);
            }
            case "TitledBorder" -> {
                String title = (String)properties.get("title");
                TitledBorder tb = new TitledBorder(title);
                tb.setTitlePosition(getInt("titlePosition"));
                tb.setTitleJustification(getInt("titleJustification"));
                tb.setTitleColor(new Color(getInt("colorRGB"), true));
                String fontName = (String)properties.get("fontName");
                int fontStyle   = getInt("fontStyle");
                int fontSize    = getInt("fontSize");
                tb.setTitleFont(new Font(fontName, fontStyle, fontSize));
                return tb;
            }
            case "EtchedBorder" -> {
                int etchType     = getInt("etchType");
                Color hiColor    = new Color(getInt("highlightRGB"), true);
                Color shadowColor= new Color(getInt("shadowRGB"), true);
                return new EtchedBorder(etchType, hiColor, shadowColor);
            }
            case "CompoundBorder" -> {
                Object out = properties.get("outside");
                Object in  = properties.get("inside");
                Border outside = (out instanceof BorderData bdOut) ? bdOut.toBorder() : null;
                Border inside  = (in  instanceof BorderData bdIn)  ? bdIn.toBorder()  : null;
                return new CompoundBorder(outside, inside);
            }
            default -> {
                // unknown type → no border
                return null;
            }
        }
    }

    /** Helper to pull an int (or 0) out of the properties map. */
    private int getInt(String key) {
        Object v = properties.get(key);
        return (v instanceof Number n) ? n.intValue() : 0;
    }
}