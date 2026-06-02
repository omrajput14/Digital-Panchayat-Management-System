package panchayat.view;

import panchayat.dao.ComplaintDAO;
import panchayat.model.Complaint;
import panchayat.util.ReportExporter;
import panchayat.util.PdfExporter;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * ComplaintPanel — Full CRUD interface for citizen complaints.
 *
 * Layout strategy:
 *   - North: filter bar (status combo + ward text field + search button)
 *   - Center: scrollable JTable showing matching complaints
 *   - South: form panel (register new complaint) + action buttons
 *
 * The JTable uses a custom DefaultTableModel that marks all cells
 * non-editable — edits go through the "Update Status" button only,
 * preventing accidental inline changes.
 */
public class ComplaintPanel extends JPanel {

    private final ComplaintDAO dao = new ComplaintDAO();

    // ── Table ──────────────────────────────────────────────────────────
    private JTable           table;
    private DefaultTableModel tableModel;

    // ── Filter controls ────────────────────────────────────────────────
    private JComboBox<String> cbFilterStatus;
    private JTextField        tfFilterWard;

    // ── Form fields ────────────────────────────────────────────────────
    private JTextField        tfCitizenName;
    private JTextField        tfWardNumber;
    private JComboBox<String> cbCategory;
    private JTextArea         taDescription;
    private JComboBox<String> cbStatus;

    // ── Colours ────────────────────────────────────────────────────────
    private static final Color BG_DARK    = new Color(15, 23, 42);
    private static final Color BG_CARD    = new Color(30, 41, 59);
    private static final Color BG_INPUT   = new Color(51, 65, 85);
    private static final Color ACCENT     = new Color(59, 130, 246);
    private static final Color ACCENT_RED = new Color(239, 68, 68);
    private static final Color TEXT_WHITE = new Color(248, 250, 252);
    private static final Color TEXT_MUTED = new Color(148, 163, 184);
    private static final Color ROW_ALT    = new Color(22, 32, 52);
    private static final Color ROW_SEL    = new Color(37, 99, 235);

    // Column names for the JTable
    private static final String[] COLUMNS = {
        "ID", "Citizen Name", "Ward", "Category", "Date Filed", "Status"
    };

    public ComplaintPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(buildHeader(),     BorderLayout.NORTH);
        add(buildTableArea(),  BorderLayout.CENTER);
        add(buildFormArea(),   BorderLayout.SOUTH);

        loadData(null, null);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Header / filter bar
    // ─────────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(16, 0));
        p.setBackground(BG_DARK);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JLabel title = new JLabel("🏠  Citizen Complaint Portal");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(TEXT_WHITE);

        // Filter controls
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filters.setBackground(BG_DARK);

        filters.add(styledLabel("Filter by Status:"));
        cbFilterStatus = new JComboBox<>(new String[]{"All",
            Complaint.STATUS_PENDING, Complaint.STATUS_IN_PROGRESS, Complaint.STATUS_RESOLVED});
        styleCombo(cbFilterStatus);
        filters.add(cbFilterStatus);

        filters.add(styledLabel("Ward:"));
        tfFilterWard = styledTextField(8);
        filters.add(tfFilterWard);

        JButton btnFilter = actionButton("🔍 Search", ACCENT);
        btnFilter.addActionListener(e -> applyFilters());
        filters.add(btnFilter);

        JButton btnClear = actionButton("✕ Clear", new Color(71, 85, 105));
        btnClear.addActionListener(e -> {
            cbFilterStatus.setSelectedIndex(0);
            tfFilterWard.setText("");
            loadData(null, null);
        });
        filters.add(btnClear);

