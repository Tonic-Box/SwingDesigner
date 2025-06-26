package designer.panels;

import designer.SwingDesignerApp;
import designer.misc.ResourceUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class CodeTabbedPane extends JPanel {
    private final CodeViewPanel designerView;
    private final CodeViewPanel codeView;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);

    private final JToggleButton btnDesign = new JToggleButton("Generated Design");
    private final JToggleButton btnCode   = new JToggleButton("User Code");
    private final JButton runTab          = new JButton();
    private ActionListener runListener;

    public CodeTabbedPane(CodeViewPanel designer, CodeViewPanel code) {
        super(new BorderLayout());
        this.designerView = designer;
        this.codeView     = code;

        // 1) content area
        contentPanel.add(designerView, "DESIGN");
        contentPanel.add(codeView,     "CODE");
        add(contentPanel, BorderLayout.CENTER);

        // 2) custom tab strip
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createMatteBorder(
                0, 0, 1, 0, UIManager.getColor("TabbedPane.highlight")));

        // left tabs
        JPanel leftTabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnDesign.setFocusPainted(false);
        btnCode  .setFocusPainted(false);
        btnDesign.setBorder(BorderFactory.createEmptyBorder(5,12,5,12));
        btnCode  .setBorder(BorderFactory.createEmptyBorder(5,12,5,12));
        ButtonGroup grp = new ButtonGroup();
        grp.add(btnDesign);
        grp.add(btnCode);
        leftTabs.add(btnDesign);
        leftTabs.add(btnCode);
        header.add(leftTabs, BorderLayout.WEST);

        // right: RUN icon button
        BufferedImage img = ResourceUtil.loadImageResource(SwingDesignerApp.class, "run.png");
        if(img != null)
        {
            ImageIcon icon = new ImageIcon(img.getScaledInstance(16,16,Image.SCALE_SMOOTH));
            runTab.setIcon(icon);
        }
        else
        {
            runTab.setText("RUN");
        }

        runTab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        runTab.setToolTipText("Run");
        runTab.setFocusPainted(false);
        runTab.setBorder(BorderFactory.createEmptyBorder(5,12,5,12));
        runTab.setOpaque(true);
        //runTab.setBackground(new Color(0,153,0));  // nice green
        runTab.setForeground(new Color(0,153,0));
        header.add(runTab, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // 3) wire up toggles ↔ cards
        btnDesign.addActionListener(e -> cardLayout.show(contentPanel, "DESIGN"));
        btnCode  .addActionListener(e -> cardLayout.show(contentPanel, "CODE"));
        btnCode.setSelected(true);
        cardLayout.show(contentPanel, "CODE");

        // 4) run action
        runTab.addActionListener(e -> {
            if (runListener != null) runListener.actionPerformed(
                    new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "RUN"));
        });
    }

    /** Register your compile/apply action here */
    public void onRun(ActionListener l) {
        this.runListener = l;
    }

    /** Designer (0) or User-code (1) */
    public CodeViewPanel getCodeViewAt(int idx) {
        if (idx == 0) return designerView;
        if (idx == 1) return codeView;
        throw new IndexOutOfBoundsException("Valid: 0 or 1");
    }

    /** Which real tab is showing (0=design,1=code) */
    public int getSelectedIndex() {
        return btnDesign.isSelected() ? 0 : 1;
    }
}
