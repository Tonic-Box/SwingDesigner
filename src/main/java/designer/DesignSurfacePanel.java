package designer;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DesignSurfacePanel extends JPanel implements DropTargetListener {
    private boolean gridEnabled = false;
    private final int GRID_SPAN = 10;
    private JComponent selectedComp = null;
    private final List<DesignChangeListener> changeL = new ArrayList<>();
    private final List<SelectionListener>    selectL = new ArrayList<>();
    private final AtomicInteger idSeq   = new AtomicInteger();

    DesignSurfacePanel() {
        super(null);
        setBackground(new Color(SwingDesignerApp.BG_DARK.getRGB()));
        setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        new DropTarget(this, DnDConstants.ACTION_COPY, this, true);

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                Component hit = getComponentAt(e.getPoint());
                // allow the panel itself to be selected too
                if( hit instanceof JComponent ) {
                    selectedComp = (JComponent)hit;
                } else {
                    selectedComp = null;
                }
                notifySelection(selectedComp);
            }
        });
    }

    /** Toggle grid on/off */
    public void setGridEnabled(boolean on) {
        this.gridEnabled = on;
        repaint();
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

    /** Nudge selected by dx/dy */
    public void nudgeSelection(int dx, int dy) {
        if (selectedComp != null) {
            Rectangle r = selectedComp.getBounds();
            r.translate(dx, dy);
            if (gridEnabled) {
                r.x = Math.round(r.x / (float)GRID_SPAN) * GRID_SPAN;
                r.y = Math.round(r.y / (float)GRID_SPAN) * GRID_SPAN;
            }
            selectedComp.setBounds(r);
            notifyChange();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!gridEnabled) return;
        Graphics2D g2 = (Graphics2D)g.create();
        g2.setColor(new Color(100,100,100,40));
        int w = getWidth(), h = getHeight();
        for (int x = 0; x < w; x += GRID_SPAN)
            g2.drawLine(x, 0, x, h);
        for (int y = 0; y < h; y += GRID_SPAN)
            g2.drawLine(0, y, w, y);
        g2.dispose();
    }

    private void snapBounds(Rectangle r) {
        r.x      = Math.round(r.x / (float)GRID_SPAN) * GRID_SPAN;
        r.y      = Math.round(r.y / (float)GRID_SPAN) * GRID_SPAN;
        r.width  = Math.max(GRID_SPAN, Math.round(r.width  / (float)GRID_SPAN) * GRID_SPAN);
        r.height = Math.max(GRID_SPAN, Math.round(r.height / (float)GRID_SPAN) * GRID_SPAN);
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
     *  3) fire our designChanged listeners.
     */
    void externalPropertyChanged() {
        ensureUniqueNames();
        reapplyConstraints(this);
        notifyChange();
        //notifySelection(selectedComp);
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
    void addDesignChangeListener(DesignChangeListener l){ changeL.add(l);}
    void addSelectionListener   (SelectionListener    l){ selectL.add(l);}
    private void notifyChange()   { revalidate(); repaint(); changeL.forEach(DesignChangeListener::designChanged);}
    private void notifySelection(Component c){ selectL .forEach(l -> l.selectionChanged(c)); }

    /* ───── Simple code generation (Java Swing) ───── */
    String generateCode() {
        StringBuilder sb = new StringBuilder("// ---- auto-generated layout ----\n");
        emitContainer(this, "panel", sb, true);
        return sb.toString();
    }

    /* recursive helper */
    private void emitContainer(Container cont, String var, StringBuilder sb, boolean isRoot) {
        if (isRoot) {
            sb.append("JPanel ").append(var).append(" = new JPanel();\n");
            // emit the root layout too:
            sb.append(var).append(".setLayout(")
                    .append(layoutExpr(cont.getLayout()))
                    .append(");\n\n");
        } else {
            sb.append(var).append(".setLayout(").append(layoutExpr(cont.getLayout())).append(");\n");
            Color bg = cont.getBackground();
            if (bg != null)
                sb.append(var).append(".setBackground(new Color(0x")
                        .append(String.format("%06X", bg.getRGB() & 0xFFFFFF))
                        .append("));\n");
        }
        for (Component c : cont.getComponents()) {
            if (!(c instanceof JComponent jc)) continue;
            String id = jc.getName();
            sb.append(jc.getClass().getSimpleName()).append(' ').append(id)
                    .append(" = new ").append(jc.getClass().getSimpleName()).append("();\n");

            if (jc instanceof AbstractButton but && but.getText() != null)
                sb.append(id).append(".setText(\"")
                        .append(but.getText().replace("\"","\\\"")).append("\");\n");

            sb.append(id).append(".setLayout(")
                    .append(layoutExpr(jc.getLayout())).append(");\n");

            Color bg = jc.getBackground(), fg = jc.getForeground();
            if (bg != null)
                sb.append(id).append(".setBackground(new Color(0x")
                        .append(String.format("%06X", bg.getRGB() & 0xFFFFFF))
                        .append("));\n");
            if (fg != null)
                sb.append(id).append(".setForeground(new Color(0x")
                        .append(String.format("%06X", fg.getRGB() & 0xFFFFFF))
                        .append("));\n");

            PositionType posType = (PositionType) jc.getClientProperty("positionType");

            if (posType == null) posType = PositionType.ABSOLUTE;

            if (posType == PositionType.ABSOLUTE) {
                Rectangle r = jc.getBounds();
                sb.append(id).append(".setBounds(")
                        .append(r.x).append(',').append(r.y).append(',')
                        .append(r.width).append(',').append(r.height).append(");\n");
            }

            Object cons = jc.getClientProperty("layoutConstraint");
            String constraint = (cons != null
                    ? "BorderLayout." + cons.toString().toUpperCase()
                    : "BorderLayout.CENTER");
            sb.append(var).append(".add(")
                    .append(id).append(", ").append(constraint).append(");\n\n");

            // recurse into children (every JComponent is a Container)
            if (jc.getComponentCount() > 0) {
                emitContainer((Container) jc, id, sb, false);
            }
        }
    }


    /* ───── helper classes ───── */
    @FunctionalInterface interface DesignChangeListener { void designChanged(); }
    @FunctionalInterface interface SelectionListener    { void selectionChanged(Component c); }

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
        private void maybeShowPopup(MouseEvent e){
            if (e.isPopupTrigger()){
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
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
                if (gridEnabled) snapBounds(r);
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