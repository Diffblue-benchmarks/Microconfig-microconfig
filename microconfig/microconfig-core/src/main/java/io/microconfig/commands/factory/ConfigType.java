package io.microconfig.commands.factory;

import lombok.Getter;

import java.util.stream.Stream;

@Getter
public enum ConfigType {
    SERVICE("yaml", "service"),
    PROCESS("proc", "process"),
    SECRET("secret"),
    DEPENDENCIES("dependencies"),
    ENV("env"),
    LOG4j("log4j"),
    LOG4J2("log4j2"),
    SAP("sap");

    private final String configExtension;
    private final String configFileName;
    private final String resultFileName;

    ConfigType(String configExtension) {
        this(configExtension, configExtension);
    }

    ConfigType(String configExtension, String resultFileName) {
        this.configExtension = "." + configExtension;
        this.configFileName = resultFileName + this.configExtension;
        this.resultFileName = resultFileName + "." + System.getProperty("outputFormat", "properties");
    }

    public static ConfigType byExtension(String ext) {
        return Stream.of(values())
                .filter(ct -> ct.configExtension.equals(ext))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Can't find ConfigType for extension " + ext));
    }
}