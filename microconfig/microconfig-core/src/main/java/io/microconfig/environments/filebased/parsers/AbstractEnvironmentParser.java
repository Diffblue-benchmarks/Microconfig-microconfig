package io.microconfig.environments.filebased.parsers;

import io.microconfig.environments.Component;
import io.microconfig.environments.ComponentGroup;
import io.microconfig.environments.EnvInclude;
import io.microconfig.environments.Environment;
import io.microconfig.environments.filebased.EnvironmentParser;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Optional.*;
import static java.util.stream.Collectors.toList;

public abstract class AbstractEnvironmentParser implements EnvironmentParser {
    static final String IP = "ip";
    static final String PORT_OFFSET = "portOffset";
    static final String INCLUDE = "include";
    static final String INCLUDE_ENV = "env";
    static final String EXCLUDE = "exclude";
    static final String APPEND = "append";
    static final String COMPONENTS = "components";

    @Override
    public Environment parse(String name, String content) {
        Map<String, Object> map = toMap(content);

        Optional<EnvInclude> envInclude = parseInclude(map);
        Optional<Integer> portOffset = parsePortOffset(map);
        Optional<String> envIp = parseIp(map);
        List<ComponentGroup> componentGroups = parseComponentGroups(map, envIp);

        return new Environment(name, componentGroups, envIp, portOffset, envInclude);
    }

    protected abstract Map<String, Object> toMap(String content);

    @SuppressWarnings("unchecked")
    protected Optional<EnvInclude> parseInclude(Map<String, Object> map) {
        Map<String, Object> includeProps = (Map<String, Object>) map.remove(INCLUDE);
        if (includeProps == null) return empty();

        String name = (String) includeProps.get(INCLUDE_ENV);
        Collection<String> excludes = (Collection<String>) includeProps.getOrDefault(EXCLUDE, emptyList());
        return of(new EnvInclude(name, new LinkedHashSet<>(excludes)));
    }

    private Optional<Integer> parsePortOffset(Map<String, ?> map) {
        return ofNullable(map.remove(PORT_OFFSET))
                .map(Double.class::cast)
                .map(Double::intValue);
    }

    private Optional<String> parseIp(Map<String, ?> map) {
        return ofNullable(map.remove(IP)).map(Object::toString);
    }

    private List<ComponentGroup> parseComponentGroups(Map<String, Object> map, Optional<String> envIp) {
        return map.entrySet()
                .stream()
                .map(componentGroupDeclaration -> {
                    String componentGroupName = componentGroupDeclaration.getKey();
                    Map<String, Object> properties = (Map<String, Object>) componentGroupDeclaration.getValue();
                    Optional<String> ip = ofNullable((String) properties.getOrDefault(IP, envIp.orElse(null)));

                    List<Component> parsedComponents = fetchComponentsFromProperties(properties, COMPONENTS);
                    List<Component> excludedComponents = fetchComponentsFromProperties(properties, EXCLUDE);
                    List<Component> appendedComponents = fetchComponentsFromProperties(properties, APPEND);

                    return new ComponentGroup(componentGroupName, ip, parsedComponents, excludedComponents, appendedComponents);
                }).collect(toList());
    }

    @SuppressWarnings("unchecked")
    private List<Component> fetchComponentsFromProperties(Map<String, Object> properties, String property) {
        List<String> values = (List<String>) properties.get(property);
        return values == null ? emptyList() : parseComponents(values);
    }

    private List<Component> parseComponents(List<String> components) {
        return components.stream()
                .filter(Objects::nonNull)
                .map(s -> {
                    String[] parts = s.split(":");
                    if (parts.length > 2) throw new IllegalArgumentException("Incorrect component declaration: " + s);
                    return parts.length == 1 ? Component.byType(parts[0]) : Component.byNameAndType(parts[0], parts[1]);
                }).collect(toList());
    }
}