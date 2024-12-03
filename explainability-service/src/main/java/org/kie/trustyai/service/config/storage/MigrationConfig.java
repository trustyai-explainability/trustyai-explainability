package org.kie.trustyai.service.config.storage;

public class MigrationConfig {
    private final String fromFolder;
    private final String fromFilename;

    public MigrationConfig(String fromFolder, String fromFilename) {
        this.fromFilename = fromFilename;
        this.fromFolder = fromFolder;
    }

    public String getFromFolder() {
        return fromFolder;
    }

    public String getFromFilename() {
        return fromFilename;
    }
}
