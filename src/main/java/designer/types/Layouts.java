package designer.types;

import java.awt.*;

public final class Layouts {
    public static final String[] NAMES = { "Absolute", "FlowLayout", "BorderLayout", "GridLayout", "GridBagLayout" };

    public static LayoutManager fromName(String n){
        return switch (n) {
            case "FlowLayout"   -> new FlowLayout();
            case "BorderLayout" -> new BorderLayout();
            case "GridLayout"   -> new GridLayout();
            case "GridBagLayout"      -> new GridBagLayout();
            default             -> null;
        };
    }
    public static String toName(LayoutManager lm){
        if(lm==null)                return "Absolute";
        return switch (lm.getClass().getSimpleName()){
            case "FlowLayout"   -> "FlowLayout";
            case "BorderLayout" -> "BorderLayout";
            case "GridLayout"   -> "GridLayout";
            case "GridBagLayout"-> "GridBagLayout";
            default             -> lm.getClass().getSimpleName();
        };
    }
}
