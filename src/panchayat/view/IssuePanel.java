package panchayat.view;

import panchayat.dao.IssueDAO;
import panchayat.model.Issue;
import panchayat.util.ReportExporter;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * IssuePanel — Infrastructure Issue Tracker UI.
 *
 * Key feature: Overdue rows (expected_resolution < today AND not Resolved)
 * are highlighted in a warm red tint so staff can immediately see which
 * issues need escalation — this is done entirely in the cell renderer
 * without touching the model or DAO.
 */
public class IssuePanel extends JPanel {

    private final IssueDAO dao = new IssueDAO();

    private JTable            table;
    private DefaultTableModel tableModel;

    // Filter controls
    private JComboBox<String> cbFilterSeverity;
    private JTextField        tfFilterWard;

    // Form fields
    private JTextField        tfTitle;
    private JTextField        tfWard;
    private JTextField        tfOfficer;
    private JTextField        tfExpectedDate;
    private JComboBox<String> cbType;
    private JComboBox<String> cbSeverity;
    private JComboBox<String> cbStatus;

    // Colours
    private static final Color BG_DARK      = new Color(15, 23, 42);
    private static final Color BG_CARD      = new Color(30, 41, 59);
    private static final Color BG_INPUT     = new Color(51, 65, 85);
    private static final Color ACCENT       = new Color(16, 185, 129);   // emerald
    private static final Color ACCENT_RED   = new Color(239, 68, 68);
    private static final Color OVERDUE_BG   = new Color(60, 20, 20);     // dark red tint
    private static final Color TEXT_WHITE   = new Color(248, 250, 252);
    private static final Color TEXT_MUTED   = new Color(148, 163, 184);
    private static final Color ROW_ALT      = new Color(22, 32, 52);
    private static final Color ROW_SEL      = new Color(6, 120, 90);

    private static final String[] COLUMNS = {
        "ID", "Title", "Ward", "Type", "Severity", "Officer", "Reported", "Expected", "Status"
    };

    public IssuePanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(buildHeader(),    BorderLayout.NORTH);
        add(buildTableArea(), BorderLayout.CENTER);
        add(buildFormArea(),  BorderLayout.SOUTH);

