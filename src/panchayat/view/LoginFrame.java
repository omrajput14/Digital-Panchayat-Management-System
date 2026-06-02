package panchayat.view;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class LoginFrame extends JFrame {

    private static final Color BG_DARK   = new Color(15, 23, 42);
    private static final Color BG_CARD   = new Color(30, 41, 59);
    private static final Color BG_INPUT  = new Color(51, 65, 85);
    private static final Color ACCENT    = new Color(99, 102, 241);
    private static final Color TEXT_WHITE = new Color(248, 250, 252);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);

    public LoginFrame() {
        super("Panchayat System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 480);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(BG_DARK);
        mainPanel.setBorder(new EmptyBorder(50, 40, 50, 40));

        JLabel logo = new JLabel("🏛");
        logo.setFont(new Font("SansSerif", Font.PLAIN, 64));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Digital Panchayat System");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(TEXT_WHITE);

        JLabel subtitle = new JLabel("Please select your login type to continue");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setForeground(TEXT_MUTED);

        mainPanel.add(logo);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(title);
        mainPanel.add(Box.createVerticalStrut(8));
        mainPanel.add(subtitle);
        mainPanel.add(Box.createVerticalStrut(50));

        JButton btnPublic = createLoginButton("👥  Public / Citizen Login", new Color(59, 130, 246));
        JButton btnGov    = createLoginButton("🏛  Government Login",       ACCENT);

        btnPublic.addActionListener(e -> launchMain(false));
        btnGov.addActionListener(e -> showGovPasswordDialog());

        btnPublic.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnGov.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(btnPublic);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(btnGov);

        add(mainPanel);
    }

    // ── Custom gov password dialog ────────────────────────────────────────
    private void showGovPasswordDialog() {
        JDialog dialog = new JDialog(this, "Government Login", true);
        dialog.setSize(380, 260);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.setUndecorated(false);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_CARD);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        // Icon + title
        JLabel icon = new JLabel("🔐  Government Portal");
        icon.setFont(new Font("SansSerif", Font.BOLD, 17));
        icon.setForeground(TEXT_WHITE);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hint = new JLabel("Enter your access password to continue");
        hint.setFont(new Font("SansSerif", Font.PLAIN, 12));
        hint.setForeground(TEXT_MUTED);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(icon);
        panel.add(Box.createVerticalStrut(6));
        panel.add(hint);
        panel.add(Box.createVerticalStrut(24));

        // Password field row with eye toggle
        JPasswordField pwdField = new JPasswordField(16);
        pwdField.setBackground(BG_INPUT);
        pwdField.setForeground(TEXT_WHITE);
        pwdField.setCaretColor(TEXT_WHITE);
        pwdField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        pwdField.setEchoChar('●');
        pwdField.setBorder(new CompoundBorder(
            new LineBorder(new Color(71, 85, 105), 1, true),
            new EmptyBorder(8, 10, 8, 10)
        ));

        // Eye toggle button
        JToggleButton eyeBtn = new JToggleButton("👁");
        eyeBtn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        eyeBtn.setFocusPainted(false);
        eyeBtn.setBorderPainted(false);
        eyeBtn.setContentAreaFilled(false);
        eyeBtn.setForeground(TEXT_MUTED);
        eyeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        eyeBtn.setToolTipText("Show / Hide password");
        eyeBtn.addActionListener(e -> {
            if (eyeBtn.isSelected()) {
                pwdField.setEchoChar((char) 0); // show plain text
                eyeBtn.setForeground(ACCENT);
            } else {
                pwdField.setEchoChar('●');       // hide again
                eyeBtn.setForeground(TEXT_MUTED);
            }
        });

        JPanel pwdRow = new JPanel(new BorderLayout(6, 0));
        pwdRow.setBackground(BG_CARD);
        pwdRow.add(pwdField, BorderLayout.CENTER);
        pwdRow.add(eyeBtn,   BorderLayout.EAST);
        pwdRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        panel.add(pwdRow);
        panel.add(Box.createVerticalStrut(20));

        // Login button
        JButton loginBtn = new JButton("Login →") {
            final Color bg = ACCENT;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? bg.darker() :
                            getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        loginBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFocusPainted(false);
        loginBtn.setBorderPainted(false);
        loginBtn.setContentAreaFilled(false);
        loginBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Validate password on click or Enter key
        ActionListener validate = e -> {
            String entered = new String(pwdField.getPassword());
            if (entered.equals("WADE")) {
                dialog.dispose();
                launchMain(true);
            } else {
                pwdField.setText("");
                pwdField.setBorder(new CompoundBorder(
                    new LineBorder(new Color(239, 68, 68), 1, true),
                    new EmptyBorder(8, 10, 8, 10)
                ));
                hint.setText("❌ Incorrect password. Try again.");
                hint.setForeground(new Color(252, 165, 165));
            }
        };

        loginBtn.addActionListener(validate);
        pwdField.addActionListener(validate); // press Enter to submit

        panel.add(loginBtn);

        dialog.setContentPane(panel);
        dialog.getRootPane().setDefaultButton(loginBtn);
        dialog.setVisible(true);
    }

    private void launchMain(boolean isGov) {
        dispose();
        SwingUtilities.invokeLater(() -> new MainFrame(isGov));
    }

    private JButton createLoginButton(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed())       g2.setColor(bg.darker());
                else if (getModel().isRollover()) g2.setColor(bg.brighter());
                else                              g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 15));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(320, 50));
        btn.setPreferredSize(new Dimension(320, 50));
        return btn;
    }
}
