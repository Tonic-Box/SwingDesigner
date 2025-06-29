package designer.model;

import java.util.List;

/**
 * One node in your component hierarchy.
 * Captures everything from bounds to colors, fonts, borders, layouts, popup-menus, etc.
 */
public class ComponentData {
    // identity
    public String className;
    public String name;
    public String text;
    public boolean visible;
    // geometry
    public RectangleData bounds;
    public SizeData     preferredSize;
    public SizeData     minimumSize;
    public SizeData     maximumSize;

    // styling
    public ColorData  backgroundColor;
    public ColorData  foregroundColor;
    public FontData   font;
    public BorderData border;

    // layout within its parent
    public LayoutData        layout;
    public String            layoutConstraint;
    public String            positionType;

    // popup/context-menu
    public String            popupMenuName;

    // children
    public List<ComponentData> children;
}
