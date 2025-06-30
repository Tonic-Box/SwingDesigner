package designer.ui;

import designer.util.ModelBuilder;
import designer.SwingDesignerApp;
import designer.model.*;
import designer.types.PositionType;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Area;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DesignSurfacePanel extends JPanel implements DropTargetListener {
    private JComponent selectedComp = null;
    private final List<DesignChangeListener> changeL = new ArrayList<>();
    private final List<SelectionListener>    selectL = new ArrayList<>();
    private final AtomicInteger idSeq   = new AtomicInteger();
    private boolean snapToGrid = false;
    private boolean lockComponents = false;
    private int     gridSize   = 10;
    private Color   gridColor  = new Color(200, 200, 200, 64);

    public DesignSurfacePanel() {
        super(null);
        setName("panel");
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

    public void setLockComponents(boolean lock)
    {
        this.lockComponents = lock;
    }

    /** Called by DesignerFrame when the user toggles Snap to Grid */
    public void setSnapToGrid(boolean s) {
        this.snapToGrid = s;
    }
    public void setGridSize(int sz) {
        this.gridSize = sz;
    }
    public void setGridColor(Color c) {
        this.gridColor = c;
    }

    public Color getGridColor() { return gridColor; }

    public int getGridSize() {
        return gridSize;
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

        if (snapToGrid && gridSize >= 2 && selectedComp != null) {
            Container host = selectedComp.getParent();
            if (host != null && host.getLayout() == null) {
                Point origin = SwingUtilities.convertPoint(host, 0, 0, this);
                int w = host.getWidth(), h = host.getHeight();

                List<Rectangle> children = new ArrayList<>();
                for (Component c : host.getComponents()) {
                    if (c instanceof JComponent jc) {
                        Rectangle r = SwingUtilities.convertRectangle(
                                jc.getParent(), jc.getBounds(), this
                        );
                        children.add(r);
                    }
                }

                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    // build a clip region: host bounds MINUS the selRect
                    Area clip = new Area(new Rectangle(origin.x, origin.y, w, h));
                    for(Rectangle r : children) {
                        if (r.intersects(origin.x, origin.y, w, h)) {
                            clip.subtract(new Area(r));
                        }
                    }
                    g2.setClip(clip);

                    g2.setColor(gridColor);
                    for (int x = origin.x; x <= origin.x + w; x += gridSize)
                        g2.drawLine(x, origin.y, x, origin.y + h);
                    for (int y = origin.y; y <= origin.y + h; y += gridSize)
                        g2.drawLine(origin.x, y, origin.x + w, y);
                } finally {
                    g2.dispose();
                }
            }
        }

        // ...then draw your orange selection rect as before
        if (selectedComp != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(Color.ORANGE);
                float[] dash = {4f,4f};
                g2.setStroke(new BasicStroke(
                        2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, dash, 0f
                ));
                Rectangle r = SwingUtilities.convertRectangle(
                        selectedComp.getParent(),
                        selectedComp.getBounds(),
                        this
                );
                g2.drawRect(r.x, r.y, r.width-1, r.height-1);
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
    public void installDragResizeBehavior(JComponent c) {
        for(MouseListener l : c.getMouseListeners()) {
            if (l instanceof MoveResizeAdapter) {
                return;
            }
        }
        MoveResizeAdapter adapter = new MoveResizeAdapter(c);
        c.addMouseListener(adapter);
        c.addMouseMotionListener(adapter);
    }

    /* ───── Observers ───── */
    public void addDesignChangeListener(DesignChangeListener l){ changeL.add(l);}
    public void addSelectionListener(SelectionListener l){ selectL.add(l); selectL.add(c -> repaint());}
    private void notifyChange()   { revalidate(); repaint(); changeL.forEach(DesignChangeListener::designChanged);}
    private void notifySelection(Component c){ selectL .forEach(l -> l.selectionChanged(c)); }

    public void importProject(ProjectData proj) throws Exception {
        removeAll();
        ModelBuilder.rebuildFromData(this, proj.root);
        revalidate();
        repaint();
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

            JMenuItem alignRight = new JMenuItem("Right");
            alignRight.addActionListener(e -> {
                Rectangle r = target.getBounds();
                int parentW = target.getParent().getWidth();
                r.x = parentW - r.width;
                target.setBounds(r);
                notifyChange();
            });
            alignMenu.add(alignRight);

            JMenuItem alignTop = new JMenuItem("Top");
            alignTop.addActionListener(e -> {
                Rectangle r = target.getBounds();
                r.y = 0;
                target.setBounds(r);
                notifyChange();
            });
            alignMenu.add(alignTop);

            JMenuItem alignBottom = new JMenuItem("Bottom");
            alignBottom.addActionListener(e -> {
                Rectangle r = target.getBounds();
                int parentH = target.getParent().getHeight();
                r.y = parentH - r.height;
                target.setBounds(r);
                notifyChange();
            });
            alignMenu.add(alignBottom);

            JMenuItem alignCenter = new JMenuItem("Center Horizontally");
            alignCenter.addActionListener(e -> {
                Rectangle r = target.getBounds();
                int parentW = target.getParent().getWidth();
                r.x = (parentW - r.width) / 2;
                target.setBounds(r);
                notifyChange();
            });
            alignMenu.add(alignCenter);

            JMenuItem alignMiddle = new JMenuItem("Center Vertically");
            alignMiddle.addActionListener(e -> {
                Rectangle r = target.getBounds();
                int parentH = target.getParent().getHeight();
                r.y = (parentH - r.height) / 2;
                target.setBounds(r);
                notifyChange();
            });
            alignMenu.add(alignMiddle);

            popup.add(alignMenu);
        }

        /* helper: show popup across platforms */
        private void maybeShowPopup(MouseEvent e) {
            if (!e.isPopupTrigger()) return;
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

            if(lockComponents) {
                // if components are locked, do not allow any dragging or resizing
                return;
            }

            /* re-parent if dropped over a different panel */
            Point center = SwingUtilities.convertPoint(
                    target,
                    target.getWidth()/2, target.getHeight()/2,
                    DesignSurfacePanel.this
            );
            Container newParent = findContainerAt(center, target);
            if (newParent == null) newParent = DesignSurfacePanel.this;

            if (newParent != target.getParent() && newParent != target && !SwingUtilities.isDescendingFrom(newParent, target)) {
                // calculate new location
                Point loc = convertPointTo(newParent, center);
                loc.translate(-target.getWidth() / 2, -target.getHeight() / 2);

                // remove from old, add to new with constraint if needed
                Container oldParent = target.getParent();
                oldParent.remove(target);

                LayoutManager lm = newParent.getLayout();
                Object cons = target.getClientProperty("layoutConstraint");
                if (lm instanceof BorderLayout) {
                    String c = (cons instanceof String ? (String) cons : "Center");
                    newParent.add(target, c);
                }
                else {
                    newParent.add(target);
                }

                target.setLocation(loc);
                notifySelection(target);
                System.out.println();
            }

            notifyChange();
        }

        @Override
        public void mouseDragged(MouseEvent e) {

            if (dragOffset == null || lockComponents) {
                return;
            }

            PositionType positionType = (PositionType) target.getClientProperty("positionType");
            if (positionType == PositionType.RELATIVE) {
                // Disable dragging in RELATIVE mode
                return;
            }

            if (resizing) {
                int newW = Math.max(20, e.getX());
                int newH = Math.max(20, e.getY());

                // snap to grid when appropriate
                if (snapToGrid && target.getParent().getLayout() == null) {
                    newW = Math.round((float)newW / gridSize) * gridSize;
                    newH = Math.round((float)newH / gridSize) * gridSize;
                }

                target.setSize(newW, newH);
            } else {
                // compute the raw new location in parent coords
                Point surfPt   = SwingUtilities.convertPoint(target, e.getPoint(), DesignSurfacePanel.this);
                Point parentPt = SwingUtilities.convertPoint(DesignSurfacePanel.this, surfPt, target.getParent());
                Rectangle r    = target.getBounds();

                int newX = parentPt.x - dragOffset.x;
                int newY = parentPt.y - dragOffset.y;

                // only snap when parent layout is null AND snapToGrid is ON
                if (target.getParent().getLayout() == null && snapToGrid) {
                    newX = Math.round((float)newX / gridSize) * gridSize;
                    newY = Math.round((float)newY / gridSize) * gridSize;
                }

                target.setBounds(newX, newY, r.width, r.height);
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