package designer.ui.componants;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaHighlighter;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

public class ExRSyntaxTextArea extends RSyntaxTextArea
{
    public ExRSyntaxTextArea(int rows, int cols) {
        super(rows, cols);

        setTheme();
        setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
        setCodeFoldingEnabled(true);
        setCodeFoldingEnabled(true);
        setAnimateBracketMatching(true);
        setAutoIndentEnabled(true);
        setHighlighter(new RSyntaxTextAreaHighlighter());

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    handleRightClick(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    handleRightClick(e);
                }
            }

            private void handleRightClick(MouseEvent e) {
                if (getSelectedText() == null) {
                    int offset = viewToModel2D(e.getPoint());
                    setCaretPosition(offset);
                }
            }
        });
    }

    private void setTheme()
    {
        try
        {
            Theme theme = Theme.load(getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/dark.xml"));
            theme.apply(this);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }
}