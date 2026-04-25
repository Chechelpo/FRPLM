package chechelpo.demo.asset;

import java.nio.file.Path;

/**
 * Decides the directory for the app based on host system.
 */
public final class AppDirectory {
    public static final Path APP_DIRECTORY;
    private static final String APP_NAME = "demo";
    static {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData == null) {
                throw new IllegalStateException("APPDATA not set");
            }
            APP_DIRECTORY = Path.of(appData, APP_NAME);
        } else {
            String home = System.getProperty("user.home");
            APP_DIRECTORY = Path.of(home, ".local", "share", APP_NAME);
        }
        System.out.println("App persisting data to " + APP_DIRECTORY);
    }

    private AppDirectory() {}
}
