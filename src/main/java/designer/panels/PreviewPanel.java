package designer.panels;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;

public class PreviewPanel extends JPanel {
    private final DesignSurfacePanel designSurface;
    private final JPanel canvas = new JPanel(null);
    public PreviewPanel(DesignSurfacePanel ds){
        super(new BorderLayout());
        this.designSurface=ds;

        designSurface.addDesignChangeListener(this::refresh);

        setPreferredSize(new Dimension(380,0));
        setBorder(new EmptyBorder(5,5,5,5));
        //add(new JLabel("Live Preview",SwingConstants.CENTER), BorderLayout.NORTH);
        add(new JScrollPane(canvas), BorderLayout.CENTER);
        refresh();
    }
    public void refresh() {
        canvas.removeAll();
        canvas.setBackground(designSurface.getBackground());
        canvas.setLayout(designSurface.getLayout());
        try { cloneInto(designSurface, canvas); }
        catch (Exception ex) { ex.printStackTrace(); }
        canvas.revalidate(); canvas.repaint();
    }

    /* deep copy subtree */
    private void cloneInto(Container src, Container dst) throws Exception {
        for (Component child : src.getComponents()) {
            Component copy = cloneComponent(child);
            LayoutManager lm = dst.getLayout();
            if (lm instanceof BorderLayout) {
                Object cons = ((JComponent)child).getClientProperty("layoutConstraint");
                String cstr = cons!=null ? cons.toString() : "Center";
                dst.add(copy, cstr);
            } else {
                dst.add(copy);
            }
            if (child instanceof Container sc && copy instanceof Container dc)
                cloneInto(sc, dc);
        }
    }

    private Component cloneComponent(Component original) throws Exception {
        Constructor<?> ctor = original.getClass().getConstructor();
        Component copy = (Component) ctor.newInstance();
        copy.setBounds(original.getBounds());
        BeanInfo info = Introspector.getBeanInfo(original.getClass(),Object.class);
        for(PropertyDescriptor pd:info.getPropertyDescriptors()){
            if(pd.getReadMethod()!=null && pd.getWriteMethod()!=null){
                try{ pd.getWriteMethod().invoke(copy, pd.getReadMethod().invoke(original)); }
                catch(Exception ignored){}
            }
        }
        if (original instanceof JComponent origJC && copy instanceof JComponent copyJC) {
            Object maybe = origJC.getClientProperty("savedPopup");
            if (maybe instanceof JPopupMenu pm) {
                copyJC.setComponentPopupMenu(pm);
            }
        }
        return copy;
    }
}