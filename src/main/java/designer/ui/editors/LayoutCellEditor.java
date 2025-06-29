package designer.ui.editors;

import designer.types.Layouts;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.Objects;

public class LayoutCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final JComboBox<String> combo = new JComboBox<>(Layouts.NAMES);

    public LayoutCellEditor() {
        combo.addActionListener(e -> fireEditingStopped());
    }

    @Override public Object getCellEditorValue() {
        return Layouts.fromName((String) Objects.requireNonNull(combo.getSelectedItem()));
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