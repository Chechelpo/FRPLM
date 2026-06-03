package chechelpo.frplm.extensions;

import chechelpo.frplm.extensions.api.types.Extension;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ServiceLoader;

@Component
final class ServiceLoaderExtensionDiscovery {

    public List<Extension> discoverExtensions() {
        return ServiceLoader.load(Extension.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .toList();
    }
}