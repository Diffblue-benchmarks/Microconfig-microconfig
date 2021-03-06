package io.microconfig.commands.buildconfig.features.templates;

import io.microconfig.commands.buildconfig.BuildConfigPostProcessor;
import io.microconfig.configs.ConfigProvider;
import io.microconfig.configs.Property;
import io.microconfig.configs.resolver.EnvComponent;
import io.microconfig.configs.resolver.PropertyResolver;
import io.microconfig.configs.resolver.PropertyResolverHolder;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Map;

import static io.microconfig.configs.Property.asStringMap;

@RequiredArgsConstructor
public class CopyTemplatesPostProcessor implements BuildConfigPostProcessor {
    private final CopyTemplatesService copyTemplatesService;

    @Override
    public void process(EnvComponent currentComponent,
                        Map<String, Property> componentProperties,
                        ConfigProvider configProvider, File resultFile) {
        if (configProvider instanceof PropertyResolverHolder) {
            PropertyResolver resolver = ((PropertyResolverHolder) configProvider).getResolver();
            copyTemplatesService.copyTemplates(currentComponent, resultFile.getParentFile(), asStringMap(componentProperties), resolver);
        }
    }
}