package panchayat.view;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * MainFrame — Root application window.
 *
 * Uses a JTabbedPane rather than CardLayout because the tab strip gives
 * office staff an always-visible navigation bar — they don't need to know
 * a menu system exists to switch modules.
 *
 * The DashboardPanel reference is kept so we can call refreshStats() when
 * the user switches back to the Dashboard tab (catches changes made in
 * other modules during the same session).
 */
public class MainFrame extends JFrame {

    private DashboardPanel dashboardPanel;
    private boolean isGov;

    // Sidebar navigation constants
    private static final Color BG_DARK    = new Color(15, 23, 42);
    private static final Color SIDEBAR_BG = new Color(9, 15, 30);
    private static final Color ACCENT     = new Color(99, 102, 241);   // indigo
    private static final Color TEXT_WHITE = new Color(248, 250, 252);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);

    public MainFrame(boolean isGov) {
        super("Digital Panchayat Management System" + (isGov ? " - Government Portal" : " - Public Portal"));
        this.isGov = isGov;
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setPreferredSize(new Dimension(1280, 800));

        // Ask for confirmation before closing to prevent accidental data loss
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int res = JOptionPane.showConfirmDialog(MainFrame.this,
                    "Exit Panchayat Management System?",
                    "Confirm Exit", JOptionPane.YES_NO_OPTION);
                if (res == JOptionPane.YES_OPTION) {
                    panchayat.db.DatabaseManager.closeConnection();
                    dispose();
                    System.exit(0);
                }
            }
        });

        buildUI();
        pack();
        setLocationRelativeTo(null); // Center on screen
        setVisible(true);
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_DARK);

        // ── Sidebar ─────────────────────────────────────────────────────
        JPanel sidebar = buildSidebar();
        add(sidebar, BorderLayout.WEST);

        // ── Tabbed content area ─────────────────────────────────────────
        JTabbedPane tabs = buildTabbedPane();
        add(tabs, BorderLayout.CENTER);

        // ── Status bar at the bottom ─────────────────────────────────────
        add(buildStatusBar(), BorderLayout.SOUTH);

        // Refresh dashboard stats whenever user switches back to tab 0
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 0 && dashboardPanel != null) {
                dashboardPanel.refreshStats();
            }
        });
    }

    // ─── Left sidebar (branding + navigation hint) ───────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                // Gradient from top-left to bottom of sidebar
                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(30, 27, 75),
                    0, getHeight(), new Color(9, 15, 30)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(28, 16, 20, 16));

        // App icon / logo area
        JLabel logo = new JLabel("🏛") {
            @Override public Dimension getPreferredSize() { return new Dimension(60, 60); }
        };
        logo.setFont(new Font("SansSerif", Font.PLAIN, 40));
        logo.setAlignmentX(CENTER_ALIGNMENT);

        JLabel appName = new JLabel("<html><center>Digital<br>Panchayat</center></html>");
        appName.setFont(new Font("SansSerif", Font.BOLD, 16));
        appName.setForeground(TEXT_WHITE);
        appName.setAlignmentX(CENTER_ALIGNMENT);

        JLabel appVer = new JLabel("v1.0");
        appVer.setFont(new Font("SansSerif", Font.PLAIN, 11));
        appVer.setForeground(TEXT_MUTED);
        appVer.setAlignmentX(CENTER_ALIGNMENT);

        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(appName);
        sidebar.add(appVer);
        sidebar.add(Box.createVerticalStrut(30));

        // Divider
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(51, 65, 85));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sidebar.add(sep);
        sidebar.add(Box.createVerticalStrut(20));

        // Nav menu labels (decorative — actual navigation is via tabs)
        String[] navItems = isGov ? 
            new String[]{"📊 Dashboard", "🏠 Complaints", "🔧 Issues", "📅 Meetings"} : 
            new String[]{"📊 Dashboard", "🏠 Complaints"};
            
        for (String item : navItems) {
            JLabel nav = new JLabel(item);
            nav.setFont(new Font("SansSerif", Font.PLAIN, 13));
            nav.setForeground(TEXT_MUTED);
            nav.setAlignmentX(LEFT_ALIGNMENT);
            nav.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
            sidebar.add(nav);
            sidebar.add(Box.createVerticalStrut(2));
        }

        sidebar.add(Box.createVerticalGlue());

        // Footer
        JLabel footer = new JLabel("<html><center>Gram Panchayat<br>Office System</center></html>");
        footer.setFont(new Font("SansSerif", Font.PLAIN, 10));
        footer.setForeground(new Color(71, 85, 105));
        footer.setAlignmentX(CENTER_ALIGNMENT);
        sidebar.add(footer);

        // Right border separator
        sidebar.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 0, 1, new Color(30, 41, 59)),
            BorderFactory.createEmptyBorder(28, 16, 20, 16)
        ));

        return sidebar;
    }

    // ─── Custom styled tabbed pane ────────────────────────────────────────
    private JTabbedPane buildTabbedPane() {
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP) {
            @Override
            public void updateUI() {
                super.updateUI();
                // Override tab UI after L&F update
            }
        };

        // Apply dark background to the tabbed pane itself
        tabs.setBackground(BG_DARK);
        tabs.setForeground(TEXT_WHITE);
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabs.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Build panels
        dashboardPanel      = new DashboardPanel();
        ComplaintPanel cpanel = new ComplaintPanel(isGov);

        tabs.addTab("📊  Dashboard",   dashboardPanel);
        tabs.addTab("🏠  Complaints",  cpanel);
        tabs.setToolTipTextAt(0, "Summary Dashboard — overview statistics");
        tabs.setToolTipTextAt(1, "Citizen Complaint Portal — register and view complaints");

        if (isGov) {
            IssuePanel     ipanel = new IssuePanel();
            MeetingPanel   mpanel = new MeetingPanel();
            tabs.addTab("🔧  Issues",      ipanel);
            tabs.addTab("📅  Meetings",    mpanel);
            tabs.setToolTipTextAt(1, "Citizen Complaint Portal — register and manage complaints");
            tabs.setToolTipTextAt(2, "Infrastructure Issue Tracker — log and monitor field issues");
            tabs.setToolTipTextAt(3, "Meeting Records Manager — record and search Panchayat meetings");
        }

        return tabs;
    }

    // ─── Bottom status bar ─────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(9, 15, 30));
        bar.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, new Color(30, 41, 59)),
            BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));

        JLabel left = new JLabel("Digital Panchayat Management System  •  SQLite Local DB  •  Ready");
        left.setFont(new Font("SansSerif", Font.PLAIN, 11));
        left.setForeground(TEXT_MUTED);

        // Show current date on the right
        JLabel right = new JLabel("Date: " + java.time.LocalDate.now());
        right.setFont(new Font("SansSerif", Font.PLAIN, 11));
        right.setForeground(TEXT_MUTED);

        bar.add(left,  BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }
}
