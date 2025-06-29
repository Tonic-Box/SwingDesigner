package designer.util;

import designer.model.MenuItemData;
import designer.model.PopupMenuData;
import designer.model.ProjectData;
import designer.ui.ComponentHierarchyPanel;
import designer.ui.DesignSurfacePanel;
import designer.ui.OutputConsole;
import designer.ui.PreviewPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static designer.util.Static.*;

public class ProjectIO
{
    private static File currentFile;
    private static File lastDirectory;
    private static final JFileChooser fileChooser;

    static
    {
        lastDirectory = new File(System.getProperty("user.home"));
        fileChooser   = new JFileChooser(lastDirectory);
    }

    /** Clears everything to start a brand-new project. */
    public static void newProject() {
        codeTabs.setDesignerCode("");
        codeTabs.setUserCode("");

        designSurface = new DesignSurfacePanel();
        preview       = new PreviewPanel(designSurface, codeTabs);

        centerTabs.setComponentAt(0, designSurface);
        centerTabs.setComponentAt(1, preview);

        hierarchyPanel = new ComponentHierarchyPanel(designSurface);
        leftTabs.setComponentAt(1, hierarchyPanel);

        inspector.setLayout(new BorderLayout());

        designerFrame.setupListenersAndBindings();
        currentFile = null;
        codeTabs.setDesignerCode(CodeManager.generateCode(designSurface));
    }

    public static void saveProject() {
        fileChooser.setCurrentDirectory(lastDirectory);
        fileChooser.setDialogTitle("Save Project");
        fileChooser.setSelectedFile(currentFile);

        if (fileChooser.showSaveDialog(designerFrame) != JFileChooser.APPROVE_OPTION) return;

        File chosen = fileChooser.getSelectedFile();
        lastDirectory = chosen.getParentFile();   // remember for next time
        currentFile   = chosen;

        try {
            ProjectData proj = ModelBuilder.exportProject(designerFrame);
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(currentFile, proj);
            OutputConsole.info("Saved project as '" + currentFile.getName() + "'");
        } catch (Exception ex) {
            ex.printStackTrace();
            OutputConsole.error("Save failed: " + ex.getMessage());
        }
    }

    /** Load a project JSON and rebuild the surface from it. */
    public static void openProject() {
        fileChooser.setCurrentDirectory(lastDirectory);
        fileChooser.setDialogTitle("Open Project");

        if (fileChooser.showOpenDialog(designerFrame) != JFileChooser.APPROVE_OPTION) return;

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

            codeTabs.setUserCode(proj.userCode);
            designSurface.importProject(proj);

            hierarchyPanel.designChanged();

            preview = new PreviewPanel(designSurface, codeTabs);
            centerTabs.setComponentAt(1, preview);
            designerFrame.setupListenersAndBindings();
            codeTabs.setDesignerCode(CodeManager.generateCode(designSurface));
            OutputConsole.info("Opened project '" + currentFile.getName() + "'");
        } catch (Exception ex) {
            ex.printStackTrace();
            OutputConsole.error("Open failed: " + ex.getMessage());
        }
    }
}
