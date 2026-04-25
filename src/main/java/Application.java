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

    public WOSession createSessionForRequest(WORequest request) {
        WOSession session = super.createSessionForRequest(request);
        synchronized (this) { _totalSessions++; }
        return session;
    }

    public int totalSessions() { return _totalSessions; }
}
