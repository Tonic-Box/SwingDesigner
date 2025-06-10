package designer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class DesignerFrame extends JFrame {
    private final ComponentPalettePanel palette      = new ComponentPalettePanel();
    private final DesignSurfacePanel   designSurface = new DesignSurfacePanel();
    private final PropertyInspectorPanel inspector   = new PropertyInspectorPanel(designSurface);
    private final CodeViewPanel          codeView    = new CodeViewPanel();
    private final PreviewPanel           preview     = new PreviewPanel(designSurface);

    public DesignerFrame() {
        super("Swing Visual Designer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1400, 850);
        setLocationRelativeTo(null);

        // ─── TOOLBAR ─────────────────────────────────────────────────────
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        JToggleButton gridToggle = new JToggleButton("Grid");
        gridToggle.addActionListener(e -> {
            designSurface.setGridEnabled(gridToggle.isSelected());
        });
        toolbar.add(gridToggle);
        add(toolbar, BorderLayout.NORTH);

        // ─── SPLITS ──────────────────────────────────────────────────────
        //JSplitPane left  = new JSplitPane(JSplitPane.VERTICAL_SPLIT, palette, inspector);

        JTabbedPane leftTabs = new JTabbedPane();
        leftTabs.addTab("Palette",    palette);
        leftTabs.addTab("Hierarchy",  new ComponentHierarchyPanel(designSurface));
        leftTabs.addTab("Properties", inspector);

        JSplitPane right = new JSplitPane(JSplitPane.VERTICAL_SPLIT, preview, codeView);
        JSplitPane main  = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftTabs, designSurface);
        JSplitPane outer = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, main, right);
        right.setDividerLocation(400);
        main.setDividerLocation(260);
        outer.setDividerLocation(900);
        add(outer, BorderLayout.CENTER);

        /* Wiring */
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

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),  "nudgeLeft");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,0),  "nudgeRight");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP,   0),  "nudgeUp");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),  "nudgeDown");
        am.put("nudgeLeft",  new NudgeAction(-1,  0));
        am.put("nudgeRight", new NudgeAction( 1,  0));
        am.put("nudgeUp",    new NudgeAction( 0, -1));
        am.put("nudgeDown",  new NudgeAction( 0,  1));
    }

    private class NudgeAction extends AbstractAction {
        private final int dx, dy;
        NudgeAction(int dx, int dy){ this.dx = dx; this.dy = dy; }
        public void actionPerformed(ActionEvent e){
            designSurface.nudgeSelection(dx, dy);
        }
    }
}