package panchayat;

import panchayat.db.DatabaseManager;
import panchayat.view.MainFrame;

import javax.swing.*;

/**
 * Main — Application entry point for the Digital Panchayat Management System.
 *
 * Startup sequence:
 *  1. Load the SQLite JDBC driver (no-op for JDBC 4+ but explicit for clarity)
 *  2. Initialise the database (create tables if first run)
 *  3. Switch to the Swing Event Dispatch Thread before creating any UI
 *     — mandatory per the Swing threading model to avoid random EDT violations.
 *  4. Apply Nimbus Look & Feel for a modern cross-platform appearance
 *     that improves on the default Metal L&F without needing native theming.
 *
 * HOW TO COMPILE AND RUN (from PanchayatSystem/ directory):
 *
 *   javac -cp lib/sqlite-jdbc-*.jar -sourcepath src \
 *         -d out src/panchayat/Main.java
 *
 *   java -cp out:lib/sqlite-jdbc-*.jar panchayat.Main
 *
 * On Windows replace ':' with ';' in the classpath.
 */
public class Main {

    public static void main(String[] args) {
        // Step 1: Apply Nimbus L&F before any UI component is created
        applyLookAndFeel();

        // Step 2: Initialise DB (safe to call every startup — uses IF NOT EXISTS)
        try {
            DatabaseManager.createTables();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Failed to initialise database:\n" + e.getMessage() +
                "\n\nMake sure the SQLite JDBC driver (sqlite-jdbc-*.jar) is on the classpath.",
                "Database Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Step 3: Launch UI on the EDT
        SwingUtilities.invokeLater(() -> new panchayat.view.LoginFrame().setVisible(true));
    }

    private static void applyLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    // Override Nimbus defaults to match our dark theme
                    UIManager.put("control",         new java.awt.Color(30, 41, 59));
                    UIManager.put("text",            new java.awt.Color(248, 250, 252));
                    UIManager.put("nimbusBase",      new java.awt.Color(15, 23, 42));
                    UIManager.put("nimbusBlueGrey",  new java.awt.Color(51, 65, 85));
                    UIManager.put("nimbusFocus",     new java.awt.Color(99, 102, 241));
                    UIManager.put("TabbedPane.background", new java.awt.Color(15, 23, 42));
                    break;
                }
            }
        } catch (Exception e) {
            // Nimbus not available — fall back to system L&F gracefully
            System.err.println("[Main] Nimbus L&F not available, using default.");
        }
    }
}
