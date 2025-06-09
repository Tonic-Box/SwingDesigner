package designer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

public class ComponentPalettePanel extends JPanel {
    private static final Class<?>[] DEFAULT_COMPONENTS = new Class<?>[]{
            // Buttons & Toggles
            JButton.class, JToggleButton.class, JCheckBox.class, JRadioButton.class,
            // Labels & Separators
            JLabel.class, JSeparator.class, JToolTip.class,
            // Text components
            JTextField.class, JPasswordField.class, JFormattedTextField.class,
            JTextArea.class, JEditorPane.class, JTextPane.class,
            // Lists, tables, trees
            JList.class, JTable.class, JTree.class,
            // Spinners, sliders, progress
            JSpinner.class, JSlider.class, JProgressBar.class,
            // Scroll & split
            JScrollBar.class, JScrollPane.class, JSplitPane.class,
            // Tabs & toolbars
            JTabbedPane.class, JToolBar.class,
            // Menus & popups
            JMenuBar.class, JMenu.class, JMenuItem.class, JPopupMenu.class,
            // Layered & desktop
            JRootPane.class, JLayeredPane.class, JDesktopPane.class,
            // Generic containers
            JPanel.class
    };

    ComponentPalettePanel() {
        super(new BorderLayout());
        setBorder(new EmptyBorder(5,5,5,5));
        add(new JLabel("Component Palette"), BorderLayout.NORTH);

        DefaultListModel<Class<?>> model = new DefaultListModel<>();
        for (Class<?> c : DEFAULT_COMPONENTS)
            model.addElement(c);

        JList<Class<?>> list = new JList<>(model);
        list.setCellRenderer((l,v,i,s,f) -> new JLabel(v.getSimpleName()));

        list.setDragEnabled(true);
        list.setTransferHandler(new TransferHandler() {
            @Override protected Transferable createTransferable(JComponent c) {
                @SuppressWarnings("unchecked")
                Class<?> clazz = ((JList<Class<?>>)c).getSelectedValue();
                return clazz == null ? null : new StringSelection(clazz.getName());
            }
            @Override public int getSourceActions(JComponent c) { return COPY; }
        });

        add(new JScrollPane(list), BorderLayout.CENTER);
    }
}