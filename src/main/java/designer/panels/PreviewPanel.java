package designer.panels;

import designer.misc.CodeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class PreviewPanel extends JPanel {
    private final DesignSurfacePanel designSurface;
    private final CodeTabbedPane codeView;
    private final JPanel canvas = new JPanel(null);
    public PreviewPanel(DesignSurfacePanel ds, CodeTabbedPane codeView){
        super(new BorderLayout());
        this.designSurface=ds;
        this.codeView = codeView;

        codeView.onRun((a) -> {
            refresh();
        });

        setPreferredSize(new Dimension(380,0));
        setBorder(new EmptyBorder(5,5,5,5));
        //add(new JLabel("Live Preview",SwingConstants.CENTER), BorderLayout.NORTH);
        add(new JScrollPane(canvas), BorderLayout.CENTER);
    }
    public void refresh() {
        canvas.removeAll();
        canvas.setBackground(designSurface.getBackground());
        canvas.setLayout(designSurface.getLayout());
        try { CodeManager.compileAndApply(this, codeView); }
        catch (Exception ex) { ex.printStackTrace(); }
        canvas.revalidate(); canvas.repaint();
    }
}