package org.kie.trustyai.service.data.storage.hibernate.migration;

public class MigrationEvent {
    public class MigrationTriggerEvent {
    }

    public class MigrationFinishEvent {
    }

    public class MigrationSingleDataframeFinishEvent {
        String migratedModel;

        public MigrationSingleDataframeFinishEvent(String migratedModel) {
            this.migratedModel = migratedModel;
        }

        public String getMigratedModel() {
            return migratedModel;
        }
    }

    public static MigrationTriggerEvent getMigrationTriggerEvent() {
        return new MigrationEvent().new MigrationTriggerEvent();
    }

    public static MigrationFinishEvent getMigrationFinishEvent() {
        return new MigrationEvent().new MigrationFinishEvent();
    }

    public static MigrationSingleDataframeFinishEvent getMigrationSingleDataframeFinishEvent(String migratedModel) {
        return new MigrationEvent().new MigrationSingleDataframeFinishEvent(migratedModel);
    }
}
