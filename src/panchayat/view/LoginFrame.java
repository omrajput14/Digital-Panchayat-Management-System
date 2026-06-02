package panchayat.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginFrame extends JFrame {
    
    public LoginFrame() {
        super("Panchayat System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 450);
        setLocationRelativeTo(null);
        setResizable(false);
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(new Color(15, 23, 42));
        mainPanel.setBorder(new EmptyBorder(50, 40, 50, 40));
        
        JLabel logo = new JLabel("🏛");
        logo.setFont(new Font("SansSerif", Font.PLAIN, 64));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setForeground(new Color(248, 250, 252));
        
        JLabel title = new JLabel("Digital Panchayat System");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(new Color(248, 250, 252));
        
        JLabel subtitle = new JLabel("Please select your login type to continue");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setForeground(new Color(100, 116, 139));
        
        mainPanel.add(logo);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(title);
        mainPanel.add(Box.createVerticalStrut(8));
        mainPanel.add(subtitle);
        mainPanel.add(Box.createVerticalStrut(50));
        
        JButton btnPublic = createLoginButton("👥  Public / Citizen Login", new Color(59, 130, 246));
        JButton btnGov = createLoginButton("🏛  Government Login", new Color(99, 102, 241));
        
        btnPublic.addActionListener(e -> {
            launchMain(false);
        });
        
        btnGov.addActionListener(e -> {
            // Simple password prompt for gov login
            JPasswordField pwd = new JPasswordField(10);
            int action = JOptionPane.showConfirmDialog(this, pwd, "Enter Government Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (action == JOptionPane.OK_OPTION) {
                if (new String(pwd.getPassword()).equals("WADE")) {
                    launchMain(true);
                } else {
                    JOptionPane.showMessageDialog(this, "Incorrect password!", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        btnPublic.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnGov.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        mainPanel.add(btnPublic);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(btnGov);
        
        add(mainPanel);
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
                if (getModel().isPressed()) g2.setColor(bg.darker());
                else if (getModel().isRollover()) g2.setColor(bg.brighter());
                else g2.setColor(bg);
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
