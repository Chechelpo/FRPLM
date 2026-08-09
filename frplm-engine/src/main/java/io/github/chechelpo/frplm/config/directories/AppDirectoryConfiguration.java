package io.github.chechelpo.frplm.config.directories;

import io.github.chechelpo.frplm.FRPLMEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration(proxyBeanMethods = false)
public class AppDirectoryConfiguration {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AppDirectoryConfiguration.class);

    @Bean
    AppDirectory appDirectory(Environment environment) {
        Path root;

        if (environment.acceptsProfiles(Profiles.of("production"))) {
            root = new ApplicationHome(FRPLMEngine.class)
                    .getDir()
                    .toPath();
        } else {
            root = Path.of("")
                    .toAbsolutePath()
                    .normalize();
        }

        return createAppDirectory(root);
    }

    private AppDirectory createAppDirectory(Path root) {
        AppDirectory directories = new AppDirectory(
                root.toAbsolutePath().normalize()
        );

        createDirectory(directories.assets());
        createDirectory(directories.plugins());
        createDirectory(directories.data());

        LOGGER.info("Application root: {}", directories.root());
        LOGGER.info("Data directory: {}", directories.data());
        LOGGER.info("Assets directory: {}", directories.assets());
        LOGGER.info("Plugins directory: {}", directories.plugins());

        return directories;
    }

    private static void createDirectory(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Could not create directory: " + directory,
                    exception
            );
        }
    }
}