package designer.panels;

import designer.misc.PopupMenuManager;
import designer.SwingDesignerApp;
import designer.model.*;
import designer.types.PositionType;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class DesignSurfacePanel extends JPanel implements DropTargetListener {
    private JComponent selectedComp = null;
    private final List<DesignChangeListener> changeL = new ArrayList<>();
    private final List<SelectionListener>    selectL = new ArrayList<>();
    private final AtomicInteger idSeq   = new AtomicInteger();
    private int anonCount = 0;

    public DesignSurfacePanel() {
        super(null);
        setBackground(new Color(SwingDesignerApp.BG_DARK.getRGB()));
        setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        new DropTarget(this, DnDConstants.ACTION_COPY, this, true);

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                Component hit = SwingUtilities.getDeepestComponentAt(
                        DesignSurfacePanel.this,
                        e.getX(), e.getY()
                );
                // allow the panel itself to be selected too
                if( hit instanceof JComponent ) {
                    selectedComp = (JComponent)hit;
                } else {
                    selectedComp = null;
                }
                notifySelection(selectedComp);
                repaint();
            }
        });
    }

    /** Remove currently selected component */
    public void removeSelected() {
        if (selectedComp != null) {
            Container parent = selectedComp.getParent();
            parent.remove(selectedComp);
            selectedComp = null;
            notifySelection(null);
            notifyChange();
        }
    }

    public void setSelectedComponent(JComponent c) {
        this.selectedComp = c;
        notifySelection(c);
        repaint();
    }
    public void clearSelection() {
        setSelectedComponent(null);
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);

        if (selectedComp != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(Color.ORANGE);
                float[] dash = {4f, 4f};
                g2.setStroke(new BasicStroke(
                        2f,
                        BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER,
                        1f, dash, 0f
                ));

                // convert the child’s bounds into surface coords
                Rectangle r = SwingUtilities.convertRectangle(
                        selectedComp.getParent(),
                        selectedComp.getBounds(),
                        this
                );

                // inset by 1 so stroke is fully visible inside the component
                //g2.drawRect(r.x + 1, r.y + 1, r.width - 3, r.height - 3);

                // draw a 2px-wide stroke exactly around the component
                // subtract 1 from width/height so we hit the outer pixel exactly
                g2.drawRect(r.x, r.y, r.width - 1, r.height - 1);
            } finally {
                g2.dispose();
            }
        }
    }

    /* DropTargetListener */
    @Override public void dragEnter(DropTargetDragEvent e) {}
    @Override public void dragOver (DropTargetDragEvent e) {}
    @Override public void dropActionChanged(DropTargetDragEvent e) {}
    @Override public void dragExit (DropTargetEvent e) {}

    @Override
    public void drop(DropTargetDropEvent e) {
        try {
            e.acceptDrop(DnDConstants.ACTION_COPY);
            String className = (String)e.getTransferable().getTransferData(DataFlavor.stringFlavor);
            Class<?> clazz = Class.forName(className);
            JComponent comp = (JComponent)clazz.getConstructor().newInstance();

            /* decide parent container */
            Point dropPt     = e.getLocation();
            Container parent = findContainerAt(dropPt, null);
            if (parent == null) parent = this;

            /* compute initial position */
            Point inParent = convertPointTo(parent, dropPt);

            /* id + behaviour */
            comp.setName(clazz.getSimpleName().toLowerCase() + idSeq.incrementAndGet());
            installDragResizeBehavior(comp);

            /* add with or without constraint */
            LayoutManager lm = parent.getLayout();
            if (lm instanceof BorderLayout) {
                String c = (String)comp.getClientProperty("layoutConstraint");
                if (c == null) c = "Center";
                parent.add(comp, c);
            } else {
                comp.setBounds(inParent.x, inParent.y, 120, 30);
                parent.add(comp);
            }

            comp.putClientProperty("positionType", PositionType.ABSOLUTE);

            this.selectedComp = comp;
            notifySelection(comp);
            notifyChange();
            e.dropComplete(true);
        } catch (Exception ex) {
            ex.printStackTrace();
            e.dropComplete(false);
        }
    }

    /**
     * Call this whenever any property or constraint changes.
     * We now:
     *  1) re-name to keep names unique,
     *  2) re-apply client-property constraints for every BorderLayout,
     *  3) re-install move/resize & click listeners on every child,
     *  4) fire our designChanged listeners.
     */
    public void externalPropertyChanged() {
        ensureUniqueNames();
        reapplyConstraints(this);
        // re-attach our MoveResizeAdapter to every JComponent in the tree
        installBehaviorsRecursively(this);
        notifyChange();
        //notifySelection(selectedComp);
    }

    /** Walk the component subtree and install our move/resize + click behavior. */
    private void installBehaviorsRecursively(Container parent) {
        for (Component c : parent.getComponents()) {
            if (c instanceof JComponent jc) {
                installDragResizeBehavior(jc);
                // since JComponent is also a Container, just recurse on it
                if (jc.getComponentCount() > 0) {
                    installBehaviorsRecursively(jc);
                }
            }
        }
    }

    /**
     * Recursively walks the component tree, and for any Container
     * whose layout is a BorderLayout, tears out all of its children
     * and re-adds them using the String in each child's
     * "layoutConstraint" client-property.
     */
    private void reapplyConstraints(Container cont) {
        if (cont.getLayout() instanceof BorderLayout) {
            Component[] oldKids = cont.getComponents();
            List<Component> copy   = new ArrayList<>(List.of(oldKids));
            cont.removeAll();
            for (Component c : copy) {
                if (c instanceof JComponent jc) {
                    String key = (String)jc.getClientProperty("layoutConstraint");
                    if (key == null) key = BorderLayout.CENTER;
                    cont.add(c, key);
                } else {
                    cont.add(c);
                }
            }
            cont.validate();
            cont.repaint();
        }
        for (Component c : cont.getComponents()) {
            if (c instanceof Container child) {
                reapplyConstraints(child);
            }
        }
    }

    /** deepest JPanel under design-surface coords (null ⇒ root) */
    private Container findContainerAt(Point p, Component exclude) {
        Component hit = SwingUtilities.getDeepestComponentAt(this, p.x, p.y);
        while (hit != null) {
            if (hit == exclude) {
                hit = hit.getParent();
                continue;
            }
            if (hit instanceof JPanel) {
                return (JPanel) hit;
            }
            hit = hit.getParent();
        }
        return null;
    }

    /** convert a point from design-surface coords → target container coords */
    private Point convertPointTo(Container target, Point pOnSurface) {
        return SwingUtilities.convertPoint(this, pOnSurface, target);
    }

    private String layoutExpr(LayoutManager lm) {
        if (lm == null) return "null";
        if (lm instanceof FlowLayout)   return "new FlowLayout()";
        if (lm instanceof BorderLayout) return "new BorderLayout()";
        if (lm instanceof GridLayout g) return "new GridLayout(" +
                g.getRows() + "," + g.getColumns() + ")";
        /* fall-back */                 return "null";
    }

    /* guarantee uniqueness of Component.getName() */
    private void ensureUniqueNames() {
        for (Component c1 : getComponents()) {
            if (!(c1 instanceof JComponent jc1) || jc1.getName() == null) continue;
            String base = jc1.getName(), candidate = base;
            int n = 1;
            while (true) {
                boolean clash = false;
                for (Component c2 : getComponents()) {
                    if (c2 != c1 && c2 instanceof JComponent jc2 &&
                            candidate.equals(jc2.getName())) { clash = true; break; }
                }
                if (!clash) { jc1.setName(candidate); break; }
                candidate = base + (++n);
            }
        }
    }

    /* ───── Movable & resizable behaviour ───── */
    private void installDragResizeBehavior(JComponent c) {
        MoveResizeAdapter adapter = new MoveResizeAdapter(c);
        c.addMouseListener(adapter);
        c.addMouseMotionListener(adapter);
    }

    /* ───── Observers ───── */
    public void addDesignChangeListener(DesignChangeListener l){ changeL.add(l);}
    public void addSelectionListener(SelectionListener l){ selectL.add(l);}
    private void notifyChange()   { revalidate(); repaint(); changeL.forEach(DesignChangeListener::designChanged);}
    private void notifySelection(Component c){ selectL .forEach(l -> l.selectionChanged(c)); }

    /* ───── Simple code generation (Java Swing) ───── */
    public String generateCode() {
        anonCount = 0;
        StringBuilder sb = new StringBuilder("// ---- auto-generated layout ----\n");
        for (String name : PopupMenuManager.getMenuNames()) {
            String var = name.replaceAll("\\W+", "_");
            sb.append("JPopupMenu ").append(var)
                    .append(" = new JPopupMenu();\n");
            JPopupMenu pm = PopupMenuManager.getMenu(name);
            for (Component mi : pm.getComponents()) {
                if (mi instanceof JMenuItem item) {
                    sb.append(var)
                            .append(".add(new JMenuItem(\"")
                            .append(item.getText().replace("\"", "\\\""))
                            .append("\"));\n");
                }
            }
            sb.append("\n");
        }

        emitContainer(this, "panel", sb, true);
        return sb.toString();
    }

    /**
     * Recursively emits code for a container and *every* child component,
     * including preferred/minimum/maximum size calls.
     */
    private void emitContainer(Container cont, String var, StringBuilder sb, boolean isRoot) {
        if (isRoot) {
            sb.append("JPanel ").append(var).append(" = new JPanel();\n")
                    .append(var).append(".setLayout(").append(layoutExpr(cont.getLayout())).append(");\n\n");
        } else {
            sb.append(var).append(".setLayout(").append(layoutExpr(cont.getLayout())).append(");\n");
            Color bg = cont.getBackground();
            if (bg != null) {
                sb.append(var).append(".setBackground(new Color(0x")
                        .append(String.format("%06X", bg.getRGB() & 0xFFFFFF)).append("));\n");
            }
            sb.append("\n");
        }

        PopupMenuManager.clearAll();

        for (Component c : cont.getComponents()) {
            if (!(c instanceof JComponent jc)) continue;

            // decide variable name
            String id = jc.getName();
            if (id == null || id.isEmpty()) {
                id = jc.getClass().getSimpleName().toLowerCase() + anonCount++;
            }

            // instantiate
            sb.append(jc.getClass().getSimpleName())
                    .append(" ").append(id)
                    .append(" = new ").append(jc.getClass().getSimpleName())
                    .append("();\n");

            // text if applicable
            if (jc instanceof AbstractButton but && but.getText() != null) {
                sb.append(id).append(".setText(\"")
                        .append(but.getText().replace("\"","\\\""))
                        .append("\");\n");
            }

            // layout
            sb.append(id).append(".setLayout(")
                    .append(layoutExpr(jc.getLayout()))
                    .append(");\n");

            // background / foreground
            Color bgc = jc.getBackground(), fgc = jc.getForeground();
            if (bgc != null) {
                sb.append(id).append(".setBackground(new Color(0x")
                        .append(String.format("%06X", bgc.getRGB() & 0xFFFFFF))
                        .append("));\n");
            }
            if (fgc != null) {
                sb.append(id).append(".setForeground(new Color(0x")
                        .append(String.format("%06X", fgc.getRGB() & 0xFFFFFF))
                        .append("));\n");
            }

            // ─── popup-menu by reference ─────────────────────────────
            // first try the actual popup, then fall back to "savedPopup" client prop
            JPopupMenu pmRef = jc.getComponentPopupMenu();
            if (pmRef == null) {
                Object saved = jc.getClientProperty("savedPopup");
                if (saved instanceof JPopupMenu) {
                    pmRef = (JPopupMenu) saved;
                }
            }
            if (pmRef != null) {
                String menuName = PopupMenuManager.menuNameOf(pmRef);
                if (menuName == null) {
                    // auto-register unknown menu
                    menuName = "popupMenu" + PopupMenuManager.getMenuNames().size();
                    PopupMenuManager.putMenu(menuName, pmRef);
                }
                String varMenu = menuName.replaceAll("\\W+", "_");
                sb.append(id).append(".setComponentPopupMenu(")
                        .append(varMenu).append(");\n");
            }

            // preferred / minimum / maximum size
            Dimension ps = jc.getPreferredSize();
            if (ps != null) {
                sb.append(id).append(".setPreferredSize(new Dimension(")
                        .append(ps.width).append(", ").append(ps.height)
                        .append("));\n");
            }
            Dimension ms = jc.getMinimumSize();
            if (ms != null) {
                sb.append(id).append(".setMinimumSize(new Dimension(")
                        .append(ms.width).append(", ").append(ms.height)
                        .append("));\n");
            }
            Dimension xs = jc.getMaximumSize();
            if (xs != null) {
                sb.append(id).append(".setMaximumSize(new Dimension(")
                        .append(xs.width).append(", ").append(xs.height)
                        .append("));\n");
            }

            // position & add
            PositionType pt = (PositionType) jc.getClientProperty("positionType");
            if (pt == null) pt = PositionType.ABSOLUTE;
            if (pt == PositionType.ABSOLUTE) {
                Rectangle r = jc.getBounds();
                sb.append(id).append(".setBounds(")
                        .append(r.x).append(", ").append(r.y).append(", ")
                        .append(r.width).append(", ").append(r.height)
                        .append(");\n");
            }
            Object cons = jc.getClientProperty("layoutConstraint");
            String constraint = cons!=null
                    ? "BorderLayout." + cons.toString().toUpperCase()
                    : "BorderLayout.CENTER";
            sb.append(var).append(".add(")
                    .append(id).append(", ").append(constraint)
                    .append(");\n\n");

            // recurse
            if (jc.getComponentCount() > 0) {
                emitContainer(jc, id, sb, false);
            }
        }
    }

    /** Build a full ProjectData out of the live surface. */
    public ProjectData exportProject() {
        ProjectData proj = new ProjectData();
        // 1) root
        proj.root = buildComponentData(this);
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

    private ComponentData buildComponentData(Container cont) {
        ComponentData data = new ComponentData();
        data.className         = cont.getClass().getName();
        if (cont instanceof JComponent jc) {
            data.name           = jc.getName();
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

    /**
     * Given a ProjectData, wipe the surface and rebuild it from scratch.
     */
    public void importProject(ProjectData proj) throws Exception {
        removeAll();
        rebuildFromData(this, proj.root);
        revalidate();
        repaint();
    }

    private void rebuildFromData(Container parent, ComponentData data) throws Exception {
        // skip class check on root: data.className should == DesignSurfacePanel
        for (ComponentData cd : data.children) {
            Class<?> cls = Class.forName(cd.className);
            JComponent comp = (JComponent)cls.getDeclaredConstructor().newInstance();
            // basic props
            comp.setName(cd.name);
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
            installDragResizeBehavior(comp);
        }
        // restore layout manager on parent
        parent.setLayout(data.layout.toLayoutManager());
    }


    /* ───── helper classes ───── */
    @FunctionalInterface public interface DesignChangeListener { void designChanged(); }
    @FunctionalInterface public interface SelectionListener    { void selectionChanged(Component c); }

    /* ───── Component-level mouse adapter for move + resize + context menu ───── */
    private class MoveResizeAdapter extends MouseAdapter {
        private final JComponent target;
        private final JPopupMenu popup = new JPopupMenu();
        private Point  dragOffset;
        private boolean resizing;
        private static final int HANDLE = 8;

        MoveResizeAdapter(JComponent target){
            this.target = target;

            JMenuItem remove = new JMenuItem("Remove");
            remove.addActionListener(e -> {
                Container parent = target.getParent();
                parent.remove(target);
                notifySelection(null);
                notifyChange();
            });
            popup.add(remove);

            popup.addSeparator();

            JMenuItem bringFront = new JMenuItem("Bring to Front");
            bringFront.addActionListener(e -> {
                Container parent = target.getParent();
                parent.setComponentZOrder(target, 0);
                notifyChange();
            });
            popup.add(bringFront);

            JMenuItem sendBack = new JMenuItem("Send to Back");
            sendBack.addActionListener(e -> {
                Container parent = target.getParent();
                int count = parent.getComponentCount();
                parent.setComponentZOrder(target, count - 1);
                notifyChange();
            });
            popup.add(sendBack);

            popup.addSeparator();

            JMenu alignMenu = new JMenu("Align");
            JMenuItem alignLeft = new JMenuItem("Left");
            alignLeft.addActionListener(e -> {
                Rectangle r = target.getBounds();
                r.x = 0;
                target.setBounds(r);
                notifyChange();
            });
            alignMenu.add(alignLeft);

            JMenuItem alignCenter = new JMenuItem("Center Horizontally");
            alignCenter.addActionListener(e -> {
                Rectangle r = target.getBounds();
                int parentW = target.getParent().getWidth();
                r.x = (parentW - r.width) / 2;
                target.setBounds(r);
                notifyChange();
            });
            alignMenu.add(alignCenter);

            popup.add(alignMenu);
        }

        /* helper: show popup across platforms */
        private void maybeShowPopup(MouseEvent e) {
            if (!e.isPopupTrigger()) return;
            // if user has set a JPopupMenu on this component, show it instead
            //JPopupMenu pm = target.getComponentPopupMenu();
            // your existing designer menu
            //Objects.requireNonNullElse(pm, popup).show(target, e.getX(), e.getY());
            popup.show(target, e.getX(), e.getY());
        }

        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
            LayoutManager lm = target.getParent().getLayout();
            if (lm == null) {
                Rectangle r = target.getBounds();
                resizing = (e.getX() >= r.width - HANDLE && e.getY() >= r.height - HANDLE);
                dragOffset = e.getPoint();
            } else {
                resizing = false;
                dragOffset = null;
            }
            notifySelection(target);

            DesignSurfacePanel.this.selectedComp = target;
            DesignSurfacePanel.this.notifySelection(target);
            DesignSurfacePanel.this.repaint();
        }
        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);

            /* re-parent if dropped over a different panel */
            Point center = SwingUtilities.convertPoint(
                    target,
                    target.getWidth()/2, target.getHeight()/2,
                    DesignSurfacePanel.this
            );
            Container newParent = findContainerAt(center, target);
            if (newParent == null) newParent = DesignSurfacePanel.this;

            if (newParent != target.getParent()) {
                // calculate new location
                Point loc = convertPointTo(newParent, center);
                loc.translate(-target.getWidth()/2, -target.getHeight()/2);

                // remove from old, add to new with constraint if needed
                Container oldParent = target.getParent();
                oldParent.remove(target);

                LayoutManager lm = newParent.getLayout();
                Object cons = target.getClientProperty("layoutConstraint");
                if (lm instanceof BorderLayout) {
                    String c = (cons instanceof String ? (String)cons : "Center");
                    newParent.add(target, c);
                } else {
                    newParent.add(target);
                }

                target.setLocation(loc);
                notifySelection(target);
            }

            notifyChange();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            PositionType positionType = (PositionType) target.getClientProperty("positionType");
            if (positionType == PositionType.RELATIVE) {
                // Disable dragging in RELATIVE mode
                return;
            }

            if (resizing) {
                int newW = Math.max(20, e.getX());
                int newH = Math.max(20, e.getY());
                target.setSize(newW, newH);
            } else {
                Point surfPt = SwingUtilities.convertPoint(target, e.getPoint(), DesignSurfacePanel.this);
                Point parentPt = SwingUtilities.convertPoint(DesignSurfacePanel.this, surfPt, target.getParent());

                Rectangle r = target.getBounds();
                if (dragOffset != null) {
                    r.setLocation(parentPt.x - dragOffset.x, parentPt.y - dragOffset.y);
                }
                target.setBounds(r);
            }
            notifyChange();
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            LayoutManager lm = target.getParent().getLayout();
            if (lm == null) {
                // show resize vs. move cursor in freeform mode
                Rectangle r = target.getBounds();
                if (e.getX() >= r.width - HANDLE && e.getY() >= r.height - HANDLE)
                    target.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                else
                    target.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            } else {
                // in managed mode, default cursor
                target.setCursor(Cursor.getDefaultCursor());
            }
        }
    }
}