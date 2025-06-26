package designer.panels;

import com.fasterxml.jackson.databind.ObjectMapper;
import designer.SwingDesignerApp;
import designer.misc.PopupMenuManager;
import designer.misc.ResourceUtil;
import designer.model.MenuItemData;
import designer.model.PopupMenuData;
import designer.model.ProjectData;
import javax.tools.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DesignerFrame extends JFrame {
    private File currentFile;
    private File lastDirectory;
    private final JFileChooser fileChooser;
    private final ObjectMapper mapper = new ObjectMapper();
    private DesignSurfacePanel designSurface;
    private final PropertyInspectorPanel inspector;
    public final CodeViewPanel codeView;
    public final CodeViewPanel designerView;
    private PreviewPanel preview;
    private final JTabbedPane leftTabs;
    private ComponentHierarchyPanel hierarchyPanel;
    private final JTabbedPane centerTabs;

    public DesignerFrame() {
        super("Swing Visual Designer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImage(ResourceUtil.loadImageResource(SwingDesignerApp.class, "icon.png"));
        setSize(1400, 850);
        setLocationRelativeTo(null);

        // initialize panels
        designSurface = new DesignSurfacePanel();
        inspector     = new PropertyInspectorPanel(designSurface);
        designerView = new CodeViewPanel();
        codeView = new CodeViewPanel();
        preview       = new PreviewPanel(designSurface);

        // init chooser
        lastDirectory = new File(System.getProperty("user.home"));
        fileChooser   = new JFileChooser(lastDirectory);

        // ─── MENU BAR ───────────────────────────────────────────────
        JMenuBar menuBar = new JMenuBar();
        JMenu   fileMenu = new JMenu("File");
        JMenuItem newItem  = new JMenuItem("New");
        JMenuItem openItem = new JMenuItem("Open..");
        JMenuItem saveItem = new JMenuItem("Save..");
        newItem.addActionListener(e -> newProject());
        openItem.addActionListener(e -> openProject());
        saveItem.addActionListener(e -> saveProject());
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        menuBar.add(fileMenu);

        JMenu viewMenu = new JMenu("View");
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

        CodeTabbedPane codeTabs = new CodeTabbedPane(designerView, codeView);
        codeTabs.onRun(e -> compileAndApply(designerView.getCode(), codeView.getCode()));
        JSplitPane outerSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, mainSplit, codeTabs
        );
        outerSplit.setDividerLocation(900);
        outerSplit.setResizeWeight(1.0);

        add(outerSplit, BorderLayout.CENTER);

        // ─── WIRING (listeners, keybindings) ───────────────────────
        setupListenersAndBindings();

        designerView.setCode(designSurface.generateCode());
    }

    /**
     * 1) Wrap the user's code in a tiny helper class
     * 2) Invoke the JavaCompiler API
     * 3) Load it with a URLClassLoader
     * 4) Call its static apply(DesignSurfacePanel) method to mutate your live surface
     */
    private void compileAndApply(String designCode, String userCode) {
        try {
            String className = "LiveDesign";
            // 1) Build full source with imports, reusing the existing root panel
            String src =
                    "import designer.panels.DesignSurfacePanel;\n" +
                            "import javax.swing.*;\n" +
                            "import java.awt.*;\n" +
                            "public class " + className + " {\n" +
                            "  public static void apply(DesignSurfacePanel ds) throws Exception {\n" +
                            "    // reuse the existing designer panel as our root\n" +
                            "    JPanel panel = ds;\n" +
                            "    // clear out any old children\n" +
                            "    panel.removeAll();\n" +
                            // insert the user's layout code directly into the existing panel
                            designCode.replace("JPanel panel = new JPanel();", "") + "\n" +
                            userCode + "\n" +
                            "    // refresh display\n" +
                            "    panel.revalidate(); panel.repaint();\n" +
                            "    // fire design changed so hierarchy & inspector update\n" +
                            "    ds.externalPropertyChanged();\n" +
                            "  }\n" +
                            "}\n";

            // 2) Write source to a temp file and compile
            File tmpDir = Files.createTempDirectory("dyn").toFile();
            File srcFile = new File(tmpDir, className + ".java");
            Files.writeString(srcFile.toPath(), src, StandardCharsets.UTF_8);

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
                Iterable<? extends JavaFileObject> units =
                        fm.getJavaFileObjectsFromFiles(List.of(srcFile));

                boolean success = compiler.getTask(
                        null,
                        fm,
                        null,
                        List.of("-d", tmpDir.getAbsolutePath()),
                        null,
                        units
                ).call();
                if (!success) throw new RuntimeException("Compilation failed");
            }

            // 3) Load the compiled class and invoke apply(ds)
            try (URLClassLoader loader = new URLClassLoader(
                    new URL[]{ tmpDir.toURI().toURL() },
                    this.getClass().getClassLoader()
            )) {
                Class<?> liveCls = Class.forName(className, true, loader);
                Method m = liveCls.getMethod("apply", DesignSurfacePanel.class);
                m.invoke(null, designSurface);
            }

        } catch (Throwable ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Run failed: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /** Wire up all your listeners and keybindings. */
    private void setupListenersAndBindings() {
        designSurface.addSelectionListener(inspector::setTarget);
        designSurface.addDesignChangeListener(() -> {
            preview.refresh();
            designerView.setCode(designSurface.generateCode());
            //codeView.setCode(designSurface.generateCode());
        });

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

    /** Clears everything to start a brand-new project. */
    private void newProject() {
        designerView.setCode("");
        codeView.setCode("");
        designSurface = new DesignSurfacePanel();
        preview       = new PreviewPanel(designSurface);

        centerTabs.setComponentAt(0, designSurface);
        centerTabs.setComponentAt(1, preview);

        hierarchyPanel = new ComponentHierarchyPanel(designSurface);
        leftTabs.setComponentAt(1, hierarchyPanel);

        inspector.setLayout(new BorderLayout());        // hack: re-create inspector
        // you may want to rebuild the hierarchy tab as well…

        setupListenersAndBindings();
        currentFile = null;
        designerView.setCode(designSurface.generateCode());
    }

    private void saveProject() {
        fileChooser.setCurrentDirectory(lastDirectory);
        fileChooser.setDialogTitle("Save Project");
        fileChooser.setSelectedFile(currentFile);

        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File chosen = fileChooser.getSelectedFile();
        lastDirectory = chosen.getParentFile();   // remember for next time
        currentFile   = chosen;

        try {
            ProjectData proj = designSurface.exportProject(this);
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(currentFile, proj);
            JOptionPane.showMessageDialog(this, "Saved to " + currentFile.getName());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    this, "Save failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /** Load a project JSON and rebuild the surface from it. */
    private void openProject() {
        designerView.setCode("");
        fileChooser.setCurrentDirectory(lastDirectory);
        fileChooser.setDialogTitle("Open Project");

        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        newProject();

        File chosen = fileChooser.getSelectedFile();
        lastDirectory = chosen.getParentFile();   // remember for next time
        currentFile   = chosen;

        try {
            ProjectData proj = mapper.readValue(currentFile, ProjectData.class);

            // --- NEW: rebuild PopupMenuManager from the saved data ---
            List<String> menuNames = new ArrayList<>(PopupMenuManager.getMenuNames());
            for (String name : menuNames) {
                PopupMenuManager.removeMenu(name);
            }
            for (PopupMenuData pmData : proj.popupMenus) {
                JPopupMenu menu = new JPopupMenu();
                for (MenuItemData miData : pmData.items) {
                    JMenuItem item = new JMenuItem(miData.text);
                    item.setActionCommand(miData.actionCommand);
                    menu.add(item);
                }
                PopupMenuManager.putMenu(pmData.name, menu);
            }

            codeView.setCode(proj.userCode);
            designSurface.importProject(proj);

            hierarchyPanel.designChanged();

            preview = new PreviewPanel(designSurface);
            centerTabs.setComponentAt(1, preview);
            setupListenersAndBindings();
            preview.refresh();
            designerView.setCode(designSurface.generateCode());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    this, "Open failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
