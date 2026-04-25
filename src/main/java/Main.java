import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;

public class Main extends WOComponent {

    public Main(WOContext context) {
        super(context);
    }

    public String sessionID() {
        return session().sessionID();
    }

    public String appName() {
        return application().name();
    }

    public WOComponent guestbook() {
        return pageWithName("GuestbookPage");
    }

    public WOComponent stats() {
        return pageWithName("StatsPage");
    }
}
