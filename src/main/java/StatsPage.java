import com.webobjects.appserver.*;
import java.lang.management.*;
import java.util.List;
import java.io.*;
import java.nio.file.*;

public class StatsPage extends WOComponent {

    public StatsPage(WOContext context) {
        super(context);
    }

    public WOComponent goHome() {
        return pageWithName("Main");
    }

    // --- JVM ---

    public String jvmUptime() {
        long ms = ManagementFactory.getRuntimeMXBean().getUptime();
        long s = ms / 1000, m = s / 60, h = m / 60, d = h / 24;
        if (d > 0) return d + "d " + (h % 24) + "h " + (m % 60) + "m";
        if (h > 0) return h + "h " + (m % 60) + "m " + (s % 60) + "s";
        if (m > 0) return m + "m " + (s % 60) + "s";
        return s + "s";
    }

    public String vmName() {
        return ManagementFactory.getRuntimeMXBean().getVmName();
    }

    public String javaVersion() {
        return System.getProperty("java.version");
    }

    public int availableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    public String systemLoad() {
        double load = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        return load >= 0 ? String.format("%.2f", load) : "N/A";
    }

    public long heapUsedMB() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() / 1048576;
    }

    public long heapMaxMB() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax() / 1048576;
    }

    public int heapPercent() {
        MemoryUsage h = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        if (h.getMax() <= 0) return 0;
        return (int) (h.getUsed() * 100 / h.getMax());
    }

    public String heapBarStyle() {
        int pct = heapPercent();
        String color = pct > 85 ? "#c03030" : pct > 70 ? "#c07820" : "#2a8a2a";
        return "width:" + pct + "%;background:" + color;
    }

    public long nonHeapUsedMB() {
        return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed() / 1048576;
    }

    public int loadedClasses() {
        return ManagementFactory.getClassLoadingMXBean().getLoadedClassCount();
    }

    public String gcSummary() {
        long collections = 0, pauseMs = 0;
        String names = "";
        List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gc : gcs) {
            if (gc.getCollectionCount() > 0) collections += gc.getCollectionCount();
            if (gc.getCollectionTime()  > 0) pauseMs    += gc.getCollectionTime();
            names = gc.getName();
        }
        return names + " — " + collections + " collections, " + pauseMs + "ms pause";
    }

    public String threads() {
        ThreadMXBean t = ManagementFactory.getThreadMXBean();
        return t.getThreadCount() + " live / " + t.getPeakThreadCount() + " peak";
    }

    // --- WebObjects ---

    public String appName() {
        return application().name();
    }

    public String appVersion() {
        String n = application().number();
        return (n == null || n.equals("-1")) ? "1.0" : n;
    }

    public int totalSessions() {
        return Application.app().totalSessions();
    }

    public String gitRevision() {
        try {
            String rev = new String(Files.readAllBytes(Paths.get("/opt/woapps/HelloWorld.woa/REVISION"))).trim();
            return rev.isEmpty() ? "unknown" : rev;
        } catch (IOException e) { return "unknown"; }
    }

    public String gitRevisionShort() {
        String rev = gitRevision();
        return (rev.length() > 7) ? rev.substring(0, 7) : rev;
    }

    public String gitRevisionUrl() {
        String rev = gitRevision();
        if (rev.equals("unknown")) return "https://github.com/radiantnode/HelloWorld.woa";
        return "https://github.com/radiantnode/HelloWorld.woa/commit/" + rev;
    }

    // --- Guestbook ---

    public int guestbookEntries() {
        return GuestbookDB.shared().entryCount();
    }

    public int totalVisitors() {
        return GuestbookDB.shared().currentVisitorCount();
    }
}
