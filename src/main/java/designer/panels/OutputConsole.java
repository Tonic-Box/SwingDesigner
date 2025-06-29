package designer.panels;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

public class OutputConsole
{
    private static final JTextPane pane = new JTextPane();
    private static final StyledDocument doc = pane.getStyledDocument();
    private static final Style infoStyle;
    private static final Style errorStyle;
    private static final JScrollPane scrollPane;

    static
    {
        // pane setup
        pane.setEditable(false);
        pane.setBackground(Color.BLACK);
        pane.setForeground(Color.WHITE);
        pane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        // create styles
        Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

        infoStyle    = doc.addStyle("info",    defaultStyle);
        StyleConstants.setForeground(infoStyle,    Color.LIGHT_GRAY);

        errorStyle   = doc.addStyle("error",   defaultStyle);
        StyleConstants.setForeground(errorStyle,   Color.RED);

        // popup menu
        JPopupMenu popup = new JPopupMenu();
        JMenuItem clear = new JMenuItem("Clear Console");
        clear.addActionListener(e -> {
            pane.setText("");
        });
        popup.add(clear);
        pane.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    popup.show(pane, e.getX(), e.getY());
                }
            }
        });

        // wrap in scroll pane
        scrollPane = new JScrollPane(pane);
        scrollPane.setPreferredSize(new Dimension(0, 150));  // width=auto, height=150
    }

    /** Returns the console scroll pane. */
    public static JScrollPane generateConsoleScrollPane()
    {
        return scrollPane;
    }

    /** Append a line using the given style. */
    private static void append(String msg, Style style)
    {
        try
        {
            doc.insertString(doc.getLength(), msg + "\n", style);
            pane.setCaretPosition(doc.getLength());
        }
        catch (BadLocationException ex)
        {
            ex.printStackTrace();
        }
    }

    /** Info (light gray) */
    public static void info(String msg)
    {
        append(msg, infoStyle);
    }

    /** Error (red) */
    public static void error(String msg)
    {
        append(msg, errorStyle);
    }
}
