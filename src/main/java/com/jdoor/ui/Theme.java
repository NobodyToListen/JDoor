package com.jdoor.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.Color;
import java.awt.Font;
import javax.swing.UIManager;

public final class Theme {
    public static final Color BACKGROUND = new Color(0x0B0D10);
    public static final Color SURFACE = new Color(0x15191F);
    public static final Color SURFACE_RAISED = new Color(0x1C222A);
    public static final Color TEXT = new Color(0xF4F1EA);
    public static final Color MUTED = new Color(0xA7AFBA);
    public static final Color ACCENT = new Color(0xFF6B35);
    public static final Color ACCENT_SOFT = new Color(0x3A2118);
    public static final Color SUCCESS = new Color(0x45C486);
    public static final Color WARNING = new Color(0xF6C85F);
    public static final Color BORDER = new Color(0x303842);

    private Theme() {}

    public static void install() {
        FlatDarkLaf.setup();
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Component.focusColor", ACCENT);
        UIManager.put("Component.focusWidth", 2);
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 12);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("TitlePane.unifiedBackground", true);
        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 15));
    }
}
