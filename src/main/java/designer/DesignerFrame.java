package designer;

import com.fasterxml.jackson.databind.ObjectMapper;
import designer.model.ProjectData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.*;

public class DesignerFrame extends JFrame {
    private File currentFile;
    private File lastDirectory;
    private final JFileChooser fileChooser;
    private final ObjectMapper mapper = new ObjectMapper();
    private       DesignSurfacePanel     designSurface;      // no longer final
    private final PropertyInspectorPanel inspector;
    private final CodeViewPanel          codeView;
    private       PreviewPanel           preview;            // no longer final

    private final JTabbedPane centerTabs;

    public DesignerFrame() {
        super("Swing Visual Designer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1400, 850);
        setLocationRelativeTo(null);

        // initialize panels
        designSurface = new DesignSurfacePanel();
        inspector     = new PropertyInspectorPanel(designSurface);
        codeView      = new CodeViewPanel();
        preview       = new PreviewPanel(designSurface);

        //init chooser
        lastDirectory = new File(System.getProperty("user.home"));
        fileChooser   = new JFileChooser(lastDirectory);

        // ─── MENU BAR ───────────────────────────────────────────────
        JMenuBar menuBar = new JMenuBar();
        JMenu   fileMenu = new JMenu("File");
        JMenuItem newItem  = new JMenuItem("New");
        JMenuItem openItem = new JMenuItem("Open..");
        JMenuItem saveItem = new JMenuItem("Save..");
        newItem .addActionListener(e -> newProject());
        openItem.addActionListener(e -> openProject());
        saveItem.addActionListener(e -> saveProject());
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // ─── LEFT COLUMN ───────────────────────────────────────────
        JTabbedPane leftTabs = new JTabbedPane();
        ComponentPalettePanel palette = new ComponentPalettePanel();
        leftTabs.addTab("Palette", palette);
        leftTabs.addTab("Hierarchy", new ComponentHierarchyPanel(designSurface));

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

        JSplitPane outerSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, mainSplit, codeView
        );
        outerSplit.setDividerLocation(900);
        outerSplit.setResizeWeight(1.0);

        add(outerSplit, BorderLayout.CENTER);

        // ─── WIRING (listeners, keybindings) ───────────────────────
        setupListenersAndBindings();

    }

    /** Wire up all your listeners and keybindings. */
    private void setupListenersAndBindings() {
        // selection → inspector
        designSurface.addSelectionListener(inspector::setTarget);
        // design → preview & codeView
        designSurface.addDesignChangeListener(() -> {
            preview.refresh();
            codeView.setCode(designSurface.generateCode());
        });

        // keybindings…
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
        NudgeAction(int dx, int dy){ this.dx = dx; this.dy = dy; }
        public void actionPerformed(ActionEvent e){
            designSurface.nudgeSelection(dx, dy);
        }
    }

    /** Clears everything to start a brand-new project. */
    private void newProject() {
        designSurface = new DesignSurfacePanel();
        preview       = new PreviewPanel(designSurface);

        centerTabs.setComponentAt(0, designSurface);
        centerTabs.setComponentAt(1, preview);

        inspector.setLayout(new BorderLayout());        // hack: re-create inspector
        // you may want to rebuild the hierarchy tab as well…

        setupListenersAndBindings();
        currentFile = null;
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
            ProjectData proj = designSurface.exportProject();
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
        fileChooser.setCurrentDirectory(lastDirectory);
        fileChooser.setDialogTitle("Open Project");

        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File chosen = fileChooser.getSelectedFile();
        lastDirectory = chosen.getParentFile();   // remember for next time
        currentFile   = chosen;

        try {
            ProjectData proj = mapper.readValue(currentFile, ProjectData.class);
            designSurface.importProject(proj);
            preview = new PreviewPanel(designSurface);
            centerTabs.setComponentAt(1, preview);
            setupListenersAndBindings();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    this, "Open failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