        p.add(title,   BorderLayout.WEST);
        p.add(filters, BorderLayout.EAST);
        return p;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Table area
    // ─────────────────────────────────────────────────────────────────────
    private JScrollPane buildTableArea() {
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        styleTable(table);

        // Custom renderer to colour-code Status column and alternate row bg
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, focus, row, col);
                setBackground(sel ? ROW_SEL : (row % 2 == 0 ? BG_CARD : ROW_ALT));
                setForeground(TEXT_WHITE);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                // Colour the Status cell based on its value
                if (col == 5 && val != null) {
                    switch (val.toString()) {
                        case "Pending"     -> setForeground(new Color(251, 191, 36));
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

    // ─────────────────────────────────────────────────────────────────────
    // Form area (register + actions)
    // ─────────────────────────────────────────────────────────────────────
    private JPanel buildFormArea() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 12));
        wrapper.setBackground(BG_DARK);

        // ── Registration form ──────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_CARD);
        form.setBorder(new CompoundBorder(
            new LineBorder(new Color(51, 65, 85), 1, true),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        JLabel formTitle = new JLabel("Register New Complaint");
        formTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        formTitle.setForeground(ACCENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        // Form title spans full width
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 6; gbc.anchor = GridBagConstraints.WEST;
        form.add(formTitle, gbc);
        gbc.gridwidth = 1;

        // Row 1: Name | Ward | Category | Status
        tfCitizenName = styledTextField(14);
        tfWardNumber  = styledTextField(8);
        cbCategory    = new JComboBox<>(Complaint.CATEGORIES);
        styleCombo(cbCategory);
        cbStatus = new JComboBox<>(new String[]{
            Complaint.STATUS_PENDING, Complaint.STATUS_IN_PROGRESS, Complaint.STATUS_RESOLVED});
        styleCombo(cbStatus);

        addFormRow(form, gbc, 1,
            "Citizen Name*", tfCitizenName,
            "Ward No.*",     tfWardNumber,
            "Category*",     cbCategory,
            "Status*",       cbStatus);

        // Row 2: Description (spans width)
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.NORTHWEST;
        form.add(styledLabel("Description*"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 5; gbc.anchor = GridBagConstraints.WEST;
        taDescription = new JTextArea(3, 40);
        taDescription.setLineWrap(true);
        taDescription.setWrapStyleWord(true);
        taDescription.setBackground(BG_INPUT);
        taDescription.setForeground(TEXT_WHITE);
        taDescription.setCaretColor(TEXT_WHITE);
        taDescription.setFont(new Font("SansSerif", Font.PLAIN, 13));
        taDescription.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        form.add(new JScrollPane(taDescription), gbc);

        wrapper.add(form, BorderLayout.CENTER);

        // ── Action buttons ─────────────────────────────────────────────
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setBackground(BG_DARK);

        JButton btnSave    = actionButton("💾 Register Complaint", ACCENT);
        JButton btnUpdate  = actionButton("✏ Update Status",        new Color(16, 185, 129));
        JButton btnDelete  = actionButton("🗑 Delete",              ACCENT_RED);
        JButton btnCsv     = actionButton("📥 Export CSV",          new Color(99, 102, 241));
        JButton btnPrint   = actionButton("🖨 Print Report",        new Color(245, 158, 11));
        JButton btnPdf     = actionButton("📄 Save as PDF",         new Color(220, 38, 38));

        btnSave.addActionListener(   e -> registerComplaint());
        btnUpdate.addActionListener( e -> updateSelectedStatus());
        btnDelete.addActionListener( e -> deleteSelected());
        btnCsv.addActionListener(    e -> ReportExporter.exportTableToCSV(table, this));
        btnPrint.addActionListener(  e -> printComplaintsReport());
        btnPdf.addActionListener(    e -> savePdfReport());

        btnPanel.add(btnDelete);
        btnPanel.add(btnPdf);
        btnPanel.add(btnPrint);
        btnPanel.add(btnCsv);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnSave);
        wrapper.add(btnPanel, BorderLayout.SOUTH);

        return wrapper;
    }

    // ── Helper: add 3 label+field pairs to a row ─────────────────────────
    private void addFormRow(JPanel form, GridBagConstraints gbc, int gridy,
                            String lbl1, JComponent c1,
                            String lbl2, JComponent c2,
                            String lbl3, JComponent c3,
                            String lbl4, JComponent c4) {
        gbc.gridy = gridy; gbc.weightx = 0;
        gbc.gridx = 0; form.add(styledLabel(lbl1), gbc);
        gbc.gridx = 1; gbc.weightx = 1;   form.add(c1, gbc); gbc.weightx = 0;
        gbc.gridx = 2; form.add(styledLabel(lbl2), gbc);
        gbc.gridx = 3; gbc.weightx = 0.5; form.add(c2, gbc); gbc.weightx = 0;
        gbc.gridx = 4; form.add(styledLabel(lbl3), gbc);
        gbc.gridx = 5; gbc.weightx = 0.5; form.add(c3, gbc); gbc.weightx = 0;
        // c4 (Status combo) added inline after c3 — append to same row visually
        gbc.gridx = 6; form.add(styledLabel(lbl4), gbc);
        gbc.gridx = 7; gbc.weightx = 0.5; form.add(c4, gbc); gbc.weightx = 0;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Business logic
    // ─────────────────────────────────────────────────────────────────────

    /** Read form, validate, persist, and refresh the table. */
    private void registerComplaint() {
        String name = tfCitizenName.getText().trim();
        String ward = tfWardNumber.getText().trim();
        String desc = taDescription.getText().trim();

        // Step 1: Required-field check — highlights empty fields in red
        if (!ReportExporter.validateRequiredFields(this, tfCitizenName, tfWardNumber, taDescription))
            return;

        // Step 2: Ward format validation (alphanumeric, max 15 chars)
        if (!ReportExporter.validateWard(ward, tfWardNumber, this)) return;

        // Step 3: Name length guard (prevent garbage data)
        if (name.length() > 80) {
            ReportExporter.highlightError(tfCitizenName);
            JOptionPane.showMessageDialog(this,
                "Citizen name is too long (max 80 characters).",
                "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Complaint c = new Complaint(
            name, ward,
            cbCategory.getSelectedItem().toString(),
            desc,
            LocalDate.now().toString(),
            cbStatus.getSelectedItem().toString()
        );

        try {
            int id = dao.insert(c);
            c.setId(id);
            clearForm();
            loadData(null, null);
            JOptionPane.showMessageDialog(this,
                "✅  Complaint #" + id + " registered successfully!",
                "Registered", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Database error: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Build and send a formatted print report of current table rows. */
    private void printComplaintsReport() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No complaint data to print.",
                "Empty Table", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("SECTION: Citizen Complaints\n\n");
        sb.append(String.format("%-5s %-20s %-8s %-14s %-12s %-12s%n",
            "ID", "Citizen Name", "Ward", "Category", "Date Filed", "Status"));
        sb.append("─".repeat(75)).append("\n");
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            sb.append(String.format("%-5s %-20s %-8s %-14s %-12s %-12s%n",
                tableModel.getValueAt(r, 0),
                truncate(tableModel.getValueAt(r, 1).toString(), 20),
                tableModel.getValueAt(r, 2),
                tableModel.getValueAt(r, 3),
                tableModel.getValueAt(r, 4),
                tableModel.getValueAt(r, 5)));
        }
        sb.append("\nTotal records: ").append(tableModel.getRowCount());
        ReportExporter.printReport("Citizen Complaints Report", sb.toString(), this);
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    /** Build report body text (shared by print and PDF). */
    private String buildReportBody() {
        StringBuilder sb = new StringBuilder();
        sb.append("SECTION: Citizen Complaints\n\n");
        sb.append(String.format("%-5s %-20s %-8s %-14s %-12s %-12s%n",
            "ID","Citizen Name","Ward","Category","Date Filed","Status"));
        sb.append("─".repeat(75)).append("\n");
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            sb.append(String.format("%-5s %-20s %-8s %-14s %-12s %-12s%n",
                tableModel.getValueAt(r,0), truncate(tableModel.getValueAt(r,1).toString(),20),
                tableModel.getValueAt(r,2), tableModel.getValueAt(r,3),
                tableModel.getValueAt(r,4), tableModel.getValueAt(r,5)));
        }
        sb.append("\nTotal records: ").append(tableModel.getRowCount());
        return sb.toString();
    }

    /** Save report directly as a PDF file (no print dialog). */
    private void savePdfReport() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No complaint data to export.",
                "Empty Table", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        PdfExporter.savePdf("Citizen Complaints Report", buildReportBody(), this);
    }

    /** Update the status of the selected table row. */
    private void updateSelectedStatus() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a complaint from the table.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);

        // Let user pick new status via dialog
        String[] opts = {Complaint.STATUS_PENDING, Complaint.STATUS_IN_PROGRESS, Complaint.STATUS_RESOLVED};
        String picked = (String) JOptionPane.showInputDialog(
            this, "Select new status for Complaint #" + id,
            "Update Status", JOptionPane.PLAIN_MESSAGE, null, opts,
            tableModel.getValueAt(row, 5));

        if (picked == null) return; // user cancelled

        try {
            dao.updateStatus(id, picked);
            loadData(null, null);
            JOptionPane.showMessageDialog(this, "Status updated successfully!", "Done",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Delete the selected complaint after confirmation. */
    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a complaint to delete.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete Complaint #" + id + "?\nThis cannot be undone.",
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

    /** Apply filter bar values and reload the table. */
    private void applyFilters() {
        String status = cbFilterStatus.getSelectedItem().toString();
        String ward   = tfFilterWard.getText().trim();
        loadData(status.equals("All") ? null : status,
                 ward.isEmpty() ? null : ward);
    }

    /** Fetch complaints from DB and repopulate the table model. */
    private void loadData(String statusFilter, String wardFilter) {
        tableModel.setRowCount(0);
        try {
            List<Complaint> list = dao.findAll(statusFilter, wardFilter);
            for (Complaint c : list) {
                tableModel.addRow(new Object[]{
                    c.getId(), c.getCitizenName(), c.getWardNumber(),
                    c.getCategory(), c.getDateFiled(), c.getStatus()
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading data: " + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        tfCitizenName.setText("");
        tfWardNumber.setText("");
        taDescription.setText("");
        cbCategory.setSelectedIndex(0);
        cbStatus.setSelectedIndex(0);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Style helpers
    // ─────────────────────────────────────────────────────────────────────
    private void styleTable(JTable t) {
        t.setBackground(BG_CARD);
        t.setForeground(TEXT_WHITE);
        t.setGridColor(new Color(51, 65, 85));
        t.setRowHeight(28);
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(false);
        t.setSelectionBackground(ROW_SEL);
        t.setSelectionForeground(Color.WHITE);
        t.getTableHeader().setBackground(new Color(15, 23, 60));
        t.getTableHeader().setForeground(TEXT_MUTED);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        t.getTableHeader().setBorder(new MatteBorder(0, 0, 2, 0, ACCENT));
        // Narrow the ID column
        t.getColumnModel().getColumn(0).setMaxWidth(50);
        t.getColumnModel().getColumn(0).setMinWidth(40);
    }

    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(TEXT_MUTED);
        return l;
    }

    private JTextField styledTextField(int cols) {
        JTextField tf = new JTextField(cols);
        tf.setBackground(BG_INPUT);
        tf.setForeground(TEXT_WHITE);
        tf.setCaretColor(TEXT_WHITE);
        tf.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tf.setBorder(new CompoundBorder(
            new LineBorder(new Color(71, 85, 105), 1, true),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        return tf;
    }

    private void styleCombo(JComboBox<?> cb) {
        cb.setBackground(BG_INPUT);
        cb.setForeground(TEXT_WHITE);
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
        btn.setPreferredSize(new Dimension(180, 34));
        return btn;
    }
}
