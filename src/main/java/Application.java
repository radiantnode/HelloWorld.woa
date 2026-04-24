import com.webobjects.appserver.WOApplication;

public class Application extends WOApplication {

    public static void main(String[] argv) {
        WOApplication.main(argv, Application.class);
    }

    public Application() {
        super();
        System.out.println("[WO] HelloWorld.woa started — " + name() + " v" + number());
    }
}
