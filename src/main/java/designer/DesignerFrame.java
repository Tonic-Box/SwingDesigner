package designer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class DesignerFrame extends JFrame {
    private final ComponentPalettePanel    palette       = new ComponentPalettePanel();
    private final DesignSurfacePanel       designSurface = new DesignSurfacePanel();
    private final PropertyInspectorPanel   inspector     = new PropertyInspectorPanel(designSurface);
    private final CodeViewPanel            codeView      = new CodeViewPanel();
    private final PreviewPanel             preview       = new PreviewPanel(designSurface);

    public DesignerFrame() {
        super("Swing Visual Designer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1400, 850);
        setLocationRelativeTo(null);

        // ─── TOOLBAR ─────────────────────────────────────────────────────
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        JToggleButton gridToggle = new JToggleButton("Grid");
        gridToggle.addActionListener(e -> designSurface.setGridEnabled(gridToggle.isSelected()));
        toolbar.add(gridToggle);
        add(toolbar, BorderLayout.NORTH);

        // ─── LEFT COLUMN (Palette / Hierarchy above Inspector) ───────────
        JTabbedPane leftTabs = new JTabbedPane();
        leftTabs.addTab("Palette",   palette);
        leftTabs.addTab("Hierarchy", new ComponentHierarchyPanel(designSurface));

        JSplitPane leftSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                leftTabs,
                inspector
        );
        leftSplit.setDividerLocation(300);
        leftSplit.setResizeWeight(0.5);

        // ─── CENTER COLUMN (Design *or* Preview) ────────────────────────
        JTabbedPane centerTabs = new JTabbedPane();
        centerTabs.addTab("Design",  designSurface);
        centerTabs.addTab("Preview", preview);

        // ─── RIGHT COLUMN (Code View) ──────────────────────────────────
        // (we keep codeView full height on the right)

        // ─── COMPOSE HORIZONTAL SPLITS ─────────────────────────────────
        // First split: left vs. center
        JSplitPane mainSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                leftSplit,
                centerTabs
        );
        mainSplit.setDividerLocation(260);
        mainSplit.setResizeWeight(0.0); // left fixed, center expands

        // Second split: (left+center) vs. codeView
        JSplitPane outerSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                mainSplit,
                codeView
        );
        outerSplit.setDividerLocation(900);
        outerSplit.setResizeWeight(1.0); // center expands, codeView fixed

        add(outerSplit, BorderLayout.CENTER);

        // ─── WIRING ─────────────────────────────────────────────────────
        designSurface.addSelectionListener(inspector::setTarget);
        designSurface.addDesignChangeListener(() -> {
            preview.refresh();
            codeView.setCode(designSurface.generateCode());
        });

        // ─── KEYBINDINGS ────────────────────────────────────────────────
        InputMap  im = designSurface.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = designSurface.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        am.put("delete", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                designSurface.removeSelected();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,  0), "nudgeLeft");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "nudgeRight");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP,    0), "nudgeUp");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,  0), "nudgeDown");
        am.put("nudgeLeft",  new NudgeAction(-1,  0));
        am.put("nudgeRight", new NudgeAction( 1,  0));
        am.put("nudgeUp",    new NudgeAction( 0, -1));
        am.put("nudgeDown",  new NudgeAction( 0,  1));
    }

    private class NudgeAction extends AbstractAction {
        private final int dx, dy;
        NudgeAction(int dx, int dy) { this.dx = dx; this.dy = dy; }
        public void actionPerformed(ActionEvent e) {
            designSurface.nudgeSelection(dx, dy);
        }
    }
}
