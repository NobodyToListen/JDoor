package com.jdoor;

import com.jdoor.cli.CommandLine;
import com.jdoor.ui.HostFrame;
import com.jdoor.ui.LauncherFrame;
import com.jdoor.ui.Theme;
import com.jdoor.ui.ViewerFrame;
import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;

public final class JDoorApplication {
    private JDoorApplication() {}

    public static void main(String[] arguments) {
        CommandLine commandLine;
        try {
            commandLine = CommandLine.parse(arguments);
        } catch (IllegalArgumentException invalidArguments) {
            System.err.println("Error: " + invalidArguments.getMessage());
            System.err.println();
            System.err.print(CommandLine.help());
            System.exit(2);
            return;
        }

        switch (commandLine.mode()) {
            case HELP -> {
                System.out.print(CommandLine.help());
                return;
            }
            case VERSION -> {
                System.out.println(AppInfo.NAME + " " + AppInfo.version());
                return;
            }
            default -> {
                if (GraphicsEnvironment.isHeadless()) {
                    System.err.println("JDoor Assist requires a graphical desktop. Use --help for CLI usage.");
                    System.exit(1);
                    return;
                }
            }
        }

        Theme.install();
        SwingUtilities.invokeLater(() -> {
            switch (commandLine.mode()) {
                case HOST -> new HostFrame(commandLine.port(), commandLine.advertisedHost()).setVisible(true);
                case JOIN -> new ViewerFrame(commandLine.pairingLink()).setVisible(true);
                case LAUNCHER -> new LauncherFrame().setVisible(true);
                default -> throw new IllegalStateException("Unexpected UI mode");
            }
        });
    }
}
