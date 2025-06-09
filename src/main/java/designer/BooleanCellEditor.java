package designer;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

public class BooleanCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final JComboBox<String> combo = new JComboBox<>(new String[]{"false","true"});

    BooleanCellEditor() {
        combo.addActionListener(e -> fireEditingStopped());
    }

    @Override public Object getCellEditorValue() {
        return Boolean.parseBoolean((String) combo.getSelectedItem());
    }
    @Override public Component getTableCellEditorComponent(
            JTable t, Object v, boolean sel, int r, int c) {
        combo.setSelectedItem(String.valueOf(v));
        return combo;
    }
}
