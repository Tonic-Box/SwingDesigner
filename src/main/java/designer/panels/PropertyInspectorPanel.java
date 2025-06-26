package designer.panels;

import designer.editors.*;
import designer.misc.ColorCellRenderer;
import designer.misc.PopupMenuManager;
import designer.misc.PropertyTableModel;
import designer.types.PositionType;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyDescriptor;
import java.util.regex.Pattern;

public class PropertyInspectorPanel extends JPanel {
    private final PropertyTableModel model;
    private final DesignSurfacePanel designSurface;
    private final JComboBox<String> constraintCombo;
    private final JSpinner rowsSpinner, colsSpinner;
    private final JSpinner cellRowSpinner, cellColSpinner;
    private final JComboBox<PositionType> positionCombo;
    private final JPanel gbcEditorHolder;
    private final JScrollPane gbcScroll;
    private final JPanel south;
    private final JPanel positionPanel;
    private final JPanel constraintPanel;
    private final JPanel gridConfigPanel;
    private final JPanel cellPosPanel;
    private final JPanel wrapper;

    public PropertyInspectorPanel(DesignSurfacePanel ds) {
        super(new BorderLayout());
        this.designSurface = ds;
        // Listen for selection changes
        this.designSurface.addSelectionListener(this::setTarget);

        this.model = new PropertyTableModel(ds::externalPropertyChanged);

        // ─── NORTH: header + search bar ────────────────────────────
        JPanel north = new JPanel(new BorderLayout(5,0));
        north.setBorder(new EmptyBorder(5,5,5,5));
        north.add(new JLabel("Property Inspector"), BorderLayout.NORTH);

        JPanel searchPanel = new JPanel(new BorderLayout(5,0));
        JTextField searchField = new JTextField();
        searchPanel.add(new JLabel("Filter: "), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);

        north.add(searchPanel, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        // ─── property table ─────────────────────────────────────────
        JTable table = new JTable(model) {
            private final ColorCellRenderer colorRend   = new ColorCellRenderer();
            private final ColorCellEditor colorEdit     = new ColorCellEditor();
            private final FontCellRenderer  fontRend    = new FontCellRenderer();
            private final FontCellEditor    fontEdit    = new FontCellEditor();
            private final BorderCellRenderer borderRend  = new BorderCellRenderer();
            private final BorderCellEditor  borderEdit  = new BorderCellEditor();
            private final LayoutCellEditor layoutEdit   = new LayoutCellEditor();
            private final BooleanCellEditor boolEdit    = new BooleanCellEditor();
            private final DimensionCellRenderer dimRend = new DimensionCellRenderer();
            private final DimensionCellEditor dimEdit   = new DimensionCellEditor();

            @Override
            public TableCellRenderer getCellRenderer(int viewRow, int col) {
                if (col == 1) {
                    int modelRow = convertRowIndexToModel(viewRow);
                    PropertyDescriptor pd = model.getPropertyDescriptor(modelRow);
                    if (pd != null) {
                        Class<?> t = pd.getPropertyType();
                        if (t == JPopupMenu.class)            return new PopupMenuCellRenderer();
                        if (t == Dimension.class)            return dimRend;
                        if (t == Color.class)                return colorRend;
                        if (t == Font.class)                 return fontRend;
                        if (Border.class.isAssignableFrom(t))return borderRend;
                    }
                }
                return super.getCellRenderer(viewRow, col);
            }

            @Override
            public TableCellEditor getCellEditor(int viewRow, int col) {
                if (col == 1) {
                    int modelRow = convertRowIndexToModel(viewRow);
                    PropertyDescriptor pd = model.getPropertyDescriptor(modelRow);
                    if (pd != null) {
                        Class<?> t = pd.getPropertyType();
                        if (t == JPopupMenu.class)                return new PopupMenuCellEditor();
                        if (t == Dimension.class)                return dimEdit;
                        if (t == Color.class)                    return colorEdit;
                        if (t == Font.class)                     return fontEdit;
                        if (Border.class.isAssignableFrom(t))     return borderEdit;
                        if (LayoutManager.class.isAssignableFrom(t)) return layoutEdit;
                        if (t == Boolean.class || t == boolean.class) return boolEdit;
                    }
                }
                return super.getCellEditor(viewRow, col);
            }
        };
        add(new JScrollPane(table), BorderLayout.CENTER);

        TableRowSorter<PropertyTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                String txt = searchField.getText().trim();
                if (txt.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(txt), 0));
                }
            }
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e){ update(); }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);

        // ─── constraint chooser (BorderLayout) ───────────────────────
        constraintPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        constraintPanel.add(new JLabel("Constraint:"));
        constraintCombo = new JComboBox<>(new String[]{"Center","North","South","East","West"});
        constraintCombo.addActionListener(e -> {
            Component tgt = model.getTarget();
            if (tgt instanceof JComponent jc) {
                jc.putClientProperty("layoutConstraint", constraintCombo.getSelectedItem());
                designSurface.externalPropertyChanged();
            }
        });
        constraintPanel.add(constraintCombo);

        // ─── GridLayout config ────────────────────────────────────────
        gridConfigPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        gridConfigPanel.add(new JLabel("Rows:"));
        rowsSpinner = new JSpinner(new SpinnerNumberModel(1,0,100,1));
        gridConfigPanel.add(rowsSpinner);
        gridConfigPanel.add(new JLabel("Cols:"));
        colsSpinner = new JSpinner(new SpinnerNumberModel(1,0,100,1));
        gridConfigPanel.add(colsSpinner);
        ChangeListener gridChange = e -> {
            Component tgt = model.getTarget();
            if (tgt instanceof Container cont && cont.getLayout() instanceof GridLayout gl) {
                gl.setRows((Integer)rowsSpinner.getValue());
                gl.setColumns((Integer)colsSpinner.getValue());
                designSurface.externalPropertyChanged();
            }
        };
        rowsSpinner.addChangeListener(gridChange);
        colsSpinner.addChangeListener(gridChange);

        // ─── Cell position (parent GridLayout) ────────────────────────
        cellPosPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        cellPosPanel.add(new JLabel("Cell Row:"));
        cellRowSpinner = new JSpinner(new SpinnerNumberModel(0,0,0,1));
        cellPosPanel.add(cellRowSpinner);
        cellPosPanel.add(new JLabel("Cell Col:"));
        cellColSpinner = new JSpinner(new SpinnerNumberModel(0,0,0,1));
        cellPosPanel.add(cellColSpinner);
        ChangeListener cellChange = e -> {
            Component tgt = model.getTarget();
            if (!(tgt instanceof JComponent jc)) return;
            Container parent = jc.getParent();
            if (parent == null || !(parent.getLayout() instanceof GridLayout gl)) return;
            int total = parent.getComponentCount();
            int rows  = gl.getRows(), cols = gl.getColumns();
            if (rows==0 && cols>0) rows = (total + cols - 1)/cols;
            if (cols==0 && rows>0) cols = (total + rows - 1)/rows;
            int r = (Integer)cellRowSpinner.getValue();
            int c = (Integer)cellColSpinner.getValue();
            r = Math.min(Math.max(0,r), rows-1);
            c = Math.min(Math.max(0,c), cols-1);
            parent.remove(jc);
            parent.add(jc, r*cols + c);
            designSurface.externalPropertyChanged();
        };
        cellRowSpinner.addChangeListener(cellChange);
        cellColSpinner.addChangeListener(cellChange);

        // ─── assemble south stack ─────────────────────────────────────
        south = new JPanel(new CardLayout());
        //south.setLayout(new BoxLayout(south,BoxLayout.Y_AXIS));

        positionCombo = new JComboBox<>(PositionType.values());
        positionCombo.addActionListener(e -> {
            Component tgt = model.getTarget();
            if (tgt instanceof JComponent jc) {
                jc.putClientProperty("positionType", positionCombo.getSelectedItem());
                designSurface.externalPropertyChanged();
            }
        });

        positionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        positionPanel.add(new JLabel("Position Type:"));
        positionPanel.add(positionCombo);

        gbcEditorHolder = new JPanel(new BorderLayout());

        south.add(constraintPanel, "BORDER");
        south.add(cellPosPanel,    "GRID_CELL");
        south.add(positionPanel,   "ABSOLUTE");

        gbcScroll = new JScrollPane(
                gbcEditorHolder,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        gbcScroll.setPreferredSize(new Dimension(0, 50));
        gbcScroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Component view = gbcScroll.getViewport().getView();
                if (view != null) {
                    Dimension pref = view.getPreferredSize();
                    pref.width = gbcScroll.getViewport().getWidth();
                    pref.height = "GRIDBAG".equals(getVisibleSouthCard()) ? Math.max(150, pref.height) : 50;
                    view.setPreferredSize(pref);
                    gbcEditorHolder.revalidate();
                }
            }
        });

        south.add(gbcScroll, "GRIDBAG");

        JPanel southWrapper = new JPanel(new BorderLayout());
        gridConfigPanel.setVisible(false);
        southWrapper.add(gridConfigPanel, BorderLayout.NORTH);
        southWrapper.add(south,       BorderLayout.CENTER);
        add(southWrapper, BorderLayout.SOUTH);
        this.wrapper   = southWrapper;
    }

    private String getVisibleSouthCard() {
        for (Component c : south.getComponents()) {
            if (c.isVisible()) {
                // if you need the name, keep a parallel array/dictionary
                // mapping index→cardName, or compare against your known panels:
                if (c == constraintPanel) return "BORDER";
                if (c == cellPosPanel)    return "GRID_CELL";
                if (c == positionPanel)   return "ABSOLUTE";
                if (c == gbcScroll)       return "GRIDBAG";
            }
        }
        return null;
    }

    private void buildSouthPanel(LayoutManager layout, JComponent jc) {
        gbcEditorHolder.removeAll();
        CardLayout cards = (CardLayout)south.getLayout();
        boolean showGridCfg = jc.getLayout() instanceof GridLayout;
        gridConfigPanel.setVisible(showGridCfg);

        gbcScroll.setPreferredSize(new Dimension(0, 0));

        if (layout instanceof BorderLayout) {
            cards.show(south, "BORDER");
        }
        else if (layout instanceof GridLayout) {
            cards.show(south, "GRID_CELL");
        }
        else if (layout instanceof GridBagLayout) {
            GridBagLayout gbl = (GridBagLayout) layout;
            GridBagConstraints gbc = gbl.getConstraints(jc);
            GridBagConstraintsEditor editor = new GridBagConstraintsEditor();
            editor.load(gbc);
            editor.setListener(newGbc -> {
                gbl.setConstraints(jc, newGbc);
                designSurface.externalPropertyChanged();
            });
            gbcEditorHolder.add(new JLabel("GridBag Constraints:"), BorderLayout.NORTH);
            gbcEditorHolder.add(new JScrollPane(editor), BorderLayout.CENTER);
            cards.show(south, "GRIDBAG");
            gbcScroll.setPreferredSize(new Dimension(0, 200));
        }
        else if (layout == null) {
            cards.show(south, "ABSOLUTE");
        }
        gbcEditorHolder.revalidate();
        gbcEditorHolder.repaint();
        south.revalidate();
        south.repaint();
        revalidate();
        repaint();
    }

    private void createLabel(JComponent comp, String text) {
        JLabel header = new JLabel(text);
        comp.add(header, BorderLayout.NORTH);
    }

    /**
     * Refresh all controls when selection changes.
     */
    public void setTarget(Component c) {
        model.setTarget(c);
        // clear any previous GridBag editor
        gbcEditorHolder.removeAll();

        if (!(c instanceof JComponent jc)) {
            revalidate(); repaint();
            return;
        }

        Container parent = jc.getParent();

        // 1) BorderLayout constraint
        if (parent.getLayout() instanceof BorderLayout) {
            Object stored = jc.getClientProperty("layoutConstraint");
            constraintCombo.setSelectedItem(stored != null ? stored : "Center");
        }

        // 2) GridLayout on this component
        if (jc.getLayout() instanceof GridLayout glc) {
            rowsSpinner.setValue(glc.getRows());
            colsSpinner.setValue(glc.getColumns());
        }

        // 3) Cell position in parent GridLayout
        if (parent.getLayout() instanceof GridLayout glp) {
            int total = parent.getComponentCount();
            int rows  = glp.getRows(), cols = glp.getColumns();
            if (rows==0 && cols>0) rows = (total + cols - 1)/cols;
            if (cols==0 && rows>0) cols = (total + rows - 1)/rows;
            int idx = parent.getComponentZOrder(jc);
            cellRowSpinner.setModel(new SpinnerNumberModel(idx/cols, 0, rows-1, 1));
            cellColSpinner.setModel(new SpinnerNumberModel(idx%cols, 0, cols-1, 1));
        }

        // 4) PositionType
        if (jc.getClientProperty("positionType") != null) {
            positionCombo.setSelectedItem(jc.getClientProperty("positionType"));
        } else {
            positionCombo.setSelectedItem(PositionType.ABSOLUTE);
        }

        // 5) GridBagLayout constraints editor
        buildSouthPanel(parent.getLayout(), jc);
    }

    // ─────────────────────────────────────────────────────────────────
    static class FontCellRenderer extends JLabel implements TableCellRenderer {
        FontCellRenderer() { setOpaque(true); }
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            Font f = (v instanceof Font) ? (Font)v : null;
            setText(f != null ? f.getFontName() + " " + f.getSize() : "null");
            setFont(f != null ? f : t.getFont()); return this;
        }
    }

    static class FontCellEditor extends AbstractCellEditor implements TableCellEditor {
        private Font current;
        @Override public Object getCellEditorValue() { return current; }
        @Override public Component getTableCellEditorComponent(JTable t, Object v, boolean sel, int r, int c) {
            current = (v instanceof Font) ? (Font)v : t.getFont();
            Font chosen = JFontChooser.showDialog(SwingUtilities.getWindowAncestor(t), "Choose Font", current);
            if (chosen != null) current = chosen;
            SwingUtilities.invokeLater(this::fireEditingStopped);
            return new JLabel(current.getFontName() + " " + current.getSize());
        }
    }

    static class BorderCellRenderer extends JLabel implements TableCellRenderer {
        BorderCellRenderer() { setOpaque(true); setText(" "); }
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            setBorder(v instanceof Border ? (Border)v : null); return this;
        }
    }

    static class BorderCellEditor extends AbstractCellEditor implements TableCellEditor {
        private Border current;
        @Override public Object getCellEditorValue() { return current; }
        @Override public Component getTableCellEditorComponent(JTable t, Object v, boolean sel, int r, int c) {
            current = (v instanceof Border) ? (Border)v : null;
            Border chosen = BorderChooser.showDialog(SwingUtilities.getWindowAncestor(t), "Choose Border", current);
            if (chosen != null) current = chosen;
            SwingUtilities.invokeLater(this::fireEditingStopped);
            JLabel preview = new JLabel(" "); preview.setBorder(current);
            return preview;
        }
    }

    /** Renders Dimensions as W: x, H: x */
    static class DimensionCellRenderer extends DefaultTableCellRenderer {
        @Override protected void setValue(Object value) {
            if (value instanceof Dimension d) {
                setText("W: " + d.width + ", H: " + d.height);
            } else super.setValue(value);
        }
    }

    /** Edits Dimensions with a popup dialog */
    static class DimensionCellEditor extends AbstractCellEditor implements TableCellEditor {
        private Dimension current;

        @Override public Object getCellEditorValue() {
            return current;
        }

        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            current = value instanceof Dimension ? (Dimension) value : new Dimension(0, 0);
            JSpinner wSpinner = new JSpinner(new SpinnerNumberModel(current.width, 0, Integer.MAX_VALUE, 1));
            JSpinner hSpinner = new JSpinner(new SpinnerNumberModel(current.height, 0, Integer.MAX_VALUE, 1));
            JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
            panel.add(new JLabel("Width:")); panel.add(wSpinner);
            panel.add(new JLabel("Height:")); panel.add(hSpinner);

            int result = JOptionPane.showConfirmDialog(
                    table,
                    panel,
                    "Edit Dimension",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );
            if (result == JOptionPane.OK_OPTION) {
                current = new Dimension((Integer) wSpinner.getValue(), (Integer) hSpinner.getValue());
            }
            // fire stop so setValueAt is invoked
            SwingUtilities.invokeLater(this::fireEditingStopped);
            // return a preview label
            return new JLabel(current != null ? "W: " + current.width + ", H: " + current.height : "");
        }
    }

    /** Renders the selected popup menu name in the cell */
    static class PopupMenuCellRenderer extends DefaultTableCellRenderer {
        @Override protected void setValue(Object value) {
            if (value instanceof JPopupMenu) {
                // find the name by identity
                String name = PopupMenuManager.getMenuNames().stream()
                        .filter(n -> PopupMenuManager.getMenu(n) == value)
                        .findFirst().orElse("<None>");
                setText(name);
            } else {
                setText("<None>");
            }
        }
    }

    static class PopupMenuCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JComboBox<String> combo = new JComboBox<>();

        PopupMenuCellEditor() {
            reloadItems();
            combo.addActionListener(e -> {
                String sel = (String)combo.getSelectedItem();
                if ("<New>".equals(sel)) {
                    // user wants to create a new menu
                    PopupMenuEditorDialog.showDialog(null);
                    reloadItems();
                    combo.showPopup();
                } else {
                    // immediately commit any normal choice
                    fireEditingStopped();
                }
            });
        }

        private void reloadItems() {
            combo.removeAllItems();
            combo.addItem("<None>");
            PopupMenuManager.getMenuNames().forEach(combo::addItem);
            combo.addItem("<New>");
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int col) {
            // pre select the current menu
            String current = "<None>";
            if (value instanceof JPopupMenu) {
                for (String n : PopupMenuManager.getMenuNames()) {
                    if (PopupMenuManager.getMenu(n) == value) {
                        current = n;
                        break;
                    }
                }
            }
            combo.setSelectedItem(current);
            return combo;
        }

        @Override
        public Object getCellEditorValue() {
            String sel = (String)combo.getSelectedItem();
            if ("<None>".equals(sel)) return null;
            return PopupMenuManager.getMenu(sel);
        }
    }
}