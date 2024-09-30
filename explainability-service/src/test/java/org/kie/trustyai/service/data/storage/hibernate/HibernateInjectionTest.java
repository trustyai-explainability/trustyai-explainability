package org.kie.trustyai.service.data.storage.hibernate;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.endpoints.metrics.RequestPayloadGenerator;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateDatasource;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateStorage;
import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;
import org.kie.trustyai.service.payloads.values.reconcilable.ReconcilableFeature;
import org.kie.trustyai.service.payloads.values.reconcilable.ReconcilableOutput;
import org.kie.trustyai.service.profiles.hibernate.HibernateTestProfile;
import org.kie.trustyai.service.utils.DataframeGenerators;

import com.fasterxml.jackson.databind.node.IntNode;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
@TestProfile(HibernateTestProfile.class)
class HibernateInjectionTest {

    @Inject
    Instance<MockHibernateStorage> storage;

    @Inject
    Instance<MockHibernateDatasource> datasource;

    String MODEL_ID = "example_model_";
    int n_dfs = 5;

    List<Dataframe> getDFs() {
        return IntStream
                .range(0, n_dfs)
                .mapToObj(i -> DataframeGenerators.generateRandomNColumnDataframeMatchingKservePayloads(5000 + i, i + 5))
                .collect(Collectors.toList());
    }

    @BeforeEach
    void populateStorage() {
        emptyStorage();
        List<Dataframe> dfs = getDFs();
        for (int i = 0; i < n_dfs; i++) {
            datasource.get().saveDataframe(dfs.get(i), MODEL_ID + i);
        }
    }

    @AfterEach
    void emptyStorage() {
        for (int i = 0; i < n_dfs; i++) {
            storage.get().clearData(MODEL_ID + i);
        }
    }

    @Test
    void sendWildcardModel() {
        final GroupMetricRequest payload = RequestPayloadGenerator.correct();
        payload.setModelId("*");
        payload.setProtectedAttribute("input-0");
        payload.setOutcomeName("output-0-0");
        payload.setUnprivilegedAttribute(new ReconcilableFeature(new IntNode(1)));
        payload.setPrivilegedAttribute(new ReconcilableFeature(new IntNode(0)));
        payload.setFavorableOutcome(new ReconcilableOutput(new IntNode(0)));

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/metrics/group/fairness/spd/")
                .then()
                .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                .body(containsString("No metadata found for model=*"));

    }

    @Test
    void sendWildcardAttribute() {
        final GroupMetricRequest payload = RequestPayloadGenerator.correct();
        payload.setModelId(MODEL_ID + 1);
        payload.setProtectedAttribute("*");
        payload.setOutcomeName("output-0-0");
        payload.setUnprivilegedAttribute(new ReconcilableFeature(new IntNode(1)));
        payload.setPrivilegedAttribute(new ReconcilableFeature(new IntNode(0)));
        payload.setFavorableOutcome(new ReconcilableOutput(new IntNode(0)));

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/metrics/group/fairness/spd/")
                .then()
                .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                .body(containsString("No protected attribute found with name=*"));
    }

    @Test
    void sendWildcardOutput() {
        final GroupMetricRequest payload = RequestPayloadGenerator.correct();
        payload.setModelId(MODEL_ID + 1);
        payload.setProtectedAttribute("input-0");
        payload.setOutcomeName("*");
        payload.setUnprivilegedAttribute(new ReconcilableFeature(new IntNode(1)));
        payload.setPrivilegedAttribute(new ReconcilableFeature(new IntNode(0)));
        payload.setFavorableOutcome(new ReconcilableOutput(new IntNode(0)));

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/metrics/group/fairness/spd/")
                .then()
                .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                .body(containsString("No output found with name=*"));
    }

}
