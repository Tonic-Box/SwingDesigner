package designer.misc;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class ComponentTreeCellRenderer extends DefaultTreeCellRenderer {
    private static final Color BG_SELECTED = new Color(0, 120, 215);
    private static final Color FG_SELECTED = Color.WHITE;

    /** the user object to paint in blue */
    private Object selectedObject;

    /** call this from your panel when selection changes */
    public void setSelectedObject(Object obj) {
        this.selectedObject = obj;
    }

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree, Object value,
            boolean sel, boolean exp,
            boolean leaf, int row,
            boolean hasFocus)
    {
        // pass sel=false so we disable the built-in blue highlight
        JLabel label = (JLabel) super.getTreeCellRendererComponent(
                tree, value, false, exp, leaf, row, hasFocus);

        // set your custom text
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object uo = node.getUserObject();
        if (uo instanceof JComponent jc && jc.getName() != null) {
            label.setText(jc.getName());
        } else {
            label.setText("Design Surface");
        }

        // if this is *your* selectedObject, paint it blue…
        if (uo == selectedObject) {
            label.setBackground(BG_SELECTED);
            label.setForeground(FG_SELECTED);
            label.setOpaque(true);
        } else {
            // …otherwise let the L&F draw the normal background
            label.setOpaque(false);
            // and leave its foreground alone (super already set it for sel=false)
        }

        return label;
    }
}
