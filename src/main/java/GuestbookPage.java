import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import java.util.List;

public class GuestbookPage extends WOComponent {

    public String authorName;
    public String email;
    public String location;
    public String message;

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
        if (authorName != null && !authorName.trim().isEmpty()
                && message != null && !message.trim().isEmpty()) {
            GuestbookDB.shared().addEntry(authorName.trim(), email, location, message.trim());
        }
        authorName = null; email = null; location = null; message = null;
        return pageWithName("GuestbookPage");
    }

    public WOComponent goHome() {
        return pageWithName("Main");
    }
}
