package panchayat.dao;

import panchayat.db.DatabaseManager;
import panchayat.model.Meeting;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * MeetingDAO — Data Access Object for Panchayat meeting records.
 *
 * The keyword search in findAll() uses a LIKE clause across meeting_type,
 * agenda, and attendees columns so staff can quickly find a meeting
 * by any of these natural identifiers.
 */
public class MeetingDAO {

    /** Insert a new meeting record; returns generated id. */
    public int insert(Meeting m) throws SQLException {
        String sql = """
            INSERT INTO meetings
              (meeting_type, date, attendees, agenda, resolutions, action_items)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = DatabaseManager.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getMeetingType());
            ps.setString(2, m.getDate());
            ps.setString(3, m.getAttendees());
            ps.setString(4, m.getAgenda());
            ps.setString(5, m.getResolutions());
            ps.setString(6, m.getActionItems());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    /** Delete a meeting record by id. */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM meetings WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Fetch meetings with an optional keyword search.
     * The search is applied across type, agenda, and attendees columns.
     */
    public List<Meeting> findAll(String keyword) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT id, meeting_type, date, attendees, agenda, resolutions, action_items " +
            "FROM meetings WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (meeting_type LIKE ? OR agenda LIKE ? OR attendees LIKE ?)");
            String kw = "%" + keyword + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        sql.append(" ORDER BY date DESC, id DESC");

        List<Meeting> list = new ArrayList<>();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Meeting(
                        rs.getInt("id"),
                        rs.getString("meeting_type"),
                        rs.getString("date"),
                        rs.getString("attendees"),
                        rs.getString("agenda"),
                        rs.getString("resolutions"),
                        rs.getString("action_items")
                    ));
                }
            }
        }
        return list;
    }

    /** No-filter variant. */
    public List<Meeting> findAll() throws SQLException {
        return findAll(null);
    }

    /**
     * Count meetings held in the current calendar month.
     * SQLite's strftime() lets us compare stored "YYYY-MM-DD" strings
     * against the current year-month without parsing them in Java.
     */
    public int countThisMonth() throws SQLException {
        String currentYearMonth = LocalDate.now().toString().substring(0, 7); // "YYYY-MM"
        String sql = "SELECT COUNT(*) FROM meetings WHERE strftime('%Y-%m', date) = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, currentYearMonth);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Count total pending action items across all meetings.
     * Action items are stored as pipe-separated strings; we count non-empty
     * rows as a proxy — a simple heuristic good enough for the dashboard.
     */
    public int countPendingActionItems() throws SQLException {
        String sql = "SELECT action_items FROM meetings WHERE action_items IS NOT NULL AND action_items != ''";
        int count = 0;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String items = rs.getString("action_items");
                if (items != null && !items.isBlank()) {
                    count += items.split("\\|").length;
                }
            }
        }
        return count;
    }
}
