package org.kie.trustyai.service.data.storage.hibernate.migration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.endpoints.metrics.RequestPayloadGenerator;
import org.kie.trustyai.service.payloads.BaseScheduledResponse;
import org.kie.trustyai.service.payloads.data.upload.ModelInferJointPayload;
import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;
import org.kie.trustyai.service.payloads.service.NameMapping;
import org.kie.trustyai.service.payloads.service.ServiceMetadata;
import org.kie.trustyai.service.payloads.values.reconcilable.ReconcilableFeature;
import org.kie.trustyai.service.payloads.values.reconcilable.ReconcilableOutput;
import org.kie.trustyai.service.profiles.hibernate.migration.scenarios.MigrationTestProfileComplex;
import org.kie.trustyai.service.utils.DataframeGenerators;
import org.kie.trustyai.service.utils.KserveRestPayloads;

import com.fasterxml.jackson.databind.node.IntNode;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;

import jakarta.ws.rs.core.Response;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestProfile(MigrationTestProfileComplex.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ComplexMigrationTest extends BaseMigrationTest {

    private static final Logger LOG = Logger.getLogger(ComplexMigrationTest.class);

    static String MODEL_NAME = "EXAMPLE_MODEL_";

    @Override
    List<Dataframe> getDFs() {
        return IntStream
                .range(0, 2)
                .mapToObj(i -> DataframeGenerators.generateRandomNColumnDataframeMatchingKservePayloads(5000 + i, i + 5))
                .collect(Collectors.toList());
    }

    @Test
    @Order(1)
    @Override
    void retrieveAndSaveOnMigratedDF() throws InterruptedException {
        validateRetrieval(datasource.get());

        for (int i = 0; i < n_dfs; i++) {
            Dataframe original = dfs.get(i);
            int dfLen = original.getRowDimension();
            int toAdd = 100;
            Dataframe newDF = DataframeGenerators.generateRandomNColumnDataframeMatchingKservePayloads(toAdd, i + 5);
            datasource.get().saveDataframe(newDF, MODEL_NAME + i);

            // retrieve migrated DF
            Dataframe retrievedFirst = datasource.get().getDataframe(MODEL_NAME + i, 0, dfLen);

            // retrieve df saved after migrated df
            Dataframe retrievedSecond = datasource.get().getDataframe(MODEL_NAME + i, dfLen, dfLen + toAdd);
            DataframeGenerators.roughEqualityCheck(original, retrievedFirst);
            DataframeGenerators.roughEqualityCheck(newDF, retrievedSecond);
        }
    }

    @Test
    @Order(2)
    void sendBiasRequestsTest() {
        // set up bias requests
        for (int i = 0; i < n_dfs; i++) {
            final GroupMetricRequest payload = RequestPayloadGenerator.correct();
            payload.setModelId(MODEL_NAME + i);
            payload.setProtectedAttribute("input-0");
            payload.setOutcomeName("output-0-0");
            payload.setUnprivilegedAttribute(new ReconcilableFeature(new IntNode(1)));
            payload.setPrivilegedAttribute(new ReconcilableFeature(new IntNode(0)));
            payload.setFavorableOutcome(new ReconcilableOutput(new IntNode(0)));

            final BaseScheduledResponse response = given()
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .when().post("/metrics/group/fairness/spd/request")
                    .then()
                    .statusCode(Response.Status.OK.getStatusCode())
                    .extract()
                    .body().as(BaseScheduledResponse.class);
        }
    }

    @Test
    @Order(3)
    void getMetadataTest() {
        // get metadata
        final Map<String, ServiceMetadata> serviceMetadata = given()
                .when().get("/info")
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body().as(new TypeRef<Map<String, ServiceMetadata>>() {
                });
    }

    @Test
    @Order(4)
    void uploadDataTest() {
        // upload data
        for (int idx = 0; idx < 10; idx++) {
            for (int i = 0; i < n_dfs; i++) {
                ModelInferJointPayload payload = KserveRestPayloads.generatePayload(5, i + 5, 1, "INT32", null);
                payload.setModelName(MODEL_NAME + i);
                given()
                        .contentType(ContentType.JSON)
                        .body(payload)
                        .when().post("/data/upload")
                        .then()
                        .statusCode(RestResponse.StatusCode.OK)
                        .body(containsString("5 datapoints"));
            }
        }
    }

    @Test
    @Order(5)
    void applyNameMappingTest() {
        for (int i = 0; i < n_dfs; i++) {
            // name mapping
            Map<String, String> inputMapping = new HashMap<>();
            inputMapping.put("input-0", "newInput-0");
            inputMapping.put("input-1", "newInput-1");
            inputMapping.put("input-2", "newInput-2");
            inputMapping.put("input-3", "newInput-3");
            inputMapping.put("input-4", "newInput-4");

            Map<String, String> outputMapping = new HashMap<>();
            outputMapping.put("output-0-0", "newOutput-0-0");

            NameMapping nm = new NameMapping(MODEL_NAME + i, inputMapping, outputMapping);

            given()
                    .contentType(ContentType.JSON)
                    .body(nm)
                    .when().post("/info/names").peek()
                    .then()
                    .statusCode(RestResponse.StatusCode.OK);
        }
    }

    @Test
    @Order(6)
    void uploadMoreDataTest() {
        uploadDataTest();
    }

}
