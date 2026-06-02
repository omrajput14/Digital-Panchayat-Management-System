package panchayat.dao;

import panchayat.db.DatabaseManager;
import panchayat.model.Issue;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * IssueDAO — Data Access Object for infrastructure issues.
 *
 * Mirrors the pattern established in ComplaintDAO: prepared statements,
 * dynamic filtering, and targeted updates to keep the DB I/O minimal.
 */
public class IssueDAO {

    /** Insert a new issue; returns the generated id. */
    public int insert(Issue issue) throws SQLException {
        String sql = """
            INSERT INTO issues
              (title, ward, type, severity, assigned_officer,
               date_reported, expected_resolution, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = DatabaseManager.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, issue.getTitle());
            ps.setString(2, issue.getWard());
            ps.setString(3, issue.getType());
            ps.setString(4, issue.getSeverity());
            ps.setString(5, issue.getAssignedOfficer());
            ps.setString(6, issue.getDateReported());
            ps.setString(7, issue.getExpectedResolution());
            ps.setString(8, issue.getStatus());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    /** Update only the status of an existing issue. */
    public void updateStatus(int id, String newStatus) throws SQLException {
        String sql = "UPDATE issues SET status = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /** Delete an issue record. */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM issues WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Fetch all issues with optional severity / ward filters.
     * Results are ordered newest-first by id.
     */
    public List<Issue> findAll(String severityFilter, String wardFilter) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT id, title, ward, type, severity, assigned_officer, " +
            "date_reported, expected_resolution, status FROM issues WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (severityFilter != null && !severityFilter.isBlank() && !severityFilter.equals("All")) {
            sql.append(" AND severity = ?");
            params.add(severityFilter);
        }
        if (wardFilter != null && !wardFilter.isBlank()) {
            sql.append(" AND ward LIKE ?");
            params.add("%" + wardFilter + "%");
        }
        sql.append(" ORDER BY id DESC");

        List<Issue> list = new ArrayList<>();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Issue(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("ward"),
                        rs.getString("type"),
                        rs.getString("severity"),
                        rs.getString("assigned_officer"),
                        rs.getString("date_reported"),
                        rs.getString("expected_resolution"),
                        rs.getString("status")
                    ));
                }
            }
        }
        return list;
    }

    /** No-filter variant used by the dashboard. */
    public List<Issue> findAll() throws SQLException {
        return findAll(null, null);
    }

    /** Count issues by severity for the summary dashboard. */
    public int countBySeverity(String severity) throws SQLException {
        String sql = "SELECT COUNT(*) FROM issues WHERE severity = ? AND status != 'Resolved'";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, severity);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Count total open (non-resolved) issues for dashboard. */
    public int countOpen() throws SQLException {
        String sql = "SELECT COUNT(*) FROM issues WHERE status != 'Resolved'";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
