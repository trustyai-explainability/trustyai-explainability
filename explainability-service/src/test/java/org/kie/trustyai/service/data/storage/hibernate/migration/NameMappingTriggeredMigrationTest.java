package org.kie.trustyai.service.data.storage.hibernate.migration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateDatasource;
import org.kie.trustyai.service.payloads.service.NameMapping;
import org.kie.trustyai.service.payloads.service.ServiceMetadata;
import org.kie.trustyai.service.profiles.hibernate.migration.silos.MigrationTestProfileNameMappingTriggered;
import org.kie.trustyai.service.utils.DataframeGenerators;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestProfile(MigrationTestProfileNameMappingTriggered.class)
class NameMappingTriggeredMigrationTest extends BaseMigrationTest {
    Map<String, String> nameMapping = Map.ofEntries(
            entry("f0", "Mapped0"),
            entry("f1", "Mapped1"),
            entry("f2", "Mapped2"));

    void triggerMigration() {
        HashMap<String, String> inputMapping = new HashMap<>();
        HashMap<String, String> outputMapping = new HashMap<>();
        inputMapping.putAll(nameMapping);
        NameMapping nameMapping = new NameMapping(MODEL_NAME + "0", inputMapping, outputMapping);

        given()
                .contentType(ContentType.JSON)
                .body(nameMapping)
                .when().post("/info/names").peek()
                .then()
                .statusCode(200)
                .body(is("Feature and output name mapping successfully applied."));
    }

    @Test
    void testMappingRetrieval() {
        final List<ServiceMetadata> serviceMetadata = given()
                .when().get("/info")
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body().as(new TypeRef<List<ServiceMetadata>>() {
                });

        for (String value : serviceMetadata.get(0).getData().getInputSchema().getNameMapping().values()) {
            assertTrue(value.contains("Mapped"));
        }
    }

    Dataframe applyNameMapping(Dataframe dataframe) {
        Dataframe result = dataframe.copy();
        result.setColumnAliases(nameMapping);
        return result;
    }

    @Override
    void validateRetrieval(MockHibernateDatasource datasource) {
        for (int i = 0; i < N_DFS; i++) {
            Dataframe original = dfs.get(i);

            if (i == 0) {
                original = applyNameMapping(original);
            }
            Dataframe retrieved = datasource.getDataframe(MODEL_NAME + i, 1000);
            DataframeGenerators.roughEqualityCheck(original, retrieved);
        }
    }

    @Override
    @Test
    void retrieveAndSaveOnMigratedDF() {
        triggerMigration();

        validateRetrieval(datasource.get());

        for (int i = 0; i < N_DFS; i++) {
            int dfLen = 100 + i;
            Dataframe newDF = DataframeGenerators.generatePositionalHintedDataframe(dfLen, i + 5);
            Dataframe original = dfs.get(i);
            datasource.get().saveDataframe(newDF, MODEL_NAME + i);

            if (i == 0) {
                newDF = applyNameMapping(newDF);
                original = applyNameMapping(original);
            }

            // retrieve migrated DF
            Dataframe retrievedFirst = datasource.get().getDataframe(MODEL_NAME + i, 0, dfLen);

            // retrieve df saved after migrated df
            Dataframe retrievedSecond = datasource.get().getDataframe(MODEL_NAME + i, dfLen, dfLen + dfLen);
            DataframeGenerators.roughEqualityCheck(original, retrievedFirst);
            DataframeGenerators.roughEqualityCheck(newDF, retrievedSecond);
        }
    }
}
