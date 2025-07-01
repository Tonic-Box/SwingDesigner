package designer.ui;

import designer.ui.componants.ExRSyntaxTextArea;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Utilities;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Predicate;

public class CodeViewPanel extends JPanel {
    private final ExRSyntaxTextArea area;
    private MouseAdapter workClickListener = null;
    private Predicate<String> onWordClick;
    private final JScrollPane scrollPane;

    public CodeViewPanel(){
        super(new BorderLayout());
        setBorder(new EmptyBorder(5,5,5,5));

        // — top bar with label + run button —
        JPanel top = new JPanel(new BorderLayout());
        add(top, BorderLayout.NORTH);

        // — code area —
        area = new ExRSyntaxTextArea(50, 60);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setEditable(true);
        scrollPane = new JScrollPane(area);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setOnWordClick(Predicate<String> callback) {
        if( workClickListener != null) {
            area.removeMouseListener(workClickListener);
        }
        this.onWordClick = callback;

        area.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // only handle left-clicks
                if (e.getButton() != MouseEvent.BUTTON1) return;
                // map point to document offset
                int offs = area.viewToModel2D(e.getPoint());
                try {
                    // find word start & end
                    int wordStart = Utilities.getWordStart(area, offs);
                    int wordEnd   = Utilities.getWordEnd(area, offs);
                    String word   = area.getDocument()
                            .getText(wordStart, wordEnd - wordStart);

                    if (onWordClick != null && !word.trim().isEmpty()) {
                        String sub = word.split("\\.")[0];
                        if(onWordClick.test(sub))
                        {
                            area.select(wordStart, wordStart + sub.length());
                        }
                    }
                } catch (BadLocationException ex) {
                    OutputConsole.error("Error getting word at mouse click: " + ex);
                }
            }
        });
    }

    /**
     * Replace the editor text, attempting to preserve scroll position:
     * 1) record the top 3 visible lines (by content)
     * 2) record current scroll percentage
     * 3) set new text
     * 4) try to locate the previous top lines and scroll them back to the top
     * 5) if not found, restore by percentage
     * 6) if that fails, scroll to top
     */
    public void setCode(String code) {
        if(code.equals(getCode())) return; // no change
        // run in EDT to safely query & update viewport
        SwingUtilities.invokeLater(() -> {
            JViewport viewport = scrollPane.getViewport();
            Point viewPos = viewport.getViewPosition();

            // 1) capture the first 3 visible lines
            String[] topLines = new String[3];
            try {
                int firstOffset = area.viewToModel2D(new Point(0, viewPos.y));
                int firstLine = area.getLineOfOffset(firstOffset);
                for (int i = 0; i < 3; i++) {
                    int idx = firstLine + i;
                    if (idx < area.getLineCount()) {
                        int start = area.getLineStartOffset(idx);
                        int end   = area.getLineEndOffset(idx);
                        topLines[i] = area.getDocument().getText(start, end - start);
                    } else {
                        topLines[i] = null;
                    }
                }
            } catch (BadLocationException e) {
                // fallback to empty
            }

            // 2) capture scroll %
            JScrollBar vert = scrollPane.getVerticalScrollBar();
            int max = vert.getMaximum() - vert.getVisibleAmount();
            float pct = max > 0 ? (float) vert.getValue() / max : 0f;

            // 3) set the new text
            area.setText(code);

            // 4) restore scroll position
            SwingUtilities.invokeLater(() -> {
                boolean restored = false;
                try {
                    // search for matching top-lines block
                    outer:
                    for (int line = 0; line < area.getLineCount(); line++) {
                        for (int j = 0; j < 3; j++) {
                            String txt = topLines[j];
                            if (txt == null) continue;
                            int idx = line + j;
                            if (idx >= area.getLineCount()) continue outer;
                            int s = area.getLineStartOffset(idx);
                            int e = area.getLineEndOffset(idx);
                            String cur = area.getDocument().getText(s, e - s);
                            if (!cur.equals(txt)) continue outer;
                        }
                        // match found; scroll to this line
                        int offs = area.getLineStartOffset(line);
                        Rectangle r = area.modelToView2D(offs).getBounds();
                        viewport.setViewPosition(new Point(0, r.y));
                        restored = true;
                        break;
                    }
                } catch (BadLocationException ignored) {}

                if (!restored) {
                    // 5) restore by percentage or 6) top
                    if (max > 0) {
                        vert.setValue(Math.round(pct * max));
                    } else {
                        vert.setValue(0);
                    }
                }
            });
        });
    }

    public String getCode(){
        return area.getText();
    }

    public void setEditable(boolean editable) {
        area.setEditable(editable);
    }

    public void setSelectionColor(Color background, Color text) {
        area.setSelectionColor(background);
        area.setSelectedTextColor(text);
        area.setUseSelectedTextColor(true);
    }
}
