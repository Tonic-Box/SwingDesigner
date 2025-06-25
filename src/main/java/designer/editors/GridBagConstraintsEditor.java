package designer.editors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A scrollable panel for editing GridBagConstraints fields.
 */
public class GridBagConstraintsEditor extends JPanel implements Scrollable {
    private final JSpinner gridx       = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
    private final JSpinner gridy       = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
    private final JSpinner gridwidth   = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
    private final JSpinner gridheight  = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
    private final JSpinner weightx     = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 10.0, 0.1));
    private final JSpinner weighty     = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 10.0, 0.1));
    private final JComboBox<String> fill   = new JComboBox<>(new String[]{"NONE","HORIZONTAL","VERTICAL","BOTH"});
    private final JComboBox<String> anchor = new JComboBox<>(new String[]{
            "CENTER","NORTH","NORTHEAST","EAST","SOUTHEAST","SOUTH","SOUTHWEST","WEST","NORTHWEST"
    });
    private final JSpinner ipadx       = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
    private final JSpinner ipady       = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
    // Wider inset text field
    private final JTextField insetTxt = new JTextField("0,0,0,0", 20);

    public interface Listener { void constraintsChanged(GridBagConstraints gbc); }
    private Listener listener;

    public GridBagConstraintsEditor() {
        super(new GridBagLayout());
        GridBagConstraints cc = new GridBagConstraints();
        cc.insets = new Insets(2,2,2,2);
        String[] labels = {
                "gridx","gridy","gridwidth","gridheight",
                "weightx","weighty","fill","anchor",
                "ipadx","ipady","insets (top,left,bottom,right)"
        };
        Component[] editors = {
                gridx, gridy, gridwidth, gridheight,
                weightx, weighty, fill, anchor,
                ipadx, ipady, insetTxt
        };
        for (int i = 0; i < labels.length; i++) {
            cc.gridx = 0; cc.gridy = i; cc.weightx = 0;
            add(new JLabel(labels[i]), cc);
            cc.gridx = 1; cc.weightx = 1;
            add(editors[i], cc);

            // wire change events
            if (editors[i] instanceof JSpinner sp) {
                sp.addChangeListener(e -> fireChange());
            } else if (editors[i] instanceof JComboBox<?> cb) {
                cb.addActionListener(e -> fireChange());
            } else if (editors[i] instanceof JTextField tf) {
                tf.addActionListener(e -> fireChange());
                tf.addFocusListener(new FocusAdapter() {
                    public void focusLost(FocusEvent e) { fireChange(); }
                });
            }
        }
    }

    public void setListener(Listener l) {
        listener = l;
    }

    /**
     * Populate fields from an existing GridBagConstraints.
     */
    public void load(GridBagConstraints gbc) {
        gridx.setValue(gbc.gridx);
        gridy.setValue(gbc.gridy);
        gridwidth.setValue(gbc.gridwidth);
        gridheight.setValue(gbc.gridheight);
        weightx.setValue(gbc.weightx);
        weighty.setValue(gbc.weighty);
        fill.setSelectedItem(fillName(gbc.fill));
        anchor.setSelectedItem(anchorName(gbc.anchor));
        ipadx.setValue(gbc.ipadx);
        ipady.setValue(gbc.ipady);
        insetTxt.setText(
                gbc.insets.top + "," +
                        gbc.insets.left + "," +
                        gbc.insets.bottom + "," +
                        gbc.insets.right
        );
    }

    private void fireChange() {
        if (listener == null) return;
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx      = (Integer) gridx.getValue();
        gbc.gridy      = (Integer) gridy.getValue();
        gbc.gridwidth  = (Integer) gridwidth.getValue();
        gbc.gridheight = (Integer) gridheight.getValue();
        gbc.weightx    = ((Number) weightx.getValue()).doubleValue();
        gbc.weighty    = ((Number) weighty.getValue()).doubleValue();
        gbc.fill       = switch ((String) fill.getSelectedItem()) {
            case "BOTH"       -> GridBagConstraints.BOTH;
            case "HORIZONTAL" -> GridBagConstraints.HORIZONTAL;
            case "VERTICAL"   -> GridBagConstraints.VERTICAL;
            default             -> GridBagConstraints.NONE;
        };
        gbc.anchor     = switch ((String) anchor.getSelectedItem()) {
            case "NORTH"     -> GridBagConstraints.NORTH;
            case "NORTHEAST" -> GridBagConstraints.NORTHEAST;
            case "EAST"      -> GridBagConstraints.EAST;
            case "SOUTHEAST" -> GridBagConstraints.SOUTHEAST;
            case "SOUTH"     -> GridBagConstraints.SOUTH;
            case "SOUTHWEST" -> GridBagConstraints.SOUTHWEST;
            case "WEST"      -> GridBagConstraints.WEST;
            case "NORTHWEST" -> GridBagConstraints.NORTHWEST;
            default            -> GridBagConstraints.CENTER;
        };
        gbc.ipadx = (Integer) ipadx.getValue();
        gbc.ipady = (Integer) ipady.getValue();
        try {
            String[] parts = insetTxt.getText().split(",");
            gbc.insets = new Insets(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim()),
                    Integer.parseInt(parts[3].trim())
            );
        } catch (Exception ignored) {}
        listener.constraintsChanged(gbc);
    }

    // ── Implement Scrollable so JScrollPane limits height ────────
    @Override public Dimension getPreferredScrollableViewportSize() {
        Dimension pref = super.getPreferredSize();
        return new Dimension(pref.width, pref.height/2);
    }
    @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 20;
    }
    @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return visibleRect.height;
    }
    @Override public boolean getScrollableTracksViewportWidth() {
        return true;
    }
    @Override public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    private String fillName(int code) {
        return switch (code) {
            case GridBagConstraints.BOTH       -> "BOTH";
            case GridBagConstraints.HORIZONTAL -> "HORIZONTAL";
            case GridBagConstraints.VERTICAL   -> "VERTICAL";
            default                             -> "NONE";
        };
    }
    private String anchorName(int code) {
        return switch (code) {
            case GridBagConstraints.NORTH     -> "NORTH";
            case GridBagConstraints.NORTHEAST -> "NORTHEAST";
            case GridBagConstraints.EAST      -> "EAST";
            case GridBagConstraints.SOUTHEAST -> "SOUTHEAST";
            case GridBagConstraints.SOUTH     -> "SOUTH";
            case GridBagConstraints.SOUTHWEST -> "SOUTHWEST";
            case GridBagConstraints.WEST      -> "WEST";
            case GridBagConstraints.NORTHWEST -> "NORTHWEST";
            default                            -> "CENTER";
        };
    }
}
