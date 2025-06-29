package designer.util;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Holds all named popup-menus for the designer */
public class PopupMenuManager {
    // preserve insertion order
    private static final Map<String, JPopupMenu> MENUS = new LinkedHashMap<>();

    /** Register a new or replaced menu under this name */
    public static void putMenu(String name, JPopupMenu menu) {
        MENUS.put(name, menu);
    }

    /** Remove a menu by name */
    public static void removeMenu(String name) {
        MENUS.remove(name);
    }

    /** Names of all menus, in creation order */
    public static Set<String> getMenuNames() {
        return MENUS.keySet();
    }

    /** Retrieve the menu by its name (or null) */
    public static JPopupMenu getMenu(String name) {
        return MENUS.get(name);
    }

    public static void clearAll() {
        MENUS.clear();
    }

    public static String menuNameOf(JPopupMenu menu) {
        for (Map.Entry<String, JPopupMenu> entry : MENUS.entrySet()) {
            if (entry.getValue() == menu) {
                return entry.getKey();
            }
        }
        return null;
    }
}
