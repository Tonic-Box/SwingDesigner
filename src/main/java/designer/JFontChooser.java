package designer;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A simple font chooser dialog.
 */
public class JFontChooser extends JDialog {
    private Font selectedFont;
    private final JList<String> familyList;
    private final JComboBox<String> styleBox;
    private final JSpinner sizeSpinner;
    private final JLabel preview;
    private boolean okPressed = false;

    private JFontChooser(Window owner, Font initial) {
        super(owner, "Choose Font", ModalityType.APPLICATION_MODAL);

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] families = ge.getAvailableFontFamilyNames();
        familyList = new JList<>(families);
        familyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        familyList.setSelectedValue(initial.getFamily(), true);

        styleBox = new JComboBox<>(new String[]{"Plain","Bold","Italic","Bold Italic"});
        styleBox.setSelectedIndex(initial.getStyle());

        sizeSpinner = new JSpinner(new SpinnerNumberModel(initial.getSize(), 6, 72, 1));

        preview = new JLabel("Sample Text");
        preview.setPreferredSize(new Dimension(200, 50));
        updatePreview();

        familyList.addListSelectionListener(e -> updatePreview());
        styleBox.addActionListener(e -> updatePreview());
        sizeSpinner.addChangeListener(e -> updatePreview());

        JPanel controls = new JPanel(new BorderLayout(5,5));
        controls.add(new JScrollPane(familyList), BorderLayout.CENTER);

        JPanel east = new JPanel(new GridLayout(3,1,5,5));
        east.add(styleBox);
        east.add(sizeSpinner);
        east.add(preview);
        controls.add(east, BorderLayout.EAST);

        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        ok.addActionListener(e -> { okPressed = true; dispose(); });
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(); buttons.add(ok); buttons.add(cancel);

        getContentPane().setLayout(new BorderLayout(5,5));
        getContentPane().add(controls, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        pack(); setLocationRelativeTo(owner);
    }

    private void updatePreview() {
        String family = familyList.getSelectedValue();
        int style = styleBox.getSelectedIndex();
        int size = (Integer)sizeSpinner.getValue();
        Font f = new Font(family, style, size);
        preview.setFont(f);
    }

    /**
     * Show the font chooser and return the selected font, or null if cancelled.
     */
    public static Font showDialog(Window owner, String title, Font initial) {
        JFontChooser dlg = new JFontChooser(owner, initial);
        dlg.setTitle(title);
        dlg.setVisible(true);
        if (dlg.okPressed) {
            String fam = dlg.familyList.getSelectedValue();
            int style = dlg.styleBox.getSelectedIndex();
            int size = (Integer)dlg.sizeSpinner.getValue();
            return new Font(fam, style, size);
        }
        return null;
    }
}