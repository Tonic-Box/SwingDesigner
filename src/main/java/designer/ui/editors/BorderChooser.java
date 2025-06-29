package designer.ui.editors;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.Objects;

/**
 * A simple border chooser dialog.
 */
public class BorderChooser extends JDialog {
    private Border selectedBorder;
    private final JComboBox<String> typeBox;
    private final JButton colorBtn;
    private final JSpinner thicknessSpinner;
    private final JSpinner topSpinner, leftSpinner, bottomSpinner, rightSpinner;
    private final JTextField titleField;
    private final JLabel preview;
    private Color lineColor = Color.BLACK;
    private boolean okPressed = false;

    private BorderChooser(Window owner, Border initial) {
        super(owner, "Choose Border", ModalityType.APPLICATION_MODAL);

        typeBox = new JComboBox<>(new String[]{"None","Line","Empty","Titled"});
        typeBox.addActionListener(e -> updateUIState());

        colorBtn = new JButton("Color");
        colorBtn.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "Border Color", lineColor);
            if (c!=null) { lineColor = c; updatePreview(); }
        });

        thicknessSpinner = new JSpinner(new SpinnerNumberModel(1,1,20,1));
        topSpinner    = new JSpinner(new SpinnerNumberModel(5,0,50,1));
        leftSpinner   = new JSpinner(new SpinnerNumberModel(5,0,50,1));
        bottomSpinner = new JSpinner(new SpinnerNumberModel(5,0,50,1));
        rightSpinner  = new JSpinner(new SpinnerNumberModel(5,0,50,1));

        titleField = new JTextField(10);
        preview = new JLabel("Preview Area");
        preview.setPreferredSize(new Dimension(200, 50));
        preview.setBorder(initial);

        ChangeListener refresh = e -> updatePreview();
        thicknessSpinner.addChangeListener(refresh);
        topSpinner.addChangeListener(refresh);
        leftSpinner.addChangeListener(refresh);
        bottomSpinner.addChangeListener(refresh);
        rightSpinner.addChangeListener(refresh);
        titleField.addActionListener(e -> updatePreview());

        JPanel config = new JPanel(new GridLayout(0,2,5,5));
        config.add(new JLabel("Type:")); config.add(typeBox);
        config.add(new JLabel("Line Thickness:")); config.add(thicknessSpinner);
        config.add(new JLabel("Line Color:"));    config.add(colorBtn);
        config.add(new JLabel("Empty Top:"));     config.add(topSpinner);
        config.add(new JLabel("Empty Left:"));    config.add(leftSpinner);
        config.add(new JLabel("Empty Bottom:"));  config.add(bottomSpinner);
        config.add(new JLabel("Empty Right:"));   config.add(rightSpinner);
        config.add(new JLabel("Title:"));         config.add(titleField);

        JButton ok = new JButton("OK"), cancel = new JButton("Cancel");
        ok.addActionListener(e -> { okPressed=true; dispose(); });
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(); buttons.add(ok); buttons.add(cancel);

        getContentPane().setLayout(new BorderLayout(5,5));
        getContentPane().add(config, BorderLayout.CENTER);
        getContentPane().add(preview, BorderLayout.NORTH);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        updateUIState();
        pack(); setLocationRelativeTo(owner);
    }

    private void updateUIState() {
        String type = (String)typeBox.getSelectedItem();
        colorBtn.setEnabled("Line".equals(type));
        thicknessSpinner.setEnabled("Line".equals(type));
        topSpinner.setEnabled("Empty".equals(type));
        leftSpinner.setEnabled("Empty".equals(type));
        bottomSpinner.setEnabled("Empty".equals(type));
        rightSpinner.setEnabled("Empty".equals(type));
        titleField.setEnabled("Titled".equals(type));
        updatePreview();
    }

    private void updatePreview() {
        String type = (String)typeBox.getSelectedItem();
        switch(Objects.requireNonNull(type)) {
            case "None":    selectedBorder = null; break;
            case "Line":    selectedBorder = BorderFactory.createLineBorder(lineColor, (Integer)thicknessSpinner.getValue()); break;
            case "Empty":   selectedBorder = BorderFactory.createEmptyBorder(
                    (Integer)topSpinner.getValue(), (Integer)leftSpinner.getValue(),
                    (Integer)bottomSpinner.getValue(), (Integer)rightSpinner.getValue()); break;
            case "Titled":  selectedBorder = BorderFactory.createTitledBorder(titleField.getText()); break;
        }
        preview.setBorder(selectedBorder);
    }

    /**
     * Show the border chooser and return the selected border, or null if cancelled.
     */
    public static Border showDialog(Window owner, String title, Border initial) {
        BorderChooser dlg = new BorderChooser(owner, initial);
        dlg.setTitle(title);
        dlg.setVisible(true);
        return dlg.okPressed ? dlg.selectedBorder : null;
    }
}