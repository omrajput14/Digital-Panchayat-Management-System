package panchayat.model;

/**
 * Issue — POJO representing one infrastructure issue logged by staff.
 *
 * The expectedResolution field is compared against today's date in the
 * view layer to colour-code overdue rows — keeping that logic in the
 * view ensures the model stays a pure data carrier.
 */
public class Issue {

    public static final String[] TYPES      = {"Road", "Water Supply", "Drainage", "Street Light"};
    public static final String[] SEVERITIES = {"Low", "Medium", "High"};
    public static final String[] STATUSES   = {"Open", "In Progress", "Resolved"};

    private int    id;
    private String title;
    private String ward;
    private String type;
    private String severity;
    private String assignedOfficer;
    private String dateReported;        // "YYYY-MM-DD"
    private String expectedResolution;  // "YYYY-MM-DD"
    private String status;

    /** Full constructor (id known — loaded from DB). */
    public Issue(int id, String title, String ward, String type,
                 String severity, String assignedOfficer,
                 String dateReported, String expectedResolution, String status) {
        this.id                 = id;
        this.title              = title;
        this.ward               = ward;
        this.type               = type;
        this.severity           = severity;
        this.assignedOfficer    = assignedOfficer;
        this.dateReported       = dateReported;
        this.expectedResolution = expectedResolution;
        this.status             = status;
    }

    /** New issue constructor (id will come from DB auto-increment). */
    public Issue(String title, String ward, String type,
                 String severity, String assignedOfficer,
                 String dateReported, String expectedResolution, String status) {
        this(0, title, ward, type, severity, assignedOfficer,
             dateReported, expectedResolution, status);
    }

    // ---- Getters ----
    public int    getId()                 { return id; }
    public String getTitle()              { return title; }
    public String getWard()               { return ward; }
    public String getType()               { return type; }
    public String getSeverity()           { return severity; }
    public String getAssignedOfficer()    { return assignedOfficer; }
    public String getDateReported()       { return dateReported; }
    public String getExpectedResolution() { return expectedResolution; }
    public String getStatus()             { return status; }

    // ---- Setters ----
    public void setId(int id)            { this.id = id; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("Issue[id=%d, title=%s, severity=%s, status=%s]",
                id, title, severity, status);
    }
}
