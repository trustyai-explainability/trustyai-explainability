package org.kie.trustyai.service.prometheus;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.service.data.datasources.DataSource;
import org.kie.trustyai.service.endpoints.metrics.RequestPayloadGenerator;
import org.kie.trustyai.service.payloads.BaseScheduledResponse;
import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;
import org.kie.trustyai.service.payloads.scheduler.ScheduleId;

import io.restassured.http.ContentType;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class BasePrometheusTest {
    protected static final String MODEL_ID = "example1";
    protected static final int N_SAMPLES = 100;

    @Inject
    Instance<DataSource> datasource;

    /**
     * Deleting a request should remove it from the Prometheus /q/metrics endpoint
     */
    @Test
    void deleteRequestIsRemovedFromMetrics() throws InterruptedException {
        final GroupMetricRequest payload = RequestPayloadGenerator.correct();
        final BaseScheduledResponse response = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/request")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(BaseScheduledResponse.class);

        final ScheduleId requestId = new ScheduleId();
        requestId.requestId = response.getRequestId();

        for (int i = 0; i < 6; i++) {
            Thread.sleep(1000);
        }

        // grab metrics endpoint before deletion
        String metricsListBeforeDelete = given()
                .when()
                .basePath("/q/metrics")
                .get().then().statusCode(Response.Status.OK.getStatusCode())
                .extract().body()
                .asString();

        // delete metric request
        given()
                .contentType(ContentType.JSON)
                .body(requestId)
                .when().delete("/request")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());

        // grab metrics endpoint after deletion
        String metricsListAfterDelete = given()
                .when()
                .basePath("/q/metrics")
                .get().then().statusCode(Response.Status.OK.getStatusCode())
                .extract().body()
                .asString();

        assertTrue(metricsListBeforeDelete
                .contains("trustyai_spd{batch_size=\"5000\",favorable_value=\"1\",metricName=\"SPD\",model=\"example1\",outcome=\"income\",privileged=\"1\",protected=\"gender\""));
        assertFalse(metricsListAfterDelete
                .contains("trustyai_spd{batch_size=\"5000\",favorable_value=\"1\",metricName=\"SPD\",model=\"example1\",outcome=\"income\",privileged=\"1\",protected=\"gender\""));
    }
}
