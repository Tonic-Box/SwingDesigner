package designer.ui.componants;

import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Set;

public class PropertyTableModel extends AbstractTableModel {
    private Component target;
    private PropertyDescriptor[] props = new PropertyDescriptor[0];

    private final Runnable onEdit;             // NEW

    private final Set<String> hiddenProperties = Set.of(
            "UI", "UIClassID", "accessibleContext", "actionMap",
            "ancestorListeners", "baselineResizeBehavior", "component", "componentCount",
            "components", "containerListeners", "debugGraphicsOption", "focusTraversalKeys",
            "focusTraversalPolicy", "focusTraversalPolicyProvider", "focusTraversalPolicySet", "graphics",
            "gridEnabled", "inputMap", "inputVerifier", "nextFocusableComponent", "registeredKeyStrokes",
            "rootPane", "topLevelAncestor", "transferHandler", "vetoableChangeListeners",
            "visibleRect"
    );

    public PropertyTableModel(Runnable onEdit){ this.onEdit = onEdit; }

    @Override public int getRowCount(){ return props.length; }
    @Override public int getColumnCount(){ return 2; }
    @Override public String getColumnName(int col){ return col==0?"Property":"Value"; }

    @Override public Object getValueAt(int r,int c){
        if(target==null) return null;
        PropertyDescriptor pd = props[r];
        try{ return c==0?pd.getName():pd.getReadMethod().invoke(target);}catch(Exception e){return e.toString();}
    }

    @Override public boolean isCellEditable(int r,int c){ return c==1 && props[r].getWriteMethod()!=null; }

    @Override
    public void setValueAt(Object v,int r,int c){
        if (target==null || c!=1) return;

        PropertyDescriptor pd   = props[r];
        Class<?> pType          = pd.getPropertyType();
        Object   value;

        if (v == null) {
            value = null;
        }
        // direct instances from custom editors
        else if (pType.isInstance(v)) {
            value = v;
        }
        else if (pType==Color.class && v instanceof Color) {
            value = v;
        }
        else if (pType==LayoutManager.class && v instanceof LayoutManager) {
            value = v;
        }
        else if ((pType==Boolean.class||pType==boolean.class) && v instanceof Boolean) {
            value = v;
        }
        else {
            value = coerce(v.toString(), pType);
        }

        try{
            // always invoke setter, even if value==null
            pd.getWriteMethod().invoke(target, value);
            fireTableCellUpdated(r,c);
            onEdit.run();
        }catch(Exception ignored){}
    }

    /** expose the currently selected component */
    public Component getTarget() {
        return target;
    }

    public PropertyDescriptor getPropertyDescriptor(int row) {
        return (row >= 0 && row < props.length) ? props[row] : null;
    }

    public void setTarget(Component comp) {
        this.target = comp;
        if (comp == null) {
            props = new PropertyDescriptor[0];
        } else {
            try {
                props = Arrays.stream(Introspector.getBeanInfo(comp.getClass(), Object.class).getPropertyDescriptors())
                        .filter(pd -> !hiddenProperties.contains(pd.getName()))
                        .filter(pd -> !pd.getName().toLowerCase().contains("listener"))
                        .toArray(PropertyDescriptor[]::new);
            } catch (IntrospectionException e) {
                props = new PropertyDescriptor[0];
            }
        }
        fireTableStructureChanged();
    }

    /* trivial string→primitive coercion */
    private Object coerce(String s, Class<?> type){
        if(type==String.class) return s;

        if(type==Color.class){
            try{ return Color.decode(s.trim()); }catch(Exception ignored){}
            return null;
        }

        try{
            if(type==int.class||type==Integer.class) return Integer.parseInt(s);
            if(type==boolean.class||type==Boolean.class) return Boolean.parseBoolean(s);
            if(type==float.class||type==Float.class) return Float.parseFloat(s);
            if(type==double.class||type==Double.class) return Double.parseDouble(s);
            if(type==long.class||type==Long.class) return Long.parseLong(s);
        }catch(Exception ignored){}
        if (LayoutManager.class.isAssignableFrom(type)) {
            return switch (s.trim().toLowerCase()) {
                case "null", "absolute"       -> null;                         // absolute positioning
                case "flow", "flowlayout"     -> new FlowLayout();
                case "border", "borderlayout" -> new BorderLayout();
                case "grid", "gridlayout"     -> new GridLayout();
                default -> null;  // unsupported name → ignore
            };
        }
        return null;
    }
}