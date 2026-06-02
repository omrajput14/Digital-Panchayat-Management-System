package panchayat.view;

import panchayat.dao.MeetingDAO;
import panchayat.model.Meeting;
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
 * MeetingPanel — Panchayat Meeting Records Manager UI.
 *
 * Attendees and action items use newline-separated input in JTextAreas
 * (one per line) which are joined/split with the "|" pipe delimiter
 * when persisting to/from SQLite.  This makes the UI intuitive for
 * non-technical staff who can simply press Enter between entries.
 *
 * Clicking a row in the table populates a read-only "detail view"
 * area at the bottom so staff can read long agenda text without
 * needing a separate dialog.
 */
public class MeetingPanel extends JPanel {

    private final MeetingDAO dao = new MeetingDAO();

    private JTable            table;
    private DefaultTableModel tableModel;

    // Filter
    private JTextField tfSearch;

    // Form fields
    private JComboBox<String> cbMeetingType;
    private JTextField        tfDate;
    private JTextArea         taAttendees;
    private JTextArea         taAgenda;
    private JTextArea         taResolutions;
    private JTextArea         taActionItems;

    // Detail view
    private JTextArea         taDetailView;

    // Colours
    private static final Color BG_DARK    = new Color(15, 23, 42);
    private static final Color BG_CARD    = new Color(30, 41, 59);
    private static final Color BG_INPUT   = new Color(51, 65, 85);
    private static final Color ACCENT     = new Color(139, 92, 246);   // violet
    private static final Color ACCENT_RED = new Color(239, 68, 68);
    private static final Color TEXT_WHITE = new Color(248, 250, 252);
    private static final Color TEXT_MUTED = new Color(148, 163, 184);
    private static final Color ROW_ALT    = new Color(22, 32, 52);
    private static final Color ROW_SEL    = new Color(80, 50, 170);

    private static final String[] COLUMNS = {
        "ID", "Type", "Date", "Attendees", "Agenda (preview)"
    };

    public MeetingPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(buildHeader(), BorderLayout.NORTH);

        // Center: split pane — table top, detail bottom
        JSplitPane center = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            buildTableArea(), buildDetailArea());
        center.setDividerLocation(220);
        center.setDividerSize(6);
        center.setBackground(BG_DARK);
        center.setBorder(null);
        add(center, BorderLayout.CENTER);

        add(buildFormArea(), BorderLayout.SOUTH);

        loadData(null);
    }

    // ─── Header ─────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(16, 0));
        p.setBackground(BG_DARK);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JLabel title = new JLabel("📅  Meeting Records Manager");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(TEXT_WHITE);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filters.setBackground(BG_DARK);
        filters.add(styledLabel("Search:"));
        tfSearch = styledTextField(18);
        tfSearch.setToolTipText("Search by type, agenda, or attendee name");
        filters.add(tfSearch);

        JButton btnSearch = actionButton("🔍 Search", ACCENT);
        btnSearch.addActionListener(e -> loadData(tfSearch.getText().trim()));
        filters.add(btnSearch);

        JButton btnClear = actionButton("✕ Clear", new Color(71, 85, 105));
        btnClear.addActionListener(e -> { tfSearch.setText(""); loadData(null); });
        filters.add(btnClear);

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

        // Row selection -> populate detail view
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showDetail();
        });

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, focus, row, col);
                setBackground(sel ? ROW_SEL : (row % 2 == 0 ? BG_CARD : ROW_ALT));
                setForeground(TEXT_WHITE);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                if (col == 1 && val != null && !sel) {
                    // Colour meeting type
                    switch (val.toString()) {
                        case "Gram Sabha"     -> setForeground(new Color(96,  165, 250));
                        case "Ward Committee" -> setForeground(new Color(52,  211, 153));
                        case "Emergency"      -> setForeground(new Color(248, 113, 113));
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

    // ─── Detail area (read-only) ─────────────────────────────────────────
    private JPanel buildDetailArea() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_DARK);
        p.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        JLabel lbl = new JLabel("  📄 Meeting Details (click a row above)");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setForeground(ACCENT);
        lbl.setBackground(new Color(25, 35, 55));
        lbl.setOpaque(true);
        lbl.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        taDetailView = new JTextArea();
        taDetailView.setEditable(false);
        taDetailView.setBackground(new Color(20, 30, 50));
        taDetailView.setForeground(TEXT_WHITE);
        taDetailView.setFont(new Font("Monospaced", Font.PLAIN, 12));
        taDetailView.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        taDetailView.setLineWrap(true);
        taDetailView.setWrapStyleWord(true);

        JScrollPane sp = new JScrollPane(taDetailView);
        sp.setBorder(null);
        sp.setBackground(BG_DARK);

        p.add(lbl, BorderLayout.NORTH);
        p.add(sp,  BorderLayout.CENTER);
        return p;
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

        JLabel formTitle = new JLabel("Record New Meeting");
        formTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        formTitle.setForeground(ACCENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill   = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 6;
        form.add(formTitle, gbc);
        gbc.gridwidth = 1;

        // Row 1: Type | Date
        cbMeetingType = new JComboBox<>(Meeting.MEETING_TYPES); styleCombo(cbMeetingType);
        tfDate = styledTextField(12);
        tfDate.setText(LocalDate.now().toString());
        tfDate.setToolTipText("Format: YYYY-MM-DD");

        gbc.gridy = 1; gbc.weighty = 0;
        gbc.gridx = 0; gbc.weightx = 0; form.add(styledLabel("Meeting Type*"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.5; form.add(cbMeetingType, gbc);
        gbc.gridx = 2; gbc.weightx = 0; form.add(styledLabel("Date*"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.5; form.add(tfDate, gbc);

        // Row 2 & 3: text areas in 2x2 grid
        taAttendees  = styledTextArea(3, 20);
        taAgenda     = styledTextArea(3, 30);
        taResolutions= styledTextArea(3, 30);
        taActionItems= styledTextArea(3, 30);

        // Attendees tip
        taAttendees.setToolTipText("One attendee per line");
        taActionItems.setToolTipText("Format per line: Task description - Responsible Person - Due Date");

        gbc.gridy = 2; gbc.weighty = 1;
        gbc.gridx = 0; gbc.weightx = 0; gbc.weighty = 0; form.add(styledLabel("Attendees* (one per line)"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.weighty = 1; form.add(new JScrollPane(taAttendees), gbc);
        gbc.gridx = 2; gbc.weightx = 0; gbc.weighty = 0; form.add(styledLabel("Agenda*"), gbc);
        gbc.gridx = 3; gbc.weightx = 1; gbc.weighty = 1; form.add(new JScrollPane(taAgenda), gbc);
        gbc.gridx = 4; gbc.weightx = 0; gbc.weighty = 0; form.add(styledLabel("Resolutions"), gbc);
        gbc.gridx = 5; gbc.weightx = 1; gbc.weighty = 1; form.add(new JScrollPane(taResolutions), gbc);

        gbc.gridy = 3;
        gbc.gridx = 0; gbc.weightx = 0; gbc.weighty = 0; form.add(styledLabel("Action Items"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 5; gbc.weightx = 1; gbc.weighty = 1;
        form.add(new JScrollPane(taActionItems), gbc);
        gbc.gridwidth = 1;

        wrapper.add(form, BorderLayout.CENTER);

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setBackground(BG_DARK);

        JButton btnSave   = actionButton("💾 Save Meeting",    ACCENT);
        JButton btnDelete = actionButton("🗑 Delete Selected", ACCENT_RED);
        JButton btnCsv    = actionButton("📥 Export CSV",       new Color(99, 102, 241));
        JButton btnPrint  = actionButton("🖨 Print Report",     new Color(245, 158, 11));
        JButton btnPdf    = actionButton("📄 Save as PDF",      new Color(220, 38, 38));

        btnSave.addActionListener(   e -> saveMeeting());
        btnDelete.addActionListener( e -> deleteSelected());
        btnCsv.addActionListener(    e -> ReportExporter.exportTableToCSV(table, this));
        btnPrint.addActionListener(  e -> printMeetingsReport());
        btnPdf.addActionListener(    e -> savePdfReport());

        btnRow.add(btnDelete);
        btnRow.add(btnPdf);
        btnRow.add(btnPrint);
        btnRow.add(btnCsv);
        btnRow.add(btnSave);
        wrapper.add(btnRow, BorderLayout.SOUTH);

        return wrapper;
    }

    // ─── Business logic ──────────────────────────────────────────────────

    private void saveMeeting() {
        String type      = cbMeetingType.getSelectedItem().toString();
        String date      = tfDate.getText().trim();
        String attendees = taAttendees.getText().trim();
        String agenda    = taAgenda.getText().trim();

        // Highlight all required fields
        if (!ReportExporter.validateRequiredFields(this, tfDate, taAttendees, taAgenda))
            return;

        // Strict calendar validation — catches invalid dates like 2024-02-30
        if (!ReportExporter.validateDate(date, tfDate, this)) return;

        // Convert newline-separated text to pipe-separated storage format
        String attendeesPiped   = attendees.replace("\n", "|").replace("\r", "");
        String actionItemsPiped = taActionItems.getText().trim().replace("\n", "|").replace("\r", "");

        Meeting m = new Meeting(type, date, attendeesPiped, agenda,
            taResolutions.getText().trim(), actionItemsPiped);

        try {
            int id = dao.insert(m);
            clearForm();
            loadData(null);
            JOptionPane.showMessageDialog(this, "✅  Meeting #" + id + " saved successfully!",
                "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Build and print a formatted meetings report. */
    private void printMeetingsReport() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No meeting data to print.",
                "Empty Table", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("SECTION: Meeting Records\n\n");
        sb.append(String.format("%-5s %-16s %-12s %-40s%n",
            "ID", "Type", "Date", "Agenda (preview)"));
        sb.append("─".repeat(75)).append("\n");
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            sb.append(String.format("%-5s %-16s %-12s %-40s%n",
                tableModel.getValueAt(r, 0),
                tableModel.getValueAt(r, 1),
                tableModel.getValueAt(r, 2),
                truncate(tableModel.getValueAt(r, 4).toString(), 40)));
        }
        sb.append("\nTotal meetings: ").append(tableModel.getRowCount());
        ReportExporter.printReport("Meeting Records Report", sb.toString(), this);
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private String buildReportBody() {
        StringBuilder sb = new StringBuilder();
        sb.append("SECTION: Meeting Records\n\n");
        sb.append(String.format("%-5s %-16s %-12s %-40s%n",
            "ID","Type","Date","Agenda (preview)"));
        sb.append("─".repeat(75)).append("\n");
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            sb.append(String.format("%-5s %-16s %-12s %-40s%n",
                tableModel.getValueAt(r,0), tableModel.getValueAt(r,1),
                tableModel.getValueAt(r,2),
                truncate(tableModel.getValueAt(r,4).toString(), 40)));
        }
        sb.append("\nTotal meetings: ").append(tableModel.getRowCount());
        return sb.toString();
    }

    private void savePdfReport() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No meeting data to export.",
                "Empty Table", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        PdfExporter.savePdf("Meeting Records Report", buildReportBody(), this);
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a meeting to delete.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete Meeting #" + id + "? This cannot be undone.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            dao.delete(id);
            taDetailView.setText("");
            loadData(null);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Show full details of the selected meeting in the detail panel. */
    private void showDetail() {
        int row = table.getSelectedRow();
        if (row < 0) { taDetailView.setText(""); return; }

        try {
            List<Meeting> all = dao.findAll();
            // Match by ID stored in column 0
            int id = (int) tableModel.getValueAt(row, 0);
            Meeting m = all.stream()
                .filter(x -> x.getId() == id)
                .findFirst().orElse(null);
            if (m == null) return;

            // Format attendees and action items for display
            String attendees = m.getAttendees() == null ? "" :
                m.getAttendees().replace("|", "\n  • ");
            String actionItems = m.getActionItems() == null || m.getActionItems().isBlank()
                ? "None" : m.getActionItems().replace("|", "\n  • ");

            taDetailView.setText(
                "Meeting Type : " + m.getMeetingType() + "\n" +
                "Date         : " + m.getDate() + "\n\n" +
                "Attendees:\n  • " + attendees + "\n\n" +
                "Agenda:\n" + m.getAgenda() + "\n\n" +
                "Resolutions:\n" + (m.getResolutions() == null || m.getResolutions().isBlank()
                    ? "None" : m.getResolutions()) + "\n\n" +
                "Action Items:\n  • " + actionItems
            );
            taDetailView.setCaretPosition(0);
        } catch (SQLException ex) {
            taDetailView.setText("Error loading details: " + ex.getMessage());
        }
    }

    private void loadData(String keyword) {
        tableModel.setRowCount(0);
        try {
            List<Meeting> list = dao.findAll(keyword);
            for (Meeting m : list) {
                // Truncate long agenda for table preview
                String agendaPreview = m.getAgenda().length() > 60
                    ? m.getAgenda().substring(0, 57) + "..." : m.getAgenda();
                String attendeeDisplay = m.getAttendees().replace("|", ", ");
                if (attendeeDisplay.length() > 40) attendeeDisplay = attendeeDisplay.substring(0, 37) + "...";

                tableModel.addRow(new Object[]{
                    m.getId(), m.getMeetingType(), m.getDate(),
                    attendeeDisplay, agendaPreview
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading data: " + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        cbMeetingType.setSelectedIndex(0);
        tfDate.setText(LocalDate.now().toString());
        taAttendees.setText(""); taAgenda.setText("");
        taResolutions.setText(""); taActionItems.setText("");
    }

    // ─── Style helpers ─────────────────────────────────────────────────
    private void styleTable(JTable t) {
        t.setBackground(BG_CARD); t.setForeground(TEXT_WHITE);
        t.setGridColor(new Color(51, 65, 85)); t.setRowHeight(28);
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        t.setShowHorizontalLines(true); t.setShowVerticalLines(false);
        t.setSelectionBackground(ROW_SEL); t.setSelectionForeground(Color.WHITE);
        t.getTableHeader().setBackground(new Color(20, 15, 45));
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
        tf.setCaretColor(TEXT_WHITE); tf.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tf.setBorder(new CompoundBorder(
            new LineBorder(new Color(71, 85, 105), 1, true),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        return tf;
    }

    private JTextArea styledTextArea(int rows, int cols) {
        JTextArea ta = new JTextArea(rows, cols);
        ta.setBackground(BG_INPUT); ta.setForeground(TEXT_WHITE);
        ta.setCaretColor(TEXT_WHITE); ta.setLineWrap(true); ta.setWrapStyleWord(true);
        ta.setFont(new Font("SansSerif", Font.PLAIN, 12));
        ta.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        return ta;
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
