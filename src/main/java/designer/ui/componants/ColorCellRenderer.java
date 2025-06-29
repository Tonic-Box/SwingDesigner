package designer.ui.componants;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class ColorCellRenderer extends JLabel implements TableCellRenderer {
    public ColorCellRenderer(){ setOpaque(true); setHorizontalAlignment(CENTER); }
    @Override public Component getTableCellRendererComponent(
            JTable t,Object v,boolean sel,boolean foc,int r,int c){
        Color col = (v instanceof Color) ? (Color)v : null;
        setBackground(col!=null?col:Color.WHITE);
        setText(col!=null ? String.format("#%06X", col.getRGB()&0xFFFFFF) : "null");
        return this;
    }
}
