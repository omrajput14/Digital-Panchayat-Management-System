package panchayat.dao;

import panchayat.db.DatabaseManager;
import panchayat.model.Complaint;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ComplaintDAO — Data Access Object for the complaints table.
 *
 * All SQL lives here, keeping view classes free of database concerns.
 * Prepared statements are used throughout to prevent SQL injection and
 * improve performance via statement caching.
 */
public class ComplaintDAO {

    /**
     * Inserts a new complaint and returns the generated primary key.
     * The auto-incremented id is needed to immediately update the model
     * object so the caller can display it in the table without a round-trip.
     */
    public int insert(Complaint c) throws SQLException {
        String sql = """
            INSERT INTO complaints
              (citizen_name, ward_number, category, description, date_filed, status)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = DatabaseManager.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getCitizenName());
            ps.setString(2, c.getWardNumber());
            ps.setString(3, c.getCategory());
            ps.setString(4, c.getDescription());
            ps.setString(5, c.getDateFiled());
            ps.setString(6, c.getStatus());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    /**
     * Updates only the status column for a given complaint id.
     * Targeted update instead of updating the whole row is more efficient
     * and avoids accidentally overwriting concurrent edits.
     */
    public void updateStatus(int id, String newStatus) throws SQLException {
        String sql = "UPDATE complaints SET status = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * Deletes a complaint by id.
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM complaints WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Returns all complaints, optionally filtered by status and/or ward.
     * Null or blank filter values are treated as "show all" — this keeps
     * the calling view code simple: just pass null to skip a filter.
     */
    public List<Complaint> findAll(String statusFilter, String wardFilter) throws SQLException {
        // Build WHERE clause dynamically based on active filters
        StringBuilder sql = new StringBuilder(
            "SELECT id, citizen_name, ward_number, category, description, date_filed, status " +
            "FROM complaints WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (statusFilter != null && !statusFilter.isBlank() && !statusFilter.equals("All")) {
            sql.append(" AND status = ?");
            params.add(statusFilter);
        }
        if (wardFilter != null && !wardFilter.isBlank()) {
            sql.append(" AND ward_number LIKE ?");
            params.add("%" + wardFilter + "%");
        }
        sql.append(" ORDER BY id DESC");

        List<Complaint> list = new ArrayList<>();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Complaint(
                        rs.getInt("id"),
                        rs.getString("citizen_name"),
                        rs.getString("ward_number"),
                        rs.getString("category"),
                        rs.getString("description"),
                        rs.getString("date_filed"),
                        rs.getString("status")
                    ));
                }
            }
        }
        return list;
    }

    /** Convenience — returns all without filters (used by dashboard). */
    public List<Complaint> findAll() throws SQLException {
        return findAll(null, null);
    }

    /** Count complaints by status — used by the Summary Dashboard. */
    public int countByStatus(String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM complaints WHERE status = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
