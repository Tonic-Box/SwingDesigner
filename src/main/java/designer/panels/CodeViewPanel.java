package designer.panels;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

public class CodeViewPanel extends JPanel {
    private final JTextArea area = new JTextArea();
    private final JButton runButton = new JButton("Run");
    private ActionListener runListener;

    public CodeViewPanel(){
        super(new BorderLayout());
        setBorder(new EmptyBorder(5,5,5,5));

        // — top bar with label + run button —
        JPanel top = new JPanel(new BorderLayout());
        top.add(new JLabel("Generated Code"), BorderLayout.WEST);
        top.add(runButton, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // — code area —
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setEditable(true);
        add(new JScrollPane(area), BorderLayout.CENTER);

        runButton.addActionListener(e -> {
            if (runListener != null) runListener.actionPerformed(e);
        });
    }

    public void setCode(String code){
        area.setText(code);
    }

    public String getCode(){
        return area.getText();
    }

    /** DesignerFrame will call this to hook in the compile-and-apply action */
    public void onRun(ActionListener l){
        this.runListener = l;
    }
}
