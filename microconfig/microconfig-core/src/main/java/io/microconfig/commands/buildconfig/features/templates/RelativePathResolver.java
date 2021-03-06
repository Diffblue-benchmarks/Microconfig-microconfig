package io.microconfig.commands.buildconfig.features.templates;

import java.io.File;

public interface RelativePathResolver {
    File overrideRelativePath(File serviceConfigDir, String path);

    static RelativePathResolver empty() {
        return (serviceConfigDir, path) -> new File(path);
    }
}
