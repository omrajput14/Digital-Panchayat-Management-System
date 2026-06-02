package panchayat.view;

import panchayat.dao.ComplaintDAO;
import panchayat.dao.IssueDAO;
import panchayat.dao.MeetingDAO;
import panchayat.model.Complaint;
import panchayat.model.Issue;
import panchayat.util.ReportExporter;
import panchayat.util.PdfExporter;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.SQLException;

/**
 * DashboardPanel — The home screen of the application.
 *
 * Shows at-a-glance statistics without requiring staff to navigate into
 * each module. A "Refresh" button lets them update stats after changes
 * made in other tabs — simpler than an auto-polling timer for a
 * lightweight desktop app.
 *
 * Design decision: Stat cards are custom-painted JPanels rather than
 * plain labels, giving each metric a visually distinct "tile" look that
 * non-technical users can scan at a glance.
 */
public class DashboardPanel extends JPanel {

    private final ComplaintDAO complaintDAO = new ComplaintDAO();
    private final IssueDAO issueDAO = new IssueDAO();
    private final MeetingDAO meetingDAO = new MeetingDAO();

    // Card labels — kept as fields so refreshStats() can update them
    private JLabel lblOpenComplaints;
    private JLabel lblPendingComplaints;
    private JLabel lblResolvedComplaints;
    private JLabel lblLowIssues;
    private JLabel lblMediumIssues;
    private JLabel lblHighIssues;
    private JLabel lblMeetingsThisMonth;
    private JLabel lblPendingActionItems;
    private JLabel lblTotalIssues;

    // ── Colour palette ──────────────────────────────────────────────────
    private static final Color BG_DARK = new Color(15, 23, 42); // slate-900
    private static final Color BG_CARD = new Color(30, 41, 59); // slate-800
    private static final Color ACCENT_GREEN = new Color(34, 197, 94); // green-500
    private static final Color ACCENT_AMBER = new Color(245, 158, 11); // amber-500
    private static final Color ACCENT_RED = new Color(239, 68, 68); // red-500
    private static final Color ACCENT_BLUE = new Color(59, 130, 246); // blue-500
    private static final Color ACCENT_INDIGO = new Color(99, 102, 241); // indigo-500
    private static final Color ACCENT_TEAL = new Color(20, 184, 166); // teal-500
    private static final Color TEXT_WHITE = new Color(248, 250, 252);
    private static final Color TEXT_MUTED = new Color(148, 163, 184);

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // ── Top header bar ──────────────────────────────────────────────
        JPanel header = buildHeader();
        add(header, BorderLayout.NORTH);

