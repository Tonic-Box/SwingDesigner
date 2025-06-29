package designer.util;

import designer.ui.CodeTabbedPane;
import designer.ui.DesignSurfacePanel;
import designer.ui.OutputConsole;
import designer.types.PositionType;

import javax.swing.*;
import javax.swing.border.*;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.awt.*;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class CodeManager
{
    private static int anonCount = 0;

    /**
     * 1) Wrap the user's code in a tiny helper class
     * 2) Invoke the JavaCompiler API
     * 3) Load it with a URLClassLoader
     * 4) Call its static apply(DesignSurfacePanel) method to mutate your live surface
     */
    public static void compileAndApply(JPanel panel, CodeTabbedPane codeView) {
        try {
            String className = "LiveDesign";
            // 1) Build full source with imports, reusing the existing root panel
            String src =
                    "import designer.ui.DesignSurfacePanel;\n" +
                            "import javax.swing.*;\n" +
                            "import java.awt.*;\n" +
                            "import java.awt.datatransfer.DataFlavor;\n" +
                            "import java.awt.dnd.*;\n" +
                            "import java.awt.event.MouseAdapter;\n" +
                            "import java.awt.event.MouseEvent;\n" +
                            "import java.awt.event.MouseListener;\n" +
                            "import java.awt.geom.Area;\n" +
                            "import java.util.*;\n" +
                            "import java.util.List;\n" +
                            "import java.util.concurrent.atomic.AtomicInteger;\n" +
                            "import java.util.stream.Collectors;\n" +
                            "public class " + className + " {\n" +
                            "  public static void apply(JPanel ds) throws Exception {\n" +
                            "    // reuse the existing designer panel as our root\n" +
                            "    JPanel panel = ds;\n" +
                            "    // clear out any old children\n" +
                            "    panel.removeAll();\n" +
                            // insert the user's layout code directly into the existing panel
                            codeView.getDesignerCode().replace("JPanel panel = new JPanel();", "") + "\n" +
                            (panel instanceof DesignSurfacePanel ? "" : codeView.getUserCode() + "\n") +
                            "    // refresh display\n" +
                            "    panel.revalidate(); panel.repaint();\n" +
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
                    panel.getClass().getClassLoader()
            )) {
                Class<?> liveCls = Class.forName(className, true, loader);
                Method m = liveCls.getMethod("apply", JPanel.class);
                m.invoke(null, panel);
                if( panel instanceof DesignSurfacePanel designPanel) {
                    designPanel.externalPropertyChanged();  // ensure the design surface is refreshed
                }
            }
            OutputConsole.info("Run successful!");
        } catch (Throwable ex) {
            ex.printStackTrace();
            OutputConsole.error("Run failed: " + ex.getMessage());
        }
    }

    public static String generateCode(DesignSurfacePanel panel) {
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

        emitContainer(panel, "panel", sb, true);
        return sb.toString();
    }

    /**
     * Recursively emits code for a container and *every* child component,
     * including preferred/minimum/maximum size calls.
     */
    private static void emitContainer(Container cont, String var, StringBuilder sb, boolean isRoot) {
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

            sb.append(id).append(".setName(\"").append(id).append("\");\n");

            // text if applicable
            if (jc instanceof AbstractButton ab && ab.getText() != null) {
                sb.append(id)
                        .append(".setText(\"")
                        .append(ab.getText().replace("\"", "\\\""))
                        .append("\");\n");
            }
            else if (jc instanceof JLabel lbl && lbl.getText() != null) {
                sb.append(id)
                        .append(".setText(\"")
                        .append(lbl.getText().replace("\"", "\\\""))
                        .append("\");\n");
            }
            else if (jc instanceof javax.swing.text.JTextComponent tc && tc.getText() != null) {
                sb.append(id)
                        .append(".setText(\"")
                        .append(tc.getText().replace("\"", "\\\""))
                        .append("\");\n");
            }
            else if (jc instanceof JComboBox<?> combo) {
                Object sel = combo.getSelectedItem();
                if (sel != null) {
                    sb.append(id)
                            .append(".setSelectedItem(\"")
                            .append(sel.toString().replace("\"", "\\\""))
                            .append("\");\n");
                }
            }
            else if (jc instanceof JSpinner spinner) {
                Object val = spinner.getValue();
                if (val != null) {
                    sb.append(id)
                            .append(".setValue(")
                            .append(val instanceof Number ? val : "\"" + val.toString().replace("\"", "\\\"") + "\"")
                            .append(");\n");
                }
            }

            Font font = jc.getFont();
            if (font != null) {
                sb.append(id)
                        .append(".setFont(new Font(\"")
                        .append(font.getFamily().replace("\"", "\\\""))
                        .append("\", ")
                        .append(fontStyleExpr(font.getStyle()))
                        .append(", ")
                        .append(font.getSize())
                        .append("));\n");
            }

            if(jc.getAutoscrolls())
            {
                sb.append(id).append(".setAutoscrolls(true);\n");
            }

            if(!jc.isEnabled())
            {
                sb.append(id).append(".setEnabled(false);\n");
            }

            Border border = jc.getBorder();
            String data = borderExpr(border);
            if (border != null && !data.isEmpty()) {
                sb.append(id)
                        .append(".setBorder(")
                        .append(borderExpr(border))
                        .append(");\n");
            }

            // layout
            sb.append(id).append(".setLayout(").append(layoutExpr(jc.getLayout())).append(");\n");

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

            if(!jc.isVisible())
            {
                sb.append(id).append(".setVisible(false);\n");
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

            if (cont.getLayout() instanceof GridBagLayout) {
                // 1) generate a fresh GridBagConstraints
                sb.append("GridBagConstraints ").append(id).append("Gbc = new GridBagConstraints();\n");
                GridBagConstraints gbc = ((GridBagLayout)cont.getLayout()).getConstraints(jc);
                // 2) emit each field
                sb.append(id).append("Gbc.gridx=").append(gbc.gridx).append(";\n");
                sb.append(id).append("Gbc.gridy=").append(gbc.gridy).append(";\n");
                sb.append(id).append("Gbc.gridwidth=").append(gbc.gridwidth).append(";\n");
                sb.append(id).append("Gbc.gridheight=").append(gbc.gridheight).append(";\n");
                sb.append(id).append("Gbc.weightx=").append(gbc.weightx).append(";\n");
                sb.append(id).append("Gbc.weighty=").append(gbc.weighty).append(";\n");
                sb.append(id).append("Gbc.fill=").append("GridBagConstraints.")
                        .append(fillName(gbc.fill)).append(";\n");
                sb.append(id).append("Gbc.anchor=").append("GridBagConstraints.")
                        .append(anchorName(gbc.anchor)).append(";\n");
                sb.append(id).append("Gbc.ipadx=").append(gbc.ipadx).append(";\n");
                sb.append(id).append("Gbc.ipady=").append(gbc.ipady).append(";\n");
                sb.append(id).append("Gbc.insets=new Insets(")
                        .append(gbc.insets.top).append(",").append(gbc.insets.left).append(",")
                        .append(gbc.insets.bottom).append(",").append(gbc.insets.right).append(");\n");
                // 3) add with constraints
                sb.append(var).append(".add(").append(id).append(", ").append(id).append("Gbc);\n\n");
            }
            else
            {
                Object cons = jc.getClientProperty("layoutConstraint");
                String constraint = cons!=null
                        ? "BorderLayout." + cons.toString().toUpperCase()
                        : "BorderLayout.CENTER";
                sb.append(var).append(".add(")
                        .append(id).append(", ").append(constraint)
                        .append(");\n\n");
            }

            // recurse
            if (jc.getComponentCount() > 0) {
                emitContainer(jc, id, sb, false);
            }
        }
    }

    /**
     * Convert Font style int into a Font.* constant expression.
     */
    private static String fontStyleExpr(int style) {
        return switch (style) {
            case Font.BOLD -> "Font.BOLD";
            case Font.ITALIC -> "Font.ITALIC";
            case Font.BOLD | Font.ITALIC -> "Font.BOLD | Font.ITALIC";
            default -> "Font.PLAIN";
        };
    }

    /**
     * Emit a BorderFactory expression for common Swing borders.
     */
    private static String borderExpr(Border b) {
        if (b instanceof LineBorder lb) {
            Color c = lb.getLineColor();
            int t = lb.getThickness();
            return "BorderFactory.createLineBorder(new Color(0x"
                    + String.format("%06X", c.getRGB() & 0xFFFFFF)
                    + "), " + t + ")";
        }
        else if (b instanceof EmptyBorder eb) {
            Insets i = eb.getBorderInsets();
            return "BorderFactory.createEmptyBorder("
                    + i.top + ", " + i.left + ", "
                    + i.bottom + ", " + i.right + ")";
        }
        else if (b instanceof EtchedBorder) {
            return "BorderFactory.createEtchedBorder()";
        }
        else if (b instanceof TitledBorder tb) {
            String title = tb.getTitle().replace("\"", "\\\"");
            return "BorderFactory.createTitledBorder(\"" + title + "\")";
        }
        else {
            // fallback for unhandled border types
            return "";
        }
    }


    private static String fillName(int code) {
        return switch(code){
            case GridBagConstraints.BOTH -> "BOTH";
            case GridBagConstraints.HORIZONTAL -> "HORIZONTAL";
            case GridBagConstraints.VERTICAL -> "VERTICAL";
            default -> "NONE";
        };
    }
    private static String anchorName(int code) {
        return switch(code){
            case GridBagConstraints.NORTH -> "NORTH";
            case GridBagConstraints.NORTHEAST -> "NORTHEAST";
            case GridBagConstraints.EAST -> "EAST";
            case GridBagConstraints.SOUTHEAST -> "SOUTHEAST";
            case GridBagConstraints.SOUTH -> "SOUTH";
            case GridBagConstraints.SOUTHWEST -> "SOUTHWEST";
            case GridBagConstraints.WEST -> "WEST";
            case GridBagConstraints.NORTHWEST -> "NORTHWEST";
            default -> "CENTER";
        };
    }

    private static String layoutExpr(LayoutManager lm) {
        if (lm == null) return "null";
        if (lm instanceof FlowLayout)   return "new FlowLayout()";
        if (lm instanceof BorderLayout) return "new BorderLayout()";
        if (lm instanceof GridLayout g) return "new GridLayout(" +
                g.getRows() + "," + g.getColumns() + ")";
        if (lm instanceof GridBagLayout) return "new GridBagLayout()";
        /* fall-back */                 return "null";
    }
}
