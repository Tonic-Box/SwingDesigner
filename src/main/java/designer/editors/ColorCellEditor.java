package designer.editors;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

public class ColorCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final JButton button = new JButton();
    private Color current = Color.WHITE;
    public ColorCellEditor(){
        button.setBorderPainted(false);
        button.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(button,"Choose colour", current);
            if(chosen!=null){ current=chosen; button.setBackground(current); }
            fireEditingStopped();
        });
    }
    @Override public Object getCellEditorValue(){ return current; }
    @Override public Component getTableCellEditorComponent(
            JTable t,Object v,boolean sel,int r,int c){
        current = (v instanceof Color)?(Color)v:Color.WHITE;
        button.setBackground(current);
        return button;
    }
}
