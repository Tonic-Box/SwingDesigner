package designer.ui.editors;

import designer.util.PopupMenuManager;

import javax.swing.*;
import java.awt.*;

/**
 * A simple modal dialog to create/edit named popup-menus.
 * On OK it writes back into PopupMenuManager.
 */
public class PopupMenuEditorDialog extends JDialog {
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> menuList       = new JList<>(listModel);
    private final DefaultListModel<JMenuItem> itemModel = new DefaultListModel<>();
    private final JList<JMenuItem> itemList    = new JList<>(itemModel);

    private PopupMenuEditorDialog(Window owner) {
        super(owner, "Edit Popup Menus", ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(5,5));
        setSize(600, 400);
        setLocationRelativeTo(owner);

        // left: list of menus + add/remove
        JPanel left = new JPanel(new BorderLayout(3,3));
        left.add(new JLabel("Menus"), BorderLayout.NORTH);
        menuList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        left.add(new JScrollPane(menuList), BorderLayout.CENTER);

        JPanel leftButtons = new JPanel(new GridLayout(1,2,5,5));
        JButton addMenu = new JButton("New");
        JButton delMenu = new JButton("Delete");
        leftButtons.add(addMenu);
        leftButtons.add(delMenu);
        left.add(leftButtons, BorderLayout.SOUTH);

        // right: items of selected menu + add/remove
        JPanel right = new JPanel(new BorderLayout(3,3));
        right.add(new JLabel("Items"), BorderLayout.NORTH);
        itemList.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(
                    JList<?> list, Object value, int idx, boolean sel, boolean foc) {
                JMenuItem mi = (JMenuItem) value;
                return super.getListCellRendererComponent(list, mi.getText(), idx, sel, foc);
            }
        });
        right.add(new JScrollPane(itemList), BorderLayout.CENTER);

        JPanel itemButtons = new JPanel(new GridLayout(1,2,5,5));
        JButton addItem = new JButton("Add");
        JButton delItem = new JButton("Remove");
        itemButtons.add(addItem);
        itemButtons.add(delItem);
        right.add(itemButtons, BorderLayout.SOUTH);

        // bottom: OK / Cancel
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok   = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        south.add(cancel);
        south.add(ok);

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        // initialize menuList from manager
        PopupMenuManager.getMenuNames().forEach(listModel::addElement);

        // when a menu is selected, show its items
        menuList.addListSelectionListener(e -> {
            itemModel.clear();
            String sel = menuList.getSelectedValue();
            if (sel != null) {
                for (Component c : PopupMenuManager.getMenu(sel).getComponents()) {
                    if (c instanceof JMenuItem mi) itemModel.addElement(mi);
                }
            }
        });

        // add new menu
        addMenu.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Menu name:");
            if (name != null && !name.isBlank() && !listModel.contains(name)) {
                listModel.addElement(name);
                // register empty menu
                PopupMenuManager.putMenu(name, new JPopupMenu());
            }
        });

        // delete menu
        delMenu.addActionListener(e -> {
            String sel = menuList.getSelectedValue();
            if (sel != null) {
                int idx = menuList.getSelectedIndex();
                PopupMenuManager.removeMenu(sel);
                listModel.remove(idx);
                itemModel.clear();
            }
        });

        // add item to selected menu
        addItem.addActionListener(e -> {
            String selMenu = menuList.getSelectedValue();
            if (selMenu == null) return;
            String label = JOptionPane.showInputDialog(this, "Item label:");
            if (label != null && !label.isBlank()) {
                JMenuItem mi = new JMenuItem(label);
                PopupMenuManager.getMenu(selMenu).add(mi);
                itemModel.addElement(mi);
            }
        });

        // remove item
        delItem.addActionListener(e -> {
            JMenuItem mi = itemList.getSelectedValue();
            String selMenu = menuList.getSelectedValue();
            if (mi != null && selMenu != null) {
                PopupMenuManager.getMenu(selMenu).remove(mi);
                itemModel.removeElement(mi);
            }
        });

        // cancel
        cancel.addActionListener(e -> dispose());
        // OK
        ok.addActionListener(e -> dispose());
    }

    /** Show the dialog; blocks until closed */
    public static void showDialog(Window owner) {
        PopupMenuEditorDialog dlg = new PopupMenuEditorDialog(owner);
        dlg.setVisible(true);
    }
}
