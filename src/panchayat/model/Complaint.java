package panchayat.model;

/**
 * Complaint — Plain-old-Java-object (POJO) representing one citizen complaint.
 *
 * Keeping model classes free of database or UI logic follows the
 * Single-Responsibility Principle: this class only knows how to hold
 * and expose complaint data.
 */
public class Complaint {

    // ----- Valid status constants -----
    public static final String STATUS_PENDING     = "Pending";
    public static final String STATUS_IN_PROGRESS = "In Progress";
    public static final String STATUS_RESOLVED    = "Resolved";

    // ----- Valid category constants -----
    public static final String[] CATEGORIES = {
        "Roads", "Water", "Electricity", "Sanitation", "Other"
    };

    private int    id;
    private String citizenName;
    private String wardNumber;
    private String category;
    private String description;
    private String dateFiled;   // stored as ISO-8601 text "YYYY-MM-DD"
    private String status;

    /** Full constructor used when loading from DB (id is known). */
    public Complaint(int id, String citizenName, String wardNumber,
                     String category, String description,
                     String dateFiled, String status) {
        this.id          = id;
        this.citizenName = citizenName;
        this.wardNumber  = wardNumber;
        this.category    = category;
        this.description = description;
        this.dateFiled   = dateFiled;
        this.status      = status;
    }

    /** Convenience constructor for new complaints (id will be assigned by DB). */
    public Complaint(String citizenName, String wardNumber,
                     String category, String description,
                     String dateFiled, String status) {
        this(0, citizenName, wardNumber, category, description, dateFiled, status);
    }

    // ---- Getters ----
    public int    getId()          { return id; }
    public String getCitizenName() { return citizenName; }
    public String getWardNumber()  { return wardNumber; }
    public String getCategory()    { return category; }
    public String getDescription() { return description; }
    public String getDateFiled()   { return dateFiled; }
    public String getStatus()      { return status; }

    // ---- Setters for mutable fields ----
    public void setId(int id)           { this.id = id; }
    public void setStatus(String status){ this.status = status; }

    @Override
    public String toString() {
        return String.format("Complaint[id=%d, citizen=%s, ward=%s, status=%s]",
                id, citizenName, wardNumber, status);
    }
}
