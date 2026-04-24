import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class GuestbookEntry {
    private final String name;
    private final String email;
    private final String location;
    private final String message;
    private final Timestamp postedAt;

    public GuestbookEntry(String name, String email, String location,
                          String message, Timestamp postedAt) {
        this.name     = name;
        this.email    = (email    != null && !email.trim().isEmpty())    ? email.trim()    : null;
        this.location = (location != null && !location.trim().isEmpty()) ? location.trim() : null;
        this.message  = message;
        this.postedAt = postedAt;
    }

    public String name()     { return name; }
    public String email()    { return email; }
    public String location() { return location; }
    public String message()  { return message; }

    public boolean hasEmail()    { return email    != null; }
    public boolean hasLocation() { return location != null; }

    public String postedAt() {
        if (postedAt == null) return "";
        return new SimpleDateFormat("MMMM d, yyyy 'at' h:mm a").format(postedAt);
    }
}
