package designer;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ComponentHierarchyPanel extends JPanel
        implements DesignSurfacePanel.DesignChangeListener,
        DesignSurfacePanel.SelectionListener
{
    private final DesignSurfacePanel surface;
    private final JTree tree;
    private final DefaultTreeModel model;

    /** Prevents tree→surface→tree loops */
    private boolean updatingFromSurface = false;
    private final ComponentTreeCellRenderer renderer;

    public ComponentHierarchyPanel(DesignSurfacePanel surface) {
        super(new BorderLayout());
        this.surface = surface;

        // build model & tree
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(surface);
        model = new DefaultTreeModel(root);
        tree  = new JTree(model);
        tree.setRootVisible(true);

        renderer = new ComponentTreeCellRenderer();
        tree.setCellRenderer(renderer);

        // 1) Tree → Surface
        tree.getSelectionModel()
                .setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(e -> {
            // only skip if *we* are driving the change
            if (updatingFromSurface) return;

            DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
            if (node == null) return;

            Object uo = node.getUserObject();

            renderer.setSelectedObject(uo);
            tree.repaint();

            if (uo instanceof JComponent jc) {
                surface.setSelectedComponent(jc);
            } else {
                surface.clearSelection();
            }
        });

        // 2) Surface → Tree
        surface.addSelectionListener(c -> SwingUtilities.invokeLater(() -> {
            updatingFromSurface = true;

            renderer.setSelectedObject(c);

            if (!(c instanceof JComponent)) {
                tree.clearSelection();
            } else {
                TreePath p = findPath((JComponent)c);
                if (p != null) {
                    tree.setSelectionPath(p);
                    tree.scrollPathToVisible(p);
                }
            }
            updatingFromSurface = false;
        }));

        surface.addDesignChangeListener(this);
        add(new JScrollPane(tree), BorderLayout.CENTER);
        rebuildTree();
    }

    private void rebuildTree() {
        // 1) remember all the userObjects whose paths are currently expanded
        List<Object> expanded = new ArrayList<>();
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = tree.getPathForRow(row);
            if (tree.isExpanded(path)) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                expanded.add(node.getUserObject());
            }
        }

        // 2) rebuild the model from scratch
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(surface);
        buildNode(root, surface);
        model.setRoot(root);
        model.reload();

        // 3) re-expand each of the previously expanded userObjects
        for (Object uo : expanded) {
            TreePath p = findPath(uo);
            if (p != null) {
                tree.expandPath(p);
            }
        }
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

    @Override public void designChanged() {
        SwingUtilities.invokeLater(this::rebuildTree);
    }

    @Override public void selectionChanged(Component c) {
        // handled by the lambda above; no need to do anything here
    }

    private TreePath findPath(JComponent target) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();
        Enumeration<TreeNode> e = root.breadthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.nextElement();
            if (node.getUserObject() == target) {
                return new TreePath(node.getPath());
            }
        }
        return null;
    }

    private TreePath findPath(Object userObject) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();
        Enumeration<TreeNode> e = root.breadthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.nextElement();
            if (node.getUserObject() == userObject) {
                return new TreePath(node.getPath());
            }
        }
        return null;
    }
}
