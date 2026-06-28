package chechelpo.frplm.config;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {

                    @Override
                    protected Resource getResource(
                            String resourcePath,
                            Resource location
                    ) throws IOException {
                        Resource requestedResource =
                                location.createRelative(resourcePath);

                        /*
                         * Serve real files normally:
                         * /assets/index.js
                         * /header/globe.png
                         * /background-2.png
                         */
                        if (
                                requestedResource.exists()
                                        && requestedResource.isReadable()
                        ) {
                            return requestedResource;
                        }

                        /*
                         * Unknown API endpoints must remain 404 responses.
                         */
                        if (
                                resourcePath.equals("api")
                                        || resourcePath.startsWith("api/")
                        ) {
                            return null;
                        }

                        /*
                         * Do not return index.html for missing JS, CSS, images,
                         * source maps, or other static files.
                         */
                        if (resourcePath.contains(".")) {
                            return null;
                        }

                        /*
                         * All remaining extensionless paths are Vue routes.
                         */
                        Resource index =
                                location.createRelative("index.html");

                        return index.exists() && index.isReadable()
                                ? index
                                : null;
                    }
                });
    }
}