package com.jdoor.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class BoundedActivityLogTest {
    @Test
    void retainsOnlyTheNewestConfiguredNumberOfLines() throws Exception {
        AtomicReference<String> content = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            JTextArea area = new JTextArea();
            for (int index = 0; index < BoundedActivityLog.MAX_LINES + 25; index++) {
                BoundedActivityLog.append(area, "activity-" + index + "\n");
            }
            content.set(area.getText());
        });

        assertTrue(content.get().lines().count() <= BoundedActivityLog.MAX_LINES);
        assertFalse(content.get().contains("activity-0\n"));
        assertTrue(content.get().contains("activity-" + (BoundedActivityLog.MAX_LINES + 24)));
    }

    @Test
    void capsCharactersWhileKeepingTheNewestContent() throws Exception {
        AtomicReference<String> content = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            JTextArea area = new JTextArea();
            BoundedActivityLog.append(area, "x".repeat(BoundedActivityLog.MAX_CHARACTERS + 100) + "newest-entry\n");
            content.set(area.getText());
        });

        assertTrue(content.get().length() <= BoundedActivityLog.MAX_CHARACTERS);
        assertTrue(content.get().endsWith("newest-entry\n"));
    }
}
