package designer.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import designer.ui.*;

import javax.swing.*;

public class Static
{
    /**
     * Frames
     */
    public static DesignerFrame designerFrame;
    public static ComponentHierarchyPanel hierarchyPanel;
    public static CodeTabbedPane codeTabs;
    public static DesignSurfacePanel designSurface;
    public static PropertyInspectorPanel inspector;
    public static PreviewPanel preview;
    public static JTabbedPane leftTabs;
    public static JTabbedPane centerTabs;

    /**
     * Utils
     */
    public static final ObjectMapper mapper = new ObjectMapper();
}
