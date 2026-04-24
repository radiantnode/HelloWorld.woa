import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import java.util.List;

public class GuestbookPage extends WOComponent {

    public String authorName;
    public String email;
    public String location;
    public String message;

    public String nameError;
    public String emailError;
    public String messageError;

    public GuestbookEntry currentEntry;

    private int _visitorCount = -1;

    public GuestbookPage(WOContext context) {
        super(context);
    }

    public int visitorCount() {
        if (_visitorCount < 0) {
            _visitorCount = GuestbookDB.shared().incrementAndGetVisitorCount();
        }
        return _visitorCount;
    }

    public List<GuestbookEntry> entries() {
        return GuestbookDB.shared().allEntries();
    }

    public int entryCount() {
        return GuestbookDB.shared().entryCount();
    }

    public WOComponent submitEntry() {
        nameError = null;
        emailError = null;
        messageError = null;

        if (authorName == null || authorName.trim().isEmpty()) {
            nameError = "Name is required.";
        }
        if (email != null && !email.trim().isEmpty()
                && !email.trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            emailError = "Please enter a valid email address.";
        }
        if (message == null || message.trim().isEmpty()) {
            messageError = "Message is required.";
        }

        if (nameError != null || emailError != null || messageError != null) {
            return null;
        }

        GuestbookDB.shared().addEntry(authorName.trim(), email, location, message.trim());
        authorName = null; email = null; location = null; message = null;
        return pageWithName("GuestbookPage");
    }

    public WOComponent goHome() {
        return pageWithName("Main");
    }
}
