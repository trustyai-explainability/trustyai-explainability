package org.kie.trustyai.service.data.storage.hibernate.migration;

import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;
import org.kie.trustyai.service.payloads.values.reconcilable.ReconcilableFeature;
import org.kie.trustyai.service.payloads.values.reconcilable.ReconcilableOutput;
import org.kie.trustyai.service.profiles.hibernate.migration.silos.MigrationTestProfileMetricTriggered;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import jakarta.ws.rs.core.Response;

import static io.restassured.RestAssured.given;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestProfile(MigrationTestProfileMetricTriggered.class)
class MetricTriggeredMigrationTest extends BaseMigrationTest {
    @Override
    void triggerMigration() {
        final GroupMetricRequest payload = new GroupMetricRequest();
        payload.setOutcomeName("o0");
        payload.setFavorableOutcome(new ReconcilableOutput(new IntNode(0)));

        payload.setProtectedAttribute("f0");
        payload.setPrivilegedAttribute(new ReconcilableFeature(new TextNode("0,0")));
        payload.setUnprivilegedAttribute(new ReconcilableFeature(new TextNode("1,0")));
        payload.setModelId("EXAMPLE_MODEL_5");

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/metrics/group/fairness/spd/").peek()
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }
}
