package designer;

import java.awt.*;

public final class Layouts {
    static final String[] NAMES = { "Absolute", "FlowLayout", "BorderLayout", "GridLayout" };

    static LayoutManager fromName(String n){
        return switch (n){
            case "Absolute"     -> null;
            case "FlowLayout"   -> new FlowLayout();
            case "BorderLayout" -> new BorderLayout();
            case "GridLayout"   -> new GridLayout();
            default             -> null;
        };
    }
    static String toName(LayoutManager lm){
        if(lm==null)                return "Absolute";
        return switch (lm.getClass().getSimpleName()){
            case "FlowLayout"   -> "FlowLayout";
            case "BorderLayout" -> "BorderLayout";
            case "GridLayout"   -> "GridLayout";
            default             -> lm.getClass().getSimpleName();
        };
    }
}
