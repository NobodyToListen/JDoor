package com.jdoor.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.plaf.basic.BasicHTML;
import org.junit.jupiter.api.Test;

class UiTest {
    @Test
    void headingsRenderUntrustedNamesAsPlainText() {
        String untrustedName = "<html><b>not markup</b></html>";

        JLabel heading = Ui.heading(untrustedName, 18);

        assertEquals(untrustedName, heading.getText());
        assertEquals(Boolean.TRUE, heading.getClientProperty("html.disable"));
        assertNull(heading.getClientProperty(BasicHTML.propertyKey));
    }

    @Test
    void buttonsKeepTheirKeyboardFocusIndicator() {
        JButton button = Ui.button("Continue", true);

        assertTrue(button.isFocusPainted());
    }
}
