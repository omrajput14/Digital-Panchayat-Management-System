package panchayat.model;

/**
 * Meeting — POJO for a single Panchayat meeting record.
 *
 * Multi-value fields (attendees, action items) are stored as delimited
 * strings in SQLite ("|" separator) — a pragmatic trade-off that avoids
 * extra join tables while keeping the schema simple for a desktop app
 * used by a single office.
 */
public class Meeting {

    public static final String[] MEETING_TYPES = {
        "Gram Sabha", "Ward Committee", "Emergency"
    };

    private int    id;
    private String meetingType;
    private String date;           // "YYYY-MM-DD"
    private String attendees;      // pipe-separated names: "Ram|Shyam|Geeta"
    private String agenda;
    private String resolutions;    // free text, may be empty
    private String actionItems;    // pipe-separated: "Fix road - Ram - 2024-07-01|..."

    /** Full constructor — id known (loaded from DB). */
    public Meeting(int id, String meetingType, String date,
                   String attendees, String agenda,
                   String resolutions, String actionItems) {
        this.id          = id;
        this.meetingType = meetingType;
        this.date        = date;
        this.attendees   = attendees;
        this.agenda      = agenda;
        this.resolutions = resolutions;
        this.actionItems = actionItems;
    }

    /** New meeting constructor. */
    public Meeting(String meetingType, String date,
                   String attendees, String agenda,
                   String resolutions, String actionItems) {
        this(0, meetingType, date, attendees, agenda, resolutions, actionItems);
    }

    // ---- Getters ----
    public int    getId()          { return id; }
    public String getMeetingType() { return meetingType; }
    public String getDate()        { return date; }
    public String getAttendees()   { return attendees; }
    public String getAgenda()      { return agenda; }
    public String getResolutions() { return resolutions; }
    public String getActionItems() { return actionItems; }

    public void setId(int id)      { this.id = id; }

    /** Returns attendee count for dashboard statistics. */
    public int getAttendeeCount() {
        if (attendees == null || attendees.isBlank()) return 0;
        return attendees.split("\\|").length;
    }

    @Override
    public String toString() {
        return String.format("Meeting[id=%d, type=%s, date=%s]",
                id, meetingType, date);
    }
}
