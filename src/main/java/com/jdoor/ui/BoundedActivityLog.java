package com.jdoor.ui;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

final class BoundedActivityLog {
    static final int MAX_LINES = 200;
    static final int MAX_CHARACTERS = 32 * 1024;

    private BoundedActivityLog() {}

    static void append(JTextArea area, String entry) {
        area.append(entry);
        trim(area.getDocument(), MAX_LINES, MAX_CHARACTERS);
    }

    static void trim(Document document, int maxLines, int maxCharacters) {
        try {
            Element root = document.getDefaultRootElement();
            while (root.getElementCount() > maxLines + 1) {
                Element oldestLine = root.getElement(0);
                document.remove(0, oldestLine.getEndOffset());
            }
            int excess = document.getLength() - maxCharacters;
            if (excess > 0) {
                document.remove(0, excess);
            }
        } catch (BadLocationException impossible) {
            throw new IllegalStateException("Could not trim the activity log", impossible);
        }
    }
}
