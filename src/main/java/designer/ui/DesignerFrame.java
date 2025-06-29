package designer.ui;

import designer.SwingDesignerApp;
import designer.util.CodeManager;
import designer.util.ResourceUtil;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import static designer.util.ProjectIO.*;
import static designer.util.Static.*;

public class DesignerFrame extends JFrame {
    public DesignerFrame() {
        super("Swing Visual Designer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImage(ResourceUtil.loadImageResource(SwingDesignerApp.class, "icon.png"));
        setSize(1400, 850);
        setLocationRelativeTo(null);

        // initialize panels
        designSurface = new DesignSurfacePanel();
        inspector     = new PropertyInspectorPanel(designSurface);
        CodeViewPanel designerView = new CodeViewPanel();
        CodeViewPanel codeView = new CodeViewPanel();

        // ─── MENU BAR ───────────────────────────────────────────────
        JMenuBar menuBar = new JMenuBar();
        JMenu   fileMenu = new JMenu("File");
        JMenuItem newItem  = new JMenuItem("New");
        JMenuItem openItem = new JMenuItem("Open..");
        JMenuItem saveItem = new JMenuItem("Save..");
        newItem.addActionListener(e -> { newProject(); OutputConsole.info("New project created."); });
        openItem.addActionListener(e -> openProject());
        saveItem.addActionListener(e -> saveProject());
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        menuBar.add(fileMenu);

        JMenu viewMenu = new JMenu("View");
        JCheckBoxMenuItem lockItem = new JCheckBoxMenuItem("Lock Components");
        lockItem.addActionListener(e -> designSurface.setLockComponents(lockItem.isSelected()));
        viewMenu.add(lockItem);
        JCheckBoxMenuItem snapItem = new JCheckBoxMenuItem("Snap to Grid");
        snapItem.addActionListener(e -> designSurface.setSnapToGrid(snapItem.isSelected()));
        viewMenu.add(snapItem);
        menuBar.add(viewMenu);
        JMenuItem gridSizeItem = new JMenuItem("Grid Size...");
        gridSizeItem.addActionListener(e -> {
            SpinnerNumberModel model =
                    new SpinnerNumberModel(designSurface.getGridSize(), 1, 200, 1);
            JSpinner spinner = new JSpinner(model);

            int result = JOptionPane.showConfirmDialog(
                    this,
                    spinner,
                    "Grid Size (px)",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result == JOptionPane.OK_OPTION) {
                designSurface.setGridSize((Integer)spinner.getValue());
            }
        });
        viewMenu.add(gridSizeItem);
        JMenuItem gridColorItem = new JMenuItem("Grid Color...");
        gridColorItem.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(
                    this,
                    "Select Grid Color",
                    designSurface.getGridColor()
            );
            if (chosen != null) {
                designSurface.setGridColor(new Color(
                        chosen.getRed(),
                        chosen.getGreen(),
                        chosen.getBlue(),
                        designSurface.getGridColor().getAlpha()  // keep translucency
                ));
            }
        });
        viewMenu.add(gridColorItem);

        setJMenuBar(menuBar);

        // ─── LEFT COLUMN ───────────────────────────────────────────
        leftTabs = new JTabbedPane();
        ComponentPalettePanel palette = new ComponentPalettePanel();
        leftTabs.addTab("Palette", palette);

        // create and hold onto it so we can replace it on newProject()
        hierarchyPanel = new ComponentHierarchyPanel(designSurface);
        leftTabs.addTab("Hierarchy", hierarchyPanel);

        JSplitPane leftSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT, leftTabs, inspector
        );
        leftSplit.setDividerLocation(300);
        leftSplit.setResizeWeight(0.5);

        // ─── CENTER COLUMN ─────────────────────────────────────────
        centerTabs = new JTabbedPane();
        centerTabs.addTab("Design",  designSurface);
        centerTabs.addTab("Preview", preview);

        // ─── RIGHT COLUMN ──────────────────────────────────────────
        // just codeView

        // ─── COMPOSITE SPLITS ──────────────────────────────────────
        JSplitPane mainSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, leftSplit, centerTabs
        );
        mainSplit.setDividerLocation(260);
        mainSplit.setResizeWeight(0.0);

        codeTabs = new CodeTabbedPane(designerView, codeView);
        preview       = new PreviewPanel(designSurface, codeTabs);
        codeTabs.onRun(e -> CodeManager.compileAndApply(designSurface, codeTabs));
        JSplitPane outerSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, mainSplit, codeTabs
        );
        outerSplit.setDividerLocation(900);
        outerSplit.setResizeWeight(1.0);

        JScrollPane consoleScrollPane = OutputConsole.generateConsoleScrollPane();
        JSplitPane verticalSplitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT, outerSplit, consoleScrollPane
        );
        verticalSplitPane.setResizeWeight(1.0);
        verticalSplitPane.setDividerSize(6);
        verticalSplitPane.setDividerLocation(getHeight() - 150);
        add(verticalSplitPane, BorderLayout.CENTER);

        // ─── WIRING (listeners, keybindings) ───────────────────────
        setupListenersAndBindings();

        designerView.setCode(CodeManager.generateCode(designSurface));
    }

    /** Wire up all your listeners and keybindings. */
    public void setupListenersAndBindings() {
        designSurface.addSelectionListener(inspector::setTarget);
        designSurface.addDesignChangeListener(() -> codeTabs.setDesignerCode(CodeManager.generateCode(designSurface)));

        // keybindings…
        InputMap  im = designSurface.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = designSurface.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        am.put("delete", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                designSurface.removeSelected();
            }
        });
    }
}