        // ── Scrollable card grid ────────────────────────────────────────
        JPanel grid = buildCardGrid();
        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(null);
        scroll.setBackground(BG_DARK);
        scroll.getViewport().setBackground(BG_DARK);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // Load initial data
        refreshStats();
    }

    // ── Header ───────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_DARK);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel title = new JLabel("📊  Summary Dashboard");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(TEXT_WHITE);

        JLabel subtitle = new JLabel("Live overview of Panchayat operations");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_MUTED);

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 2));
        left.setBackground(BG_DARK);
        left.add(title);
        left.add(subtitle);

        JButton refreshBtn = createStyledButton("⟳  Refresh", ACCENT_BLUE);
        refreshBtn.addActionListener(e -> refreshStats());

        JButton printBtn = createStyledButton("🖨  Print Report", new Color(245, 158, 11));
        printBtn.addActionListener(e -> printFullReport());

        JButton pdfBtn = createStyledButton("📄  Save as PDF", new Color(220, 38, 38));
        pdfBtn.addActionListener(e -> savePdfReport());

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnGroup.setBackground(BG_DARK);
        btnGroup.add(pdfBtn);
        btnGroup.add(printBtn);
        btnGroup.add(refreshBtn);

        p.add(left, BorderLayout.WEST);
        p.add(btnGroup, BorderLayout.EAST);
        return p;
    }

    // ── Card Grid ────────────────────────────────────────────────────────
    private JPanel buildCardGrid() {
        JPanel container = new JPanel();
        container.setBackground(BG_DARK);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        // --- Section: Complaints ---
        container.add(sectionLabel("🏠  Citizen Complaints"));
        container.add(Box.createVerticalStrut(10));
        JPanel complaintRow = new JPanel(new GridLayout(1, 3, 16, 0));
        complaintRow.setBackground(BG_DARK);
        complaintRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        lblOpenComplaints = new JLabel("—");
        lblPendingComplaints = new JLabel("—");
        lblResolvedComplaints = new JLabel("—");

        complaintRow.add(buildStatCard("Total Open", lblOpenComplaints, ACCENT_AMBER, "⚠"));
        complaintRow.add(buildStatCard("Pending", lblPendingComplaints, ACCENT_RED, "🕐"));
        complaintRow.add(buildStatCard("Resolved", lblResolvedComplaints, ACCENT_GREEN, "✔"));
        container.add(complaintRow);
        container.add(Box.createVerticalStrut(24));

        // --- Section: Infrastructure Issues ---
        container.add(sectionLabel("🔧  Infrastructure Issues"));
        container.add(Box.createVerticalStrut(10));
        JPanel issueRow = new JPanel(new GridLayout(1, 3, 16, 0));
        issueRow.setBackground(BG_DARK);
        issueRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        lblLowIssues = new JLabel("—");
        lblMediumIssues = new JLabel("—");
        lblHighIssues = new JLabel("—");
        lblTotalIssues = new JLabel("—");

        issueRow.add(buildStatCard("Low Severity", lblLowIssues, ACCENT_TEAL, "🟢"));
        issueRow.add(buildStatCard("Medium Severity", lblMediumIssues, ACCENT_AMBER, "🟡"));
        issueRow.add(buildStatCard("High Severity", lblHighIssues, ACCENT_RED, "🔴"));
        container.add(issueRow);
        container.add(Box.createVerticalStrut(24));

        // --- Section: Meetings ---
        container.add(sectionLabel("📅  Meeting Records"));
        container.add(Box.createVerticalStrut(10));
        JPanel meetingRow = new JPanel(new GridLayout(1, 2, 16, 0));
        meetingRow.setBackground(BG_DARK);
        meetingRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        lblMeetingsThisMonth = new JLabel("—");
        lblPendingActionItems = new JLabel("—");

        meetingRow.add(buildStatCard("Meetings This Month", lblMeetingsThisMonth, ACCENT_INDIGO, "📋"));
        meetingRow.add(buildStatCard("Pending Action Items", lblPendingActionItems, ACCENT_AMBER, "📌"));
        container.add(meetingRow);
        container.add(Box.createVerticalStrut(24));

        return container;
    }

    // ── Stat card factory ─────────────────────────────────────────────────
    /**
     * Builds a single coloured tile with an icon, title, and live value label.
     * The left border stripe uses the accent colour for quick visual scanning.
     */
    private JPanel buildStatCard(String title, JLabel valueLabel, Color accent, String icon) {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Accent stripe on the left edge
                g.setColor(accent);
                g.fillRoundRect(0, 0, 5, getHeight(), 5, 5);
            }
        };
        card.setBackground(BG_CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(51, 65, 85), 1, true),
                BorderFactory.createEmptyBorder(16, 20, 16, 16)));

        JLabel iconLbl = new JLabel(icon + "  " + title);
        iconLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        iconLbl.setForeground(TEXT_MUTED);

        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        valueLabel.setForeground(accent);

        card.add(iconLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    // ── Section label ─────────────────────────────────────────────────────
    private JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        lbl.setForeground(TEXT_MUTED);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        return lbl;
    }

    // ── Refresh statistics from DB ────────────────────────────────────────
    /**
     * Queries all three DAOs and updates the label text on the EDT.
     * Errors are silently printed — the dashboard is read-only and a
     * temporary DB hiccup should not crash the whole UI.
     */
    public void refreshStats() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Complaints
                int pending = complaintDAO.countByStatus(Complaint.STATUS_PENDING);
                int inProg = complaintDAO.countByStatus(Complaint.STATUS_IN_PROGRESS);
                int resolved = complaintDAO.countByStatus(Complaint.STATUS_RESOLVED);
                int openTotal = pending + inProg;

                lblOpenComplaints.setText(String.valueOf(openTotal));
                lblPendingComplaints.setText(String.valueOf(pending));
                lblResolvedComplaints.setText(String.valueOf(resolved));

                // Issues
                lblLowIssues.setText(String.valueOf(issueDAO.countBySeverity("Low")));
                lblMediumIssues.setText(String.valueOf(issueDAO.countBySeverity("Medium")));
                lblHighIssues.setText(String.valueOf(issueDAO.countBySeverity("High")));

                // Meetings
                lblMeetingsThisMonth.setText(String.valueOf(meetingDAO.countThisMonth()));
                lblPendingActionItems.setText(String.valueOf(meetingDAO.countPendingActionItems()));

            } catch (SQLException ex) {
                System.err.println("[Dashboard] Stats refresh failed: " + ex.getMessage());
            }
        });
    }

    /**
     * Assembles a consolidated text report of all live dashboard statisticsrun the
     * code base
     * and sends it to the system print dialog (macOS: Save as PDF available).
     */
    private void printFullReport() {
        try {
            int pending = complaintDAO.countByStatus(Complaint.STATUS_PENDING);
            int inProg = complaintDAO.countByStatus(Complaint.STATUS_IN_PROGRESS);
            int resolved = complaintDAO.countByStatus(Complaint.STATUS_RESOLVED);

            StringBuilder sb = new StringBuilder();
            sb.append("SECTION: Citizen Complaints Summary\n\n");
            sb.append(String.format("  ▶ Total Open (Pending + In Progress) : %d%n", pending + inProg));
            sb.append(String.format("  ▶ Pending                            : %d%n", pending));
            sb.append(String.format("  ▶ In Progress                        : %d%n", inProg));
            sb.append(String.format("  ▶ Resolved                           : %d%n\n", resolved));

            sb.append("─".repeat(60)).append("\n");
            sb.append("SECTION: Infrastructure Issues by Severity\n\n");
            sb.append(String.format("  ▶ Low Severity (open)    : %d%n", issueDAO.countBySeverity("Low")));
            sb.append(String.format("  ▶ Medium Severity (open) : %d%n", issueDAO.countBySeverity("Medium")));
            sb.append(String.format("  ▶ High Severity (open)   : %d%n", issueDAO.countBySeverity("High")));
            sb.append(String.format("  ▶ Total Open Issues       : %d%n\n", issueDAO.countOpen()));

            sb.append("─".repeat(60)).append("\n");
            sb.append("SECTION: Meeting Records\n\n");
            sb.append(String.format("  ▶ Meetings held this month : %d%n", meetingDAO.countThisMonth()));
            sb.append(String.format("  ▶ Pending action items      : %d%n\n", meetingDAO.countPendingActionItems()));

            ReportExporter.printReport("Full Panchayat Summary Report", sb.toString(), this);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error generating report: " + ex.getMessage(),
                    "Report Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Save full dashboard summary directly as a PDF file. */
    private void savePdfReport() {
        try {
            int pending = complaintDAO.countByStatus(Complaint.STATUS_PENDING);
            int inProg = complaintDAO.countByStatus(Complaint.STATUS_IN_PROGRESS);
            int resolved = complaintDAO.countByStatus(Complaint.STATUS_RESOLVED);

            StringBuilder sb = new StringBuilder();
            sb.append("SECTION: Citizen Complaints Summary\n\n");
            sb.append(String.format("  > Total Open (Pending + In Progress) : %d%n", pending + inProg));
            sb.append(String.format("  > Pending                            : %d%n", pending));
            sb.append(String.format("  > In Progress                        : %d%n", inProg));
            sb.append(String.format("  > Resolved                           : %d%n\n", resolved));
            sb.append("─".repeat(60)).append("\n");
            sb.append("SECTION: Infrastructure Issues by Severity\n\n");
            sb.append(String.format("  > Low Severity (open)    : %d%n", issueDAO.countBySeverity("Low")));
            sb.append(String.format("  > Medium Severity (open) : %d%n", issueDAO.countBySeverity("Medium")));
            sb.append(String.format("  > High Severity (open)   : %d%n", issueDAO.countBySeverity("High")));
            sb.append(String.format("  > Total Open Issues       : %d%n\n", issueDAO.countOpen()));
            sb.append("─".repeat(60)).append("\n");
            sb.append("SECTION: Meeting Records\n\n");
            sb.append(String.format("  > Meetings held this month : %d%n", meetingDAO.countThisMonth()));
            sb.append(String.format("  > Pending action items      : %d%n\n", meetingDAO.countPendingActionItems()));

            PdfExporter.savePdf("Full Panchayat Summary Report", sb.toString(), this);
        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error generating PDF: " + ex.getMessage(),
                    "PDF Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? bg.darker() : getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(140, 38));
        return btn;
    }
}
