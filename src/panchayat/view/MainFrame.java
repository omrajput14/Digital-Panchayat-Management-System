package panchayat.view;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class MainFrame extends JFrame {

    private DashboardPanel dashboardPanel;
    private boolean isGov;
    private JTabbedPane tabs;
    private JButton[] navButtons;

    private static final Color BG_DARK     = new Color(15, 23, 42);
    private static final Color ACCENT      = new Color(99, 102, 241);
    private static final Color TEXT_WHITE  = new Color(248, 250, 252);
    private static final Color TEXT_MUTED  = new Color(100, 116, 139);
    private static final Color NAV_HOVER   = new Color(30, 41, 59);
    private static final Color NAV_ACTIVE  = new Color(49, 46, 129);

    public MainFrame(boolean isGov) {
        super("Digital Panchayat Management System" + (isGov ? " - Government Portal" : " - Public Portal"));
        this.isGov = isGov;
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setPreferredSize(new Dimension(1280, 800));

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
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_DARK);

        // Build tabs first (needed by sidebar buttons)
        tabs = buildTabbedPane();

        JPanel sidebar = buildSidebar();
        add(sidebar, BorderLayout.WEST);
        add(tabs, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        // Sync sidebar highlight when tab is changed via tab bar
        tabs.addChangeListener(e -> {
            setActiveButton(tabs.getSelectedIndex());
            if (tabs.getSelectedIndex() == 0 && dashboardPanel != null) {
                dashboardPanel.refreshStats();
            }
        });

        // Start with Dashboard highlighted
        setActiveButton(0);
    }

    // ── Sidebar ──────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(30, 27, 75), 0, getHeight(), new Color(9, 15, 30)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 0, 1, new Color(30, 41, 59)),
            BorderFactory.createEmptyBorder(28, 12, 20, 12)
        ));

        // Logo
        JLabel logo = new JLabel("🏛");
        logo.setFont(new Font("SansSerif", Font.PLAIN, 40));
        logo.setAlignmentX(CENTER_ALIGNMENT);

        JLabel appName = new JLabel("<html><center>Digital<br>Panchayat</center></html>");
        appName.setFont(new Font("SansSerif", Font.BOLD, 16));
        appName.setForeground(TEXT_WHITE);
        appName.setAlignmentX(CENTER_ALIGNMENT);

        JLabel roleTag = new JLabel(isGov ? "⚙ Government Portal" : "👥 Public Portal");
        roleTag.setFont(new Font("SansSerif", Font.BOLD, 11));
        roleTag.setForeground(isGov ? new Color(167, 139, 250) : new Color(96, 165, 250));
        roleTag.setAlignmentX(CENTER_ALIGNMENT);

        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(appName);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(roleTag);
        sidebar.add(Box.createVerticalStrut(24));

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(51, 65, 85));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sidebar.add(sep);
        sidebar.add(Box.createVerticalStrut(16));

        // Nav section label
        JLabel navLabel = new JLabel("NAVIGATION");
        navLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        navLabel.setForeground(new Color(71, 85, 105));
        navLabel.setAlignmentX(LEFT_ALIGNMENT);
        navLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 8, 0));
        sidebar.add(navLabel);

        // Nav buttons
        String[] navItems = isGov
            ? new String[]{"📊  Dashboard", "🏠  Complaints", "🔧  Issues", "📅  Meetings"}
            : new String[]{"📊  Dashboard", "🏠  Complaints"};

        navButtons = new JButton[navItems.length];
        for (int i = 0; i < navItems.length; i++) {
            final int tabIndex = i;
            JButton btn = createNavButton(navItems[i]);
            btn.addActionListener(e -> {
                tabs.setSelectedIndex(tabIndex);
                setActiveButton(tabIndex);
            });
            navButtons[i] = btn;
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(4));
        }

        sidebar.add(Box.createVerticalGlue());

        // Footer
        JLabel footer = new JLabel("<html><center>Gram Panchayat<br>Office System  •  v1.0</center></html>");
        footer.setFont(new Font("SansSerif", Font.PLAIN, 10));
        footer.setForeground(new Color(71, 85, 105));
        footer.setAlignmentX(CENTER_ALIGNMENT);
        sidebar.add(footer);

        return sidebar;
    }

    private JButton createNavButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg;
                if (getClientProperty("active") != null && (boolean) getClientProperty("active")) {
                    bg = NAV_ACTIVE;
                } else if (getModel().isRollover()) {
                    bg = NAV_HOVER;
                } else {
                    bg = new Color(0, 0, 0, 0);
                }
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                // Active left accent bar
                if (getClientProperty("active") != null && (boolean) getClientProperty("active")) {
                    g2.setColor(ACCENT);
                    g2.fillRoundRect(0, 6, 4, getHeight() - 12, 4, 4);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btn.setForeground(TEXT_MUTED);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setPreferredSize(new Dimension(186, 40));
        btn.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 8));

        // Hover color change
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (btn.getClientProperty("active") == null || !(boolean) btn.getClientProperty("active"))
                    btn.setForeground(TEXT_WHITE);
                btn.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                if (btn.getClientProperty("active") == null || !(boolean) btn.getClientProperty("active"))
                    btn.setForeground(TEXT_MUTED);
                btn.repaint();
            }
        });
        return btn;
    }

    private void setActiveButton(int index) {
        if (navButtons == null) return;
        for (int i = 0; i < navButtons.length; i++) {
            boolean active = (i == index);
            navButtons[i].putClientProperty("active", active);
            navButtons[i].setForeground(active ? TEXT_WHITE : TEXT_MUTED);
            navButtons[i].setFont(new Font("SansSerif", active ? Font.BOLD : Font.PLAIN, 13));
            navButtons[i].repaint();
        }
    }

    // ── Tabbed pane ───────────────────────────────────────────────────────
    private JTabbedPane buildTabbedPane() {
        JTabbedPane t = new JTabbedPane(JTabbedPane.TOP);
        t.setBackground(BG_DARK);
        t.setForeground(TEXT_WHITE);
        t.setFont(new Font("SansSerif", Font.BOLD, 13));
        t.setBorder(BorderFactory.createEmptyBorder());

        dashboardPanel = new DashboardPanel();
        ComplaintPanel cpanel = new ComplaintPanel(isGov);

        t.addTab("📊  Dashboard",  dashboardPanel);
        t.addTab("🏠  Complaints", cpanel);
        t.setToolTipTextAt(0, "Summary statistics");
        t.setToolTipTextAt(1, "Citizen complaint portal");

        if (isGov) {
            t.addTab("🔧  Issues",   new IssuePanel());
            t.addTab("📅  Meetings", new MeetingPanel());
            t.setToolTipTextAt(2, "Infrastructure issue tracker");
            t.setToolTipTextAt(3, "Meeting records manager");
        }
        return t;
    }

    // ── Status bar ────────────────────────────────────────────────────────
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

        JLabel right = new JLabel("Date: " + java.time.LocalDate.now());
        right.setFont(new Font("SansSerif", Font.PLAIN, 11));
        right.setForeground(TEXT_MUTED);

        bar.add(left,  BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }
}
