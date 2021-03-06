package io.microconfig.commands.buildconfig.features.templates;

import io.microconfig.configs.resolver.EnvComponent;
import io.microconfig.configs.resolver.PropertyResolver;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.microconfig.commands.buildconfig.features.templates.RelativePathResolver.empty;
import static io.microconfig.commands.buildconfig.features.templates.TemplatePattern.defaultPattern;
import static io.microconfig.utils.FilePermissionUtils.copyPermissions;
import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static io.microconfig.utils.FileUtils.write;
import static io.microconfig.utils.Logger.*;

@RequiredArgsConstructor
public class CopyTemplatesServiceImpl implements CopyTemplatesService {
    private final TemplatePattern templatePattern;
    private final RelativePathResolver relativePathResolver;

    public CopyTemplatesServiceImpl() {
        this(defaultPattern(), empty());
    }

    @Override
    public void copyTemplates(EnvComponent currentComponent,
                              File serviceDestinationDir,
                              Map<String, String> componentProperties,
                              PropertyResolver propertyResolver) {
        collectTemplates(componentProperties).forEach(def -> {
            try {
                def.resolveAndCopy(propertyResolver, currentComponent, serviceDestinationDir);
            } catch (RuntimeException e) {
                error("Template error: " + def, e);
            }
        });
    }

    private Collection<TemplateDefinition> collectTemplates(Map<String, String> serviceProperties) {
        Map<String, TemplateDefinition> templateByName = new LinkedHashMap<>();

        serviceProperties.forEach((key, value) -> {
            if (!key.startsWith(templatePattern.getTemplatePrefix())) return;

            String fromFileSuffix = templatePattern.getFromFileSuffix();
            String toFileSuffix = templatePattern.getToFileSuffix();
            if (key.endsWith(fromFileSuffix)) {
                if (value.trim().isEmpty()) {
                    info("Ignoring template '" + key + "' cause value is empty");
                    return;
                }
                getOrCreate(key, fromFileSuffix, templateByName).setFromFile(value);
            } else if (key.endsWith(toFileSuffix)) {
                getOrCreate(key, toFileSuffix, templateByName).setToFile(value);
            }
        });

        return templateByName.values();
    }

    private TemplateDefinition getOrCreate(String key, String suffix, Map<String, TemplateDefinition> templates) {
        return templates.computeIfAbsent(extractTemplateName(key, suffix), TemplateDefinition::new);
    }

    private String extractTemplateName(String str, String suffix) {
        try {
            return str.substring(templatePattern.getTemplatePrefix().length(), str.length() - suffix.length());
        } catch (RuntimeException e) {
            throw new RuntimeException("Incorrect template: " + str);
        }
    }

    @RequiredArgsConstructor
    private class TemplateDefinition {
        private final String name;

        private String fromFile;
        private String toFile;

        private void resolveAndCopy(PropertyResolver propertyResolver, EnvComponent currentComponent, File destinationDir) {
            if (!isCorrect()) {
                warn("Incomplete template def " + this);
                return;
            }

            File fromFile = absoluteFromFile(destinationDir);
            File toFile = absoluteToFile(destinationDir);

            Template template = toTemplate(fromFile, currentComponent.getComponent().getName());
            if (template == null) return;

            String content = template.resolvePlaceholders(currentComponent, propertyResolver, templatePattern.getPattern());
            write(toFile, content);
            copyPermissions(fromFile.toPath(), toFile.toPath());

            info("Copied template: " + fromFile + " -> " + toFile);
        }

        private boolean isCorrect() {
            return fromFile != null && toFile != null;
        }

        private File absoluteFromFile(File serviceDir) {
            File path = relativePathResolver.overrideRelativePath(serviceDir, fromFile);
            if (!path.isAbsolute()) {
                throw new IllegalArgumentException("Using relative path for template '" + fromFile + "' for component '" + serviceDir.getName() + "'. "
                        + "Template path must be absolute. Consider using '${this@configRoot}\\..' or '${component_name@configDir}\\..' to build absolute path");
            }
            return path;
        }

        private File absoluteToFile(File serviceDir) {
            File path = relativePathResolver.overrideRelativePath(serviceDir, toFile);
            return path.isAbsolute() ? path : new File(serviceDir, path.getPath());
        }

        private Template toTemplate(File fromFile, String component) {
            if (!fromFile.exists() || !fromFile.isFile()) {
                warn("Missing file to copy. " + this + ". Service: " + component);
                return null;
            }

            try {
                return new Template(fromFile);
            } catch (RuntimeException e) {
                warn("Cannot read fromFile. " + this);
                return null;
            }
        }

        public void setFromFile(String fromFile) {
            this.fromFile = fromFile;
            if (this.toFile == null) {
                this.toFile = new File(fromFile).getName();
            }
        }

        public void setToFile(String toFile) {
            this.toFile = toFile;
        }

        @Override
        public String toString() {
            return "templateName: '" + name + "', file: '" + fromFile + "' -> '" + toFile + "'";
        }
    }
}