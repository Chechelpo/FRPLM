package chechelpo.frplm.extensions;

import io.github.chechelpo.frplm.extensions.api.types.Extension;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

@Component
final class ServiceLoaderExtensionDiscovery {

    private static final Path PLUGINS_DIR = resolvePluginsDir();
    private static Path resolvePluginsDir() {
        Path codeSource = resolveCodeSourcePath();

        if (codeSource != null) {
            if (Files.isRegularFile(codeSource)) {
                return codeSource.getParent()
                        .resolve("plugins")
                        .toAbsolutePath()
                        .normalize();
            }

            Path discovered = findUpward(codeSource, "plugins");

            if (discovered != null) {
                return discovered;
            }
        }

        return Path.of("plugins").toAbsolutePath().normalize();
    }

    private static Path resolveCodeSourcePath() {
        try {
            return Path.of(
                    ServiceLoaderExtensionDiscovery.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            ).toAbsolutePath().normalize();
        } catch (Exception e) {
            return null;
        }
    }

    private static Path findUpward(Path start, String directoryName) {
        Path current = Files.isRegularFile(start)
                ? start.getParent()
                : start;

        while (current != null) {
            Path candidate = current.resolve(directoryName);

            if (Files.isDirectory(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }

            current = current.getParent();
        }

        return null;
    }
    public @NonNull @Unmodifiable List<Extension> discoverExtensions() {
        ClassLoader pluginClassLoader = createPluginClassLoader();

        return ServiceLoader.load(Extension.class, pluginClassLoader)
                .stream()
                .map(provider -> {
                    try {
                        return provider.get();
                    } catch (Exception e) {
                        System.err.println("Error instantiating extension " + provider.type().getName());
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private ClassLoader createPluginClassLoader() {
        try {
            if (!Files.isDirectory(PLUGINS_DIR)) {
                return Thread.currentThread().getContextClassLoader();
            }

            URL[] jarUrls;

            try (var stream = Files.list(PLUGINS_DIR)) {
                jarUrls = stream
                        .filter(path -> path.toString().endsWith(".jar"))
                        .map(ServiceLoaderExtensionDiscovery::toUrl)
                        .toArray(URL[]::new);
            }

            return new URLClassLoader(
                    jarUrls,
                    Thread.currentThread().getContextClassLoader()
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan plugins directory: " + PLUGINS_DIR, e);
        }
    }

    private static URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (Exception e) {
            throw new IllegalStateException("Invalid plugin jar path: " + path, e);
        }
    }
}