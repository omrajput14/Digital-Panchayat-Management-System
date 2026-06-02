package panchayat.util;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.*;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * ReportExporter — Static utility class providing two export capabilities:
 *
 *  1. exportTableToCSV(JTable, Component)
 *     Writes every visible row/column of a JTable to a user-chosen CSV file.
 *     Uses JFileChooser so staff pick the save location without CLI knowledge.
 *
 *  2. printReport(String title, String body, Component parent)
 *     Sends a formatted text report to the system print dialog.
 *     On macOS the dialog has a built-in "Save as PDF" option — so this
 *     doubles as a PDF export with zero extra libraries.
 *
 * Design decision: No third-party PDF libraries (iText, Apache POI etc.) are
 * used so the project stays dependency-free beyond the SQLite JDBC JAR.
 * Java's java.awt.print API is universally available on all JDK versions >= 8.
 */
public final class ReportExporter {

    // Prevent instantiation — all methods are static
    private ReportExporter() {}

    // ═══════════════════════════════════════════════════════════════════════
    // 1. CSV Export
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Opens a save dialog and exports the given JTable's content to a CSV file.
     *
     * @param table   The JTable whose data should be exported
     * @param parent  Parent component for centering the dialogs
     */
    public static void exportTableToCSV(JTable table, Component parent) {
        // Use JFileChooser — friendlier for non-technical staff than typing a path
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save CSV Export");
        chooser.setSelectedFile(new File("panchayat_export_" +
            LocalDate.now().toString() + ".csv"));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "CSV Files (*.csv)", "csv"));

        int result = chooser.showSaveDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        // Append .csv if user forgot the extension
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getAbsolutePath() + ".csv");
        }

        DefaultTableModel model = (DefaultTableModel) table.getModel();
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            // Write header row
            List<String> headers = new ArrayList<>();
            for (int col = 0; col < model.getColumnCount(); col++) {
                headers.add(escapeCsv(model.getColumnName(col)));
            }
            pw.println(String.join(",", headers));

            // Write data rows
            for (int row = 0; row < model.getRowCount(); row++) {
                List<String> cells = new ArrayList<>();
                for (int col = 0; col < model.getColumnCount(); col++) {
                    Object val = model.getValueAt(row, col);
                    cells.add(escapeCsv(val == null ? "" : val.toString()));
                }
                pw.println(String.join(",", cells));
            }

            JOptionPane.showMessageDialog(parent,
                "✅  Exported " + model.getRowCount() + " rows to:\n" + file.getAbsolutePath(),
                "Export Successful", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent,
                "Failed to write CSV file:\n" + e.getMessage(),
                "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Wraps a CSV cell value in quotes and escapes internal quotes.
     * Follows RFC 4180: double-quote any field containing comma/quote/newline.
     */
    private static String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 2. Print / PDF Report
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Renders a formatted text report through the system print dialog.
     *
     * On macOS the print dialog includes a "PDF → Save as PDF" button,
     * giving staff an easy one-click PDF save with no extra dependencies.
     *
     * @param title   Report title shown at the top of every page
     * @param body    The multi-line text content of the report
     * @param parent  Parent component for the print dialog
     */
    public static void printReport(String title, String body, Component parent) {
        // Build the full content with header and footer metadata
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
        String fullContent = buildReportContent(title, body, timestamp);

        // Split into lines for rendering on paginated pages
        String[] lines = fullContent.split("\n");

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Panchayat Report — " + title);

        PageFormat pf = job.defaultPage();
        pf.setOrientation(PageFormat.PORTRAIT);

        // Printable renders one page at a time
        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            // Constants for layout
            final int LINE_HEIGHT   = 16;
            final int MARGIN_LEFT   = 40;
            final int HEADER_HEIGHT = 60;
            final int FOOTER_HEIGHT = 30;

            double pageWidth  = pageFormat.getImageableWidth();
            double pageHeight = pageFormat.getImageableHeight();
            int usableHeight  = (int) pageHeight - HEADER_HEIGHT - FOOTER_HEIGHT;
            int linesPerPage  = usableHeight / LINE_HEIGHT;

            int totalPages = (int) Math.ceil((double) lines.length / linesPerPage);
            if (pageIndex >= totalPages) return Printable.NO_SUCH_PAGE;

            Graphics2D g2 = (Graphics2D) graphics;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            // ── Header ────────────────────────────────────────────
            g2.setColor(new Color(20, 30, 80));
            g2.fillRect(0, 0, (int) pageWidth, 48);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2.drawString("🏛  Digital Panchayat Management System", MARGIN_LEFT, 20);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.drawString(title, MARGIN_LEFT, 38);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.drawString("Generated: " + timestamp, (int) pageWidth - 200, 38);

            // ── Body ──────────────────────────────────────────────
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Monospaced", Font.PLAIN, 10));

            int startLine = pageIndex * linesPerPage;
            int endLine   = Math.min(startLine + linesPerPage, lines.length);
            int y = HEADER_HEIGHT + LINE_HEIGHT;

            for (int i = startLine; i < endLine; i++) {
                String line = lines[i];
                // Render separator lines as horizontal rules
                if (line.startsWith("═") || line.startsWith("─")) {
                    g2.setColor(new Color(100, 100, 200));
                    g2.drawLine(MARGIN_LEFT, y - 4, (int) pageWidth - MARGIN_LEFT, y - 4);
                    g2.setColor(Color.BLACK);
                } else if (line.startsWith("  ▶") || line.startsWith("SECTION:")) {
                    // Section headers in bold
                    g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                    g2.setColor(new Color(30, 60, 150));
                    g2.drawString(line, MARGIN_LEFT, y);
                    g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
                    g2.setColor(Color.BLACK);
                } else {
                    g2.drawString(line, MARGIN_LEFT, y);
                }
                y += LINE_HEIGHT;
            }

            // ── Footer ────────────────────────────────────────────
            int footerY = (int) pageHeight - FOOTER_HEIGHT;
            g2.setColor(new Color(200, 200, 200));
            g2.drawLine(0, footerY, (int) pageWidth, footerY);
            g2.setColor(Color.GRAY);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
            g2.drawString("Gram Panchayat Office — Confidential", MARGIN_LEFT, footerY + 14);
            g2.drawString("Page " + (pageIndex + 1) + " of " + totalPages,
                (int) pageWidth - 80, footerY + 14);

            return Printable.PAGE_EXISTS;
        }, pf);

        // Show system print dialog (macOS: includes "Save as PDF")
        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException e) {
                JOptionPane.showMessageDialog(parent,
                    "Print failed: " + e.getMessage(),
                    "Print Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** Builds the formatted text body of the report. */
    private static String buildReportContent(String title, String body, String timestamp) {
        return "═".repeat(70) + "\n" +
               "  DIGITAL PANCHAYAT MANAGEMENT SYSTEM\n" +
               "  Report: " + title + "\n" +
               "  Generated: " + timestamp + "\n" +
               "═".repeat(70) + "\n\n" +
               body + "\n\n" +
               "─".repeat(70) + "\n" +
               "  End of Report — Gram Panchayat Office\n" +
               "─".repeat(70);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 3. Validation Helper (used by all panels)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Highlights empty required fields with a red border and returns false
     * if any are empty. Clears the border on non-empty fields.
     *
     * Usage in panels:
     *   if (!ReportExporter.validateFields(this, tfName, tfWard)) return;
     *
     * @param parent Parent component for error dialog positioning
     * @param fields JTextField or JTextArea components to validate
     * @return true if all fields have non-blank content, false otherwise
     */
    public static boolean validateRequiredFields(Component parent, JComponent... fields) {
        boolean valid = true;
        List<String> emptyLabels = new ArrayList<>();

        for (JComponent field : fields) {
            String text = "";
            if (field instanceof JTextField tf) {
                text = tf.getText().trim();
            } else if (field instanceof JTextArea ta) {
                text = ta.getText().trim();
            }

            if (text.isEmpty()) {
                // Red border to visually flag the empty field
                field.setBorder(new javax.swing.border.CompoundBorder(
                    new javax.swing.border.LineBorder(new Color(239, 68, 68), 2, true),
                    javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8)
                ));
                valid = false;
            } else {
                // Restore normal border when filled
                field.setBorder(new javax.swing.border.CompoundBorder(
                    new javax.swing.border.LineBorder(new Color(71, 85, 105), 1, true),
                    javax.swing.BorderFactory.createEmptyBorder(5, 8, 5, 8)
                ));
            }
        }

        if (!valid) {
            JOptionPane.showMessageDialog(parent,
                "Please fill in all required fields (highlighted in red).",
                "Validation Error", JOptionPane.WARNING_MESSAGE);
        }
        return valid;
    }

    /**
     * Validates a date string is in YYYY-MM-DD format and is a real calendar date.
     *
     * @param dateStr  The string to validate
     * @param field    The JTextField to highlight red on failure (may be null)
     * @param parent   Parent component for error dialog
     * @return true if valid
     */
    public static boolean validateDate(String dateStr, JTextField field, Component parent) {
        try {
            java.time.LocalDate.parse(dateStr); // throws DateTimeParseException if invalid
            if (field != null) {
                field.setBorder(new javax.swing.border.CompoundBorder(
                    new javax.swing.border.LineBorder(new Color(71, 85, 105), 1, true),
                    javax.swing.BorderFactory.createEmptyBorder(5, 8, 5, 8)));
            }
            return true;
        } catch (java.time.format.DateTimeParseException e) {
            if (field != null) {
                field.setBorder(new javax.swing.border.CompoundBorder(
                    new javax.swing.border.LineBorder(new Color(239, 68, 68), 2, true),
                    javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8)));
            }
            JOptionPane.showMessageDialog(parent,
                "'" + dateStr + "' is not a valid date.\nPlease use YYYY-MM-DD format (e.g., 2024-07-15).",
                "Invalid Date", JOptionPane.WARNING_MESSAGE);
            return false;
        }
    }

    /**
     * Validates that a ward number is non-empty and reasonably formatted
     * (1-10 alphanumeric characters, may include hyphen).
     */
    public static boolean validateWard(String ward, JTextField field, Component parent) {
        if (ward == null || ward.isBlank()) {
            highlightError(field);
            JOptionPane.showMessageDialog(parent, "Ward number is required.",
                "Validation Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (!ward.matches("[A-Za-z0-9\\-]{1,15}")) {
            highlightError(field);
            JOptionPane.showMessageDialog(parent,
                "Ward number '" + ward + "' is invalid.\n" +
                "Use up to 15 alphanumeric characters (e.g., W-12 or 7B).",
                "Invalid Ward", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        clearError(field);
        return true;
    }

    // ── Border helpers ─────────────────────────────────────────────────────
    public static void highlightError(JComponent field) {
        if (field == null) return;
        field.setBorder(new javax.swing.border.CompoundBorder(
            new javax.swing.border.LineBorder(new Color(239, 68, 68), 2, true),
            javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8)));
    }

    public static void clearError(JComponent field) {
        if (field == null) return;
        field.setBorder(new javax.swing.border.CompoundBorder(
            new javax.swing.border.LineBorder(new Color(71, 85, 105), 1, true),
            javax.swing.BorderFactory.createEmptyBorder(5, 8, 5, 8)));
    }
}
