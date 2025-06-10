package designer;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.util.Enumeration;

public class ComponentHierarchyPanel extends JPanel
        implements DesignSurfacePanel.DesignChangeListener,
        DesignSurfacePanel.SelectionListener
{
    private final DesignSurfacePanel surface;
    private final JTree tree;
    private DefaultTreeModel model;

    public ComponentHierarchyPanel(DesignSurfacePanel surface) {
        super(new BorderLayout());
        this.surface = surface;

        // initial tree model
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(surface);
        model = new DefaultTreeModel(root);
        tree = new JTree(model);
        tree.setRootVisible(true);
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override public Component getTreeCellRendererComponent(
                    JTree t, Object value, boolean sel, boolean exp,
                    boolean leaf, int row, boolean hasFocus)
            {
                super.getTreeCellRendererComponent(t, value, sel, exp, leaf, row, hasFocus);
                Object uo = ((DefaultMutableTreeNode)value).getUserObject();
                if (uo instanceof JComponent jc) {
                    setText(jc.getName());
                } else {
                    setText("Design Surface");
                }
                return this;
            }
        });

        add(new JScrollPane(tree), BorderLayout.CENTER);

        // rebuild whenever the design changes
        surface.addDesignChangeListener(this);
        // highlight in tree whenever selection changes
        surface.addSelectionListener(this);

        // if user clicks in the tree, select that component on surface
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
            if (node == null) return;
            Object uo = node.getUserObject();
            if (uo instanceof JComponent jc) {
                surface.setSelectedComponent(jc);
            } else {
                surface.clearSelection();
            }
        });

        rebuildTree();
    }

    private void rebuildTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(surface);
        buildNode(root, surface);
        model.setRoot(root);
        model.reload();
        tree.expandRow(0);
    }

    private void buildNode(DefaultMutableTreeNode parent, Container cont) {
        for (Component c : cont.getComponents()) {
            if (c instanceof JComponent jc) {
                DefaultMutableTreeNode child = new DefaultMutableTreeNode(jc);
                parent.add(child);
                if (jc.getComponentCount() > 0) {
                    buildNode(child, jc);
                }
            }
        }
    }

    // → DesignChangeListener
    @Override public void designChanged() {
        SwingUtilities.invokeLater(this::rebuildTree);
    }

    // → SelectionListener
    @Override public void selectionChanged(Component c) {
        SwingUtilities.invokeLater(() -> {
            if (!(c instanceof JComponent)) {
                tree.clearSelection();
                return;
            }
            TreePath path = findPath((JComponent)c);
            if (path != null) tree.setSelectionPath(path);
        });
    }

    private TreePath findPath(JComponent target) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();
        Enumeration<TreeNode> e = root.breadthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.nextElement();
            if (node.getUserObject() == target)
                return new TreePath(node.getPath());
        }
        return null;
    }
}
