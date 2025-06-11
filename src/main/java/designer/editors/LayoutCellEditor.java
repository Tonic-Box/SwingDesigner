package designer.editors;

import designer.types.Layouts;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

public class LayoutCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final JComboBox<String> combo = new JComboBox<>(Layouts.NAMES);

    public LayoutCellEditor() {
        combo.addActionListener(e -> fireEditingStopped());
    }

    @Override public Object getCellEditorValue() {
        return Layouts.fromName((String) combo.getSelectedItem());
    }
    @Override public Component getTableCellEditorComponent(
            JTable t, Object v, boolean sel, int r, int c) {
        String name = (v instanceof LayoutManager)
                ? Layouts.toName((LayoutManager) v)
                : String.valueOf(v);
        combo.setSelectedItem(name);
        return combo;
    }
}