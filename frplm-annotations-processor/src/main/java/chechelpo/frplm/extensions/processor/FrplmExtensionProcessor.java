package chechelpo.frplm.extensions.processor;

import chechelpo.frplm.extensions.api.annotations.FrplmExtension;
import chechelpo.frplm.extensions.api.types.Extension;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.Set;

@SupportedAnnotationTypes("chechelpo.frplm.extensions.api.annotations.FrplmExtension")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class FrplmExtensionProcessor extends AbstractProcessor {
    private static final String SERVICE_FILE =
            "META-INF/services/chechelpo.frplm.extensions.api.types.Extension";

    private final Set<String> providers = new LinkedHashSet<>();

    @Override
    public boolean process(
            Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv
    ) {
        System.out.println("FrplmExtensionProcessor process");
        TypeElement extensionType = processingEnv.getElementUtils()
                .getTypeElement(Extension.class.getCanonicalName());

        if (extensionType == null) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Could not resolve Extension type"
            );
            return true;
        }

        TypeMirror extensionMirror = extensionType.asType();

        for (Element element : roundEnv.getElementsAnnotatedWith(FrplmExtension.class)) {
            if (!(element instanceof TypeElement typeElement)) {
                continue;
            }

            if (!processingEnv.getTypeUtils()
                    .isAssignable(typeElement.asType(), extensionMirror)) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "@FrplmExtension can only be used on subclasses of Extension",
                        element
                );
                continue;
            }

            providers.add(typeElement.getQualifiedName().toString());
        }

        if (roundEnv.processingOver() && !providers.isEmpty()) {
            writeServiceFile();
        }

        return true;
    }

    private void writeServiceFile() {
        Filer filer = processingEnv.getFiler();

        try {
            FileObject file = filer.createResource(
                    StandardLocation.CLASS_OUTPUT,
                    "",
                    SERVICE_FILE
            );

            try (Writer writer = file.openWriter()) {
                for (String provider : providers) {
                    writer.write(provider);
                    writer.write(System.lineSeparator());
                }
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Failed to generate " + SERVICE_FILE + ": " + e.getMessage()
            );
        }
    }
}