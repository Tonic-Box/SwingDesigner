package designer.util;

import designer.model.*;
import designer.types.PositionType;
import designer.ui.DesignerFrame;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import static designer.util.Static.*;

public class ModelBuilder {
    public static ProjectData exportProject(DesignerFrame frame) {
        ProjectData proj = new ProjectData();
        // 1) root
        proj.root = buildComponentData(designSurface);
        proj.userCode = codeTabs.getUserCode();
        // 2) all popup menus
        proj.popupMenus = PopupMenuManager.getMenuNames().stream()
                .map(name -> {
                    PopupMenuData pm = new PopupMenuData();
                    pm.name  = name;
                    pm.items = PopupMenuManager.getMenu(name).getComponents().length == 0
                            ? List.of()
                            : Arrays.stream(PopupMenuManager.getMenu(name).getComponents())
                            .filter(c -> c instanceof JMenuItem)
                            .map(mi -> {
                                MenuItemData mid = new MenuItemData();
                                mid.text          = ((JMenuItem)mi).getText();
                                mid.actionCommand = ((JMenuItem)mi).getActionCommand();
                                return mid;
                            })
                            .collect(Collectors.toList());
                    return pm;
                })
                .collect(Collectors.toList());
        return proj;
    }

    private static ComponentData buildComponentData(Container cont) {
        ComponentData data = new ComponentData();
        data.className         = cont.getClass().getName();
        if (cont instanceof JComponent jc) {
            data.name           = jc.getName();
            data.visible        = jc.isVisible();
            if (jc instanceof AbstractButton ab) {
                data.text = ab.getText();
            }
            else if (jc instanceof JLabel lbl) {
                data.text = lbl.getText();
            }
            else if (jc instanceof javax.swing.text.JTextComponent tc) {
                data.text = tc.getText();
            }
            else if (jc instanceof JComboBox<?> combo) {
                Object sel = combo.getSelectedItem();
                data.text = sel == null ? null : sel.toString();
            }
            else if (jc instanceof JSpinner spinner) {
                Object val = spinner.getValue();
                data.text = val == null ? null : val.toString();
            }
            data.bounds         = new RectangleData(jc.getBounds());
            data.preferredSize  = new SizeData(jc.getPreferredSize());
            data.minimumSize    = new SizeData(jc.getMinimumSize());
            data.maximumSize    = new SizeData(jc.getMaximumSize());
            data.backgroundColor= new ColorData(jc.getBackground());
            data.foregroundColor= new ColorData(jc.getForeground());
            data.font           = new FontData(jc.getFont());
            // border
            if (jc.getBorder() != null) {
                data.border     = BorderData.fromBorder(jc.getBorder());
            }
            // layoutConstraint & positionType
            Object cons = jc.getClientProperty("layoutConstraint");
            data.layoutConstraint = cons == null ? null : cons.toString();
            Object pos = jc.getClientProperty("positionType");
            data.positionType     = pos == null ? null : pos.toString();
            // popup-menu
            JPopupMenu pm = jc.getComponentPopupMenu();
            data.popupMenuName    = pm == null ? null : PopupMenuManager.menuNameOf(pm);
        }
        // layout manager of this container
        data.layout = LayoutData.fromLayout(cont.getLayout());

        // recurse children
        data.children = new ArrayList<>();
        for (Component c : cont.getComponents()) {
            if (c instanceof Container child) {
                data.children.add(buildComponentData(child));
            }
        }
        return data;
    }

    public static void rebuildFromData(Container parent, ComponentData data) throws Exception {
        // skip class check on root: data.className should == DesignSurfacePanel
        for (ComponentData cd : data.children) {
            Class<?> cls = Class.forName(cd.className);
            JComponent comp = (JComponent)cls.getDeclaredConstructor().newInstance();
            // basic props
            comp.setName(cd.name);
            comp.setVisible(cd.visible);
            comp.setBounds(cd.bounds.toRectangle());
            comp.setPreferredSize(cd.preferredSize.toDimension());
            comp.setMinimumSize(cd.minimumSize.toDimension());
            comp.setMaximumSize(cd.maximumSize.toDimension());
            comp.setBackground(cd.backgroundColor.toColor());
            comp.setForeground(cd.foregroundColor.toColor());
            comp.setFont(cd.font.toFont());
            //text
            if (comp instanceof AbstractButton ab && cd.text != null) {
                ab.setText(cd.text);
            }
            else if (comp instanceof JLabel lbl && cd.text != null) {
                lbl.setText(cd.text);
            }
            else if (comp instanceof javax.swing.text.JTextComponent tc && cd.text != null) {
                tc.setText(cd.text);
            }
            else if (comp instanceof JComboBox<?> combo && cd.text != null) {
                combo.setSelectedItem(cd.text);
            }
            else if (comp instanceof JSpinner spinner && cd.text != null) {
                // attempt to convert back to a number if your spinner uses SpinnerNumberModel
                SpinnerModel model = spinner.getModel();
                if (model instanceof SpinnerNumberModel) {
                    try {
                        Number n = Double.valueOf(cd.text);
                        spinner.setValue(n);
                    } catch (NumberFormatException ex) {
                        spinner.setValue(cd.text);
                    }
                } else {
                    spinner.setValue(cd.text);
                }
            }
            // border
            if (cd.border != null) {
                comp.setBorder(cd.border.toBorder());
            }
            // popup menu
            if (cd.popupMenuName != null) {
                JPopupMenu menu = PopupMenuManager.getMenu(cd.popupMenuName);
                // save on the component so the PreviewPanel can pick it up
                comp.putClientProperty("savedPopup", menu);
                // — and do NOT call comp.setComponentPopupMenu(menu) here —
            }
            // add to parent
            if (parent.getLayout() instanceof BorderLayout) {
                parent.add(comp, cd.layoutConstraint);
            } else {
                parent.add(comp);
            }
            // positionType
            if (cd.positionType != null) {
                comp.putClientProperty(
                        "positionType",
                        PositionType.valueOf(cd.positionType)   // back to enum
                );
            } else {
                comp.putClientProperty(
                        "positionType",
                        PositionType.ABSOLUTE                   // or your default
                );
            }

            // recurse
            rebuildFromData(comp, cd);
            // re-install drag/resize
            designSurface.installDragResizeBehavior(comp);
        }
        // restore layout manager on parent
        parent.setLayout(data.layout.toLayoutManager());
    }
}
