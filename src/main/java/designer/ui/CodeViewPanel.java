package designer.ui;

import designer.ui.componants.ExRSyntaxTextArea;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class CodeViewPanel extends JPanel {
    private final ExRSyntaxTextArea area;

    public CodeViewPanel(){
        super(new BorderLayout());
        setBorder(new EmptyBorder(5,5,5,5));

        // — top bar with label + run button —
        JPanel top = new JPanel(new BorderLayout());
        add(top, BorderLayout.NORTH);

        // — code area —
        area = new ExRSyntaxTextArea(50, 60);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setEditable(true);
        add(new JScrollPane(area), BorderLayout.CENTER);
    }

    public void setCode(String code){
        area.setText(code);
    }

    public String getCode(){
        return area.getText();
    }
}
