package designer.types;

import java.awt.*;

public final class Layouts {
    public static final String[] NAMES = { "Absolute", "FlowLayout", "BorderLayout", "GridLayout" };

    public static LayoutManager fromName(String n){
        return switch (n) {
            case "FlowLayout"   -> new FlowLayout();
            case "BorderLayout" -> new BorderLayout();
            case "GridLayout"   -> new GridLayout();
            default             -> null;
        };
    }
    public static String toName(LayoutManager lm){
        if(lm==null)                return "Absolute";
        return switch (lm.getClass().getSimpleName()){
            case "FlowLayout"   -> "FlowLayout";
            case "BorderLayout" -> "BorderLayout";
            case "GridLayout"   -> "GridLayout";
            default             -> lm.getClass().getSimpleName();
        };
    }
}