        loadData(null, null);
    }

    // ─── Header / filter bar ─────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(16, 0));
        p.setBackground(BG_DARK);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JLabel title = new JLabel("🔧  Infrastructure Issue Tracker");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(TEXT_WHITE);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filters.setBackground(BG_DARK);

        filters.add(styledLabel("Severity:"));
        cbFilterSeverity = new JComboBox<>(new String[]{"All", "Low", "Medium", "High"});
        styleCombo(cbFilterSeverity);
        filters.add(cbFilterSeverity);

        filters.add(styledLabel("Ward:"));
        tfFilterWard = styledTextField(8);
        filters.add(tfFilterWard);

        JButton btnFilter = actionButton("🔍 Search", ACCENT);
        btnFilter.addActionListener(e -> applyFilters());
        filters.add(btnFilter);

        JButton btnClear = actionButton("✕ Clear", new Color(71, 85, 105));
        btnClear.addActionListener(e -> {
            cbFilterSeverity.setSelectedIndex(0);
            tfFilterWard.setText("");
            loadData(null, null);
        });
        filters.add(btnClear);

        // Legend for overdue highlight
        JLabel legend = new JLabel("  🔴 = Overdue");
        legend.setFont(new Font("SansSerif", Font.ITALIC, 11));
        legend.setForeground(ACCENT_RED);
        filters.add(legend);

        p.add(title,   BorderLayout.WEST);
        p.add(filters, BorderLayout.EAST);
        return p;
    }

    // ─── Table area ──────────────────────────────────────────────────────
    private JScrollPane buildTableArea() {
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        styleTable(table);

        // Custom renderer: overdue rows get red background, severity gets colour
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, focus, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

                // Check if this row is overdue
                String expectedDate = (String) tbl.getModel().getValueAt(row, 7);
                String status       = (String) tbl.getModel().getValueAt(row, 8);
                boolean overdue = !status.equals("Resolved")
                    && expectedDate != null
                    && LocalDate.parse(expectedDate).isBefore(LocalDate.now());

                if (sel) {
                    setBackground(ROW_SEL);
                    setForeground(TEXT_WHITE);
                } else if (overdue) {
                    setBackground(OVERDUE_BG);
                    setForeground(new Color(252, 165, 165)); // light red text
                } else {
                    setBackground(row % 2 == 0 ? BG_CARD : ROW_ALT);
                    setForeground(TEXT_WHITE);
                }

                // Colour-code Severity column (index 4)
                if (col == 4 && val != null && !sel) {
                    switch (val.toString()) {
                        case "Low"    -> setForeground(new Color(52, 211, 153));
                        case "Medium" -> setForeground(new Color(251, 191, 36));
                        case "High"   -> setForeground(new Color(248, 113, 113));
                    }
                }
                // Colour-code Status column (index 8)
                if (col == 8 && val != null && !sel) {
                    switch (val.toString()) {
                        case "Open"        -> setForeground(new Color(251, 191, 36));
                        case "In Progress" -> setForeground(new Color(96, 165, 250));
                        case "Resolved"    -> setForeground(new Color(52, 211, 153));
                    }
                }
                return this;
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBackground(BG_DARK);
        sp.getViewport().setBackground(BG_CARD);
        sp.setBorder(new LineBorder(new Color(51, 65, 85), 1, true));
        return sp;
    }

    // ─── Form area ────────────────────────────────────────────────────────
    private JPanel buildFormArea() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setBackground(BG_DARK);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_CARD);
        form.setBorder(new CompoundBorder(
            new LineBorder(new Color(51, 65, 85), 1, true),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));

        JLabel formTitle = new JLabel("Log New Infrastructure Issue");
        formTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        formTitle.setForeground(ACCENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Title row
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 8;
        form.add(formTitle, gbc);
        gbc.gridwidth = 1;

        // Row 1
        tfTitle    = styledTextField(14);
        tfWard     = styledTextField(8);
        cbType     = new JComboBox<>(Issue.TYPES);      styleCombo(cbType);
        cbSeverity = new JComboBox<>(Issue.SEVERITIES); styleCombo(cbSeverity);

        gbc.gridy = 1;
        gbc.gridx = 0; form.add(styledLabel("Title*"),    gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(tfTitle,    gbc); gbc.weightx = 0;
        gbc.gridx = 2; form.add(styledLabel("Ward*"),    gbc);
        gbc.gridx = 3; gbc.weightx = 0.5; form.add(tfWard,     gbc); gbc.weightx = 0;
        gbc.gridx = 4; form.add(styledLabel("Type*"),    gbc);
        gbc.gridx = 5; gbc.weightx = 0.5; form.add(cbType,     gbc); gbc.weightx = 0;
        gbc.gridx = 6; form.add(styledLabel("Severity*"),gbc);
        gbc.gridx = 7; gbc.weightx = 0.5; form.add(cbSeverity, gbc); gbc.weightx = 0;

        // Row 2
        tfOfficer      = styledTextField(12);
        tfExpectedDate = styledTextField(10);
        tfExpectedDate.setToolTipText("Format: YYYY-MM-DD");
        cbStatus       = new JComboBox<>(Issue.STATUSES); styleCombo(cbStatus);

        gbc.gridy = 2;
        gbc.gridx = 0; form.add(styledLabel("Assigned Officer*"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(tfOfficer,      gbc); gbc.weightx = 0;
        gbc.gridx = 2; form.add(styledLabel("Exp. Resolution*"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.5; form.add(tfExpectedDate, gbc); gbc.weightx = 0;
        gbc.gridx = 4; form.add(styledLabel("Status*"),           gbc);
        gbc.gridx = 5; gbc.weightx = 0.5; form.add(cbStatus,       gbc); gbc.weightx = 0;

        wrapper.add(form, BorderLayout.CENTER);

        // Action buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setBackground(BG_DARK);

        JButton btnSave   = actionButton("💾 Log Issue",       ACCENT);
        JButton btnUpdate = actionButton("✏ Update Status",   new Color(59, 130, 246));
        JButton btnDelete = actionButton("🗑 Delete",          ACCENT_RED);
        JButton btnCsv    = actionButton("📥 Export CSV",      new Color(99, 102, 241));
        JButton btnPrint  = actionButton("🖨 Print Report",    new Color(245, 158, 11));

        btnSave.addActionListener(   e -> logIssue());
        btnUpdate.addActionListener( e -> updateSelectedStatus());
        btnDelete.addActionListener( e -> deleteSelected());
        btnCsv.addActionListener(    e -> ReportExporter.exportTableToCSV(table, this));
        btnPrint.addActionListener(  e -> printIssuesReport());

        btnRow.add(btnDelete);
        btnRow.add(btnPrint);
        btnRow.add(btnCsv);
        btnRow.add(btnUpdate);
        btnRow.add(btnSave);
        wrapper.add(btnRow, BorderLayout.SOUTH);

        return wrapper;
    }

    // ─── Business logic ──────────────────────────────────────────────────

    private void logIssue() {
        String title   = tfTitle.getText().trim();
        String ward    = tfWard.getText().trim();
        String officer = tfOfficer.getText().trim();
        String expDate = tfExpectedDate.getText().trim();

        // Highlight all empty required fields at once
        if (!ReportExporter.validateRequiredFields(this, tfTitle, tfWard, tfOfficer, tfExpectedDate))
            return;

        // Ward format check
        if (!ReportExporter.validateWard(ward, tfWard, this)) return;

        // Strict date parsing — gives a friendly message for typos like "2024-13-01"
        if (!ReportExporter.validateDate(expDate, tfExpectedDate, this)) return;

        // Warn if expected date is in the past
        if (java.time.LocalDate.parse(expDate).isBefore(java.time.LocalDate.now())) {
            int ok = JOptionPane.showConfirmDialog(this,
                "Expected resolution date " + expDate + " is in the past.\nSave anyway?",
                "Past Date Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok != JOptionPane.YES_OPTION) return;
        }

        Issue issue = new Issue(
            title, ward,
            cbType.getSelectedItem().toString(),
            cbSeverity.getSelectedItem().toString(),
            officer,
            java.time.LocalDate.now().toString(),
            expDate,
            cbStatus.getSelectedItem().toString()
        );

        try {
            int id = dao.insert(issue);
            clearForm();
            loadData(null, null);
            JOptionPane.showMessageDialog(this, "✅  Issue #" + id + " logged successfully!",
                "Logged", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Build and print a formatted report of issues in the current table. */
    private void printIssuesReport() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No issue data to print.",
                "Empty Table", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("SECTION: Infrastructure Issues\n\n");
        sb.append(String.format("%-5s %-18s %-8s %-14s %-8s %-12s %-12s %-10s%n",
            "ID","Title","Ward","Type","Severity","Officer","Reported","Status"));
        sb.append("─".repeat(90)).append("\n");
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            sb.append(String.format("%-5s %-18s %-8s %-14s %-8s %-12s %-12s %-10s%n",
                tableModel.getValueAt(r, 0),
                truncate(tableModel.getValueAt(r, 1).toString(), 18),
                tableModel.getValueAt(r, 2),
                tableModel.getValueAt(r, 3),
                tableModel.getValueAt(r, 4),
                truncate(tableModel.getValueAt(r, 5).toString(), 12),
                tableModel.getValueAt(r, 6),
                tableModel.getValueAt(r, 8)));
        }
        sb.append("\nTotal records: ").append(tableModel.getRowCount());
        ReportExporter.printReport("Infrastructure Issues Report", sb.toString(), this);
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private void updateSelectedStatus() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an issue from the table.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        String picked = (String) JOptionPane.showInputDialog(
            this, "Select new status for Issue #" + id,
            "Update Status", JOptionPane.PLAIN_MESSAGE, null, Issue.STATUSES,
            tableModel.getValueAt(row, 8));

        if (picked == null) return;
        try {
            dao.updateStatus(id, picked);
            loadData(null, null);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an issue to delete.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete Issue #" + id + "? This cannot be undone.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            dao.delete(id);
            loadData(null, null);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyFilters() {
        String sev  = cbFilterSeverity.getSelectedItem().toString();
        String ward = tfFilterWard.getText().trim();
        loadData(sev.equals("All") ? null : sev, ward.isEmpty() ? null : ward);
    }

    private void loadData(String severityFilter, String wardFilter) {
        tableModel.setRowCount(0);
        try {
            List<Issue> list = dao.findAll(severityFilter, wardFilter);
            for (Issue i : list) {
                tableModel.addRow(new Object[]{
                    i.getId(), i.getTitle(), i.getWard(), i.getType(),
                    i.getSeverity(), i.getAssignedOfficer(),
                    i.getDateReported(), i.getExpectedResolution(), i.getStatus()
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading data: " + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        tfTitle.setText(""); tfWard.setText("");
        tfOfficer.setText(""); tfExpectedDate.setText("");
        cbType.setSelectedIndex(0); cbSeverity.setSelectedIndex(0);
        cbStatus.setSelectedIndex(0);
    }

    // ─── Style helpers ─────────────────────────────────────────────────
    private void styleTable(JTable t) {
        t.setBackground(BG_CARD); t.setForeground(TEXT_WHITE);
        t.setGridColor(new Color(51, 65, 85)); t.setRowHeight(28);
        t.setFont(new Font("SansSerif", Font.PLAIN, 12));
        t.setShowHorizontalLines(true); t.setShowVerticalLines(false);
        t.setSelectionBackground(ROW_SEL); t.setSelectionForeground(Color.WHITE);
        t.getTableHeader().setBackground(new Color(15, 30, 45));
        t.getTableHeader().setForeground(TEXT_MUTED);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        t.getTableHeader().setBorder(new MatteBorder(0, 0, 2, 0, ACCENT));
        t.getColumnModel().getColumn(0).setMaxWidth(45);
    }

    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(TEXT_MUTED);
        return l;
    }

    private JTextField styledTextField(int cols) {
        JTextField tf = new JTextField(cols);
        tf.setBackground(BG_INPUT); tf.setForeground(TEXT_WHITE);
        tf.setCaretColor(TEXT_WHITE);
        tf.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tf.setBorder(new CompoundBorder(
            new LineBorder(new Color(71, 85, 105), 1, true),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        return tf;
    }

    private void styleCombo(JComboBox<?> cb) {
        cb.setBackground(BG_INPUT); cb.setForeground(TEXT_WHITE);
        cb.setFont(new Font("SansSerif", Font.PLAIN, 13));
        cb.setBorder(new LineBorder(new Color(71, 85, 105), 1, true));
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object val,
                    int idx, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, val, idx, sel, focus);
                setBackground(sel ? ACCENT : BG_INPUT);
                setForeground(TEXT_WHITE);
                return this;
            }
        });
    }

    private JButton actionButton(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? bg.darker() :
                            getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(170, 34));
        return btn;
    }
}
