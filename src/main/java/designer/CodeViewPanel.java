package designer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

class CodeViewPanel extends JPanel {
    private final JTextArea area = new JTextArea();
    CodeViewPanel(){
        super(new BorderLayout());
        setBorder(new EmptyBorder(5,5,5,5));
        add(new JLabel("Generated Code"), BorderLayout.NORTH);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setEditable(false);
        add(new JScrollPane(area), BorderLayout.CENTER);
    }
    void setCode(String code){ area.setText(code); }
}