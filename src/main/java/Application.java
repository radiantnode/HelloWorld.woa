import com.webobjects.appserver.*;

public class Application extends WOApplication {

    private int _totalSessions = 0;

    public static Application app() {
        return (Application) WOApplication.application();
    }

    public static void main(String[] argv) {
        WOApplication.main(argv, Application.class);
    }

    public Application() {
        super();
        System.out.println("[WO] HelloWorld.woa started — " + name() + " v" + number());
    }

    private WOResponse expiredSessionResponse(WOContext ctx) {
        Main page = (Main) pageWithName("Main", ctx);
        page.warningMessage = "Your session has expired. Please try again.";
        return page.generateResponse();
    }

    public WOResponse handleSessionCreationErrorInContext(WOContext ctx) {
        return expiredSessionResponse(ctx);
    }

    public WOResponse handleSessionRestorationErrorInContext(WOContext ctx) {
        return expiredSessionResponse(ctx);
    }

    public WOResponse handlePageRestorationErrorInContext(WOContext ctx) {
        return expiredSessionResponse(ctx);
    }

    public WOSession createSessionForRequest(WORequest request) {
        WOSession session = super.createSessionForRequest(request);
        synchronized (this) { _totalSessions++; }
        return session;
    }

    public int totalSessions() { return _totalSessions; }
}
