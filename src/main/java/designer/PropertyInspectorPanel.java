package designer;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.beans.PropertyDescriptor;

public class PropertyInspectorPanel extends JPanel {
    private final PropertyTableModel model;
    private final DesignSurfacePanel designSurface;

    private final JPanel    constraintPanel;
    private final JComboBox<String> constraintCombo;

    private final JPanel gridConfigPanel;
    private final JSpinner rowsSpinner, colsSpinner;

    private final JPanel cellPosPanel;
    private final JSpinner cellRowSpinner, cellColSpinner;
    private final JComboBox<PositionType> positionCombo;

    public PropertyInspectorPanel(DesignSurfacePanel ds) {
        super(new BorderLayout());
        this.designSurface = ds;
        this.model = new PropertyTableModel(ds::externalPropertyChanged);

        setBorder(new EmptyBorder(5,5,5,5));
        add(new JLabel("Property Inspector"), BorderLayout.NORTH);

        // ─── property table ─────────────────────────────────────────
        JTable table = new JTable(model) {
            private final ColorCellRenderer colorRend   = new ColorCellRenderer();
            private final ColorCellEditor   colorEdit   = new ColorCellEditor();
            private final FontCellRenderer  fontRend    = new FontCellRenderer();
            private final FontCellEditor    fontEdit    = new FontCellEditor();
            private final BorderCellRenderer borderRend  = new BorderCellRenderer();
            private final BorderCellEditor  borderEdit  = new BorderCellEditor();
            private final LayoutCellEditor  layoutEdit  = new LayoutCellEditor();
            private final BooleanCellEditor boolEdit    = new BooleanCellEditor();

            @Override
            public TableCellRenderer getCellRenderer(int row, int col) {
                if (col == 1) {
                    PropertyDescriptor pd = model.getPropertyDescriptor(row);
                    if (pd != null) {
                        Class<?> t = pd.getPropertyType();
                        if (t == Color.class)                 return colorRend;
                        if (t == Font.class)                  return fontRend;
                        if (t != null && Border.class.isAssignableFrom(t)) return borderRend;
                    }
                }
                return super.getCellRenderer(row, col);
            }

            @Override
            public TableCellEditor getCellEditor(int row, int col) {
                if (col == 1) {
                    PropertyDescriptor pd = model.getPropertyDescriptor(row);
                    if (pd != null) {
                        Class<?> t = pd.getPropertyType();
                        if (t == Color.class)                     return colorEdit;
                        if (t == Font.class)                      return fontEdit;
                        if (Border.class.isAssignableFrom(t))     return borderEdit;
                        if (LayoutManager.class.isAssignableFrom(t)) return layoutEdit;
                        if (t == Boolean.class || t == boolean.class) return boolEdit;
                    }
                }
                return super.getCellEditor(row, col);
            }
        };
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
        gridConfigPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,0));
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
        cellPosPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,0));
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
            if (!(parent.getLayout() instanceof GridLayout gl)) return;
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
        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south,BoxLayout.Y_AXIS));
        south.add(constraintPanel);
        south.add(gridConfigPanel);
        south.add(cellPosPanel);

        positionCombo = new JComboBox<>(PositionType.values());
        positionCombo.addActionListener(e -> {
            Component tgt = model.getTarget();
            if (tgt instanceof JComponent jc) {
                jc.putClientProperty("positionType", positionCombo.getSelectedItem());
                designSurface.externalPropertyChanged();
            }
        });

        // Add positioning control to your UI:
        JPanel positionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        positionPanel.add(new JLabel("Position Type:"));
        positionPanel.add(positionCombo);

        // Add positionPanel into inspector's UI stack:
        south.add(positionPanel);

        add(south, BorderLayout.SOUTH);
    }

    /**
     * Refresh all controls when selection changes.
     */
    public void setTarget(Component c) {
        model.setTarget(c);
        if (!(c instanceof JComponent jc)) return;

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

        if (jc.getClientProperty("positionType") != null) {
            positionCombo.setSelectedItem(jc.getClientProperty("positionType"));
        } else {
            positionCombo.setSelectedItem(PositionType.ABSOLUTE);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Inner classes for Font & Border editors/renderers
    static class FontCellRenderer extends JLabel implements TableCellRenderer {
        FontCellRenderer() { setOpaque(true); }
        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            Font f = (v instanceof Font) ? (Font)v : null;
            setText(f != null ? f.getFontName() + " " + f.getSize() : "null");
            setFont(f != null ? f : t.getFont());
            return this;
        }
    }

    static class FontCellEditor extends AbstractCellEditor implements TableCellEditor {
        private Font current;
        @Override public Object getCellEditorValue() {
            return current;
        }
        @Override public Component getTableCellEditorComponent(
                JTable t, Object v, boolean sel, int r, int c) {
            current = (v instanceof Font) ? (Font)v : t.getFont();
            Font chosen = JFontChooser.showDialog(
                    SwingUtilities.getWindowAncestor(t),
                    "Choose Font",
                    current
            );
            if (chosen != null) current = chosen;
            // immediately stop editing so value is committed
            SwingUtilities.invokeLater(this::fireEditingStopped);
            return new JLabel(current.getFontName() + " " + current.getSize());
        }
    }

    static class BorderCellRenderer extends JLabel implements TableCellRenderer {
        BorderCellRenderer() { setOpaque(true); setText(" "); }
        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            setBorder(v instanceof Border ? (Border)v : null);
            return this;
        }
    }

    static class BorderCellEditor extends AbstractCellEditor implements TableCellEditor {
        private Border current;
        @Override public Object getCellEditorValue() {
            return current;
        }
        @Override public Component getTableCellEditorComponent(
                JTable t, Object v, boolean sel, int r, int c) {
            current = (v instanceof Border) ? (Border)v : null;
            Border chosen = BorderChooser.showDialog(
                    SwingUtilities.getWindowAncestor(t),
                    "Choose Border",
                    current
            );
            if (chosen != null) current = chosen;
            SwingUtilities.invokeLater(this::fireEditingStopped);
            // show a small preview
            JLabel preview = new JLabel(" ");
            preview.setBorder(current);
            return preview;
        }
    }
}