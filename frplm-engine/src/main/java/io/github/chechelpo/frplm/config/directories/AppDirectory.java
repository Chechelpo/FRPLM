package io.github.chechelpo.frplm.config.directories;

import java.nio.file.Path;

public record AppDirectory(Path root) {

    public AppDirectory {
        root = root.toAbsolutePath().normalize();
    }

    public Path data() {
        return root.resolve("data");
    }

    public Path assets() {
        return root.resolve("assets");
    }

    public Path plugins() {
        return root.resolve("plugins");
    }
}