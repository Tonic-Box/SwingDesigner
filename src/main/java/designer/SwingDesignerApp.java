package designer;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import java.awt.*;

public class SwingDesignerApp {
    /* --- colour palette --- */
    static final Color BG_DARK   = new Color(0x2B2B2B);
    static final Color BG_PANEL  = new Color(0x313335);
    static final Color FG_LIGHT  = new Color(0xE0E0E0);

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> {
            applyDarkDefaults();
            new DesignerFrame().setVisible(true);
        });
    }

    /* quickly override core UI defaults for dark mode */
    private static void applyDarkDefaults() {
        UIManager.put("Panel.background", BG_DARK);
        UIManager.put("Viewport.background", BG_DARK);
        UIManager.put("ScrollPane.background", BG_DARK);
        UIManager.put("Label.foreground", FG_LIGHT);
        UIManager.put("List.background", BG_PANEL);
        UIManager.put("List.foreground", FG_LIGHT);
        UIManager.put("Table.background", BG_PANEL);
        UIManager.put("Table.foreground", FG_LIGHT);
        UIManager.put("Table.gridColor", BG_DARK.darker());
        UIManager.put("TableHeader.background", BG_DARK);
        UIManager.put("TableHeader.foreground", FG_LIGHT);
        UIManager.put("SplitPane.background", BG_DARK);
        UIManager.put("TextArea.background", BG_PANEL);
        UIManager.put("TextArea.foreground", FG_LIGHT);
        UIManager.put("TextField.background", BG_PANEL);
        UIManager.put("TextField.foreground", FG_LIGHT);
    }
}