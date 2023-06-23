package org.kie.trustyai.service.endpoints.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.BaseTestProfile;
import org.kie.trustyai.service.data.utils.MetadataUtils;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.payloads.service.NameMapping;
import org.kie.trustyai.service.payloads.service.ServiceMetadata;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(BaseTestProfile.class)
@TestHTTPEndpoint(ServiceMetadataEndpoint.class)
class ServiceMetadataEndpointTest {

    private static final String MODEL_ID = "example1";
    @Inject
    Instance<MockDatasource> datasource;
    @Inject
    Instance<MockMemoryStorage> storage;

    @BeforeEach
    void clearStorage() {
        storage.get().emptyStorage();
    }

    @Test
    void getTwoObservations() throws JsonProcessingException {
        final Dataframe dataframe = datasource.get().generateRandomDataframe(2);
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);

        final List<ServiceMetadata> serviceMetadata = given()
                .when().get()
                .then()
                .statusCode(200)
                .extract()
                .body().as(new TypeRef<List<ServiceMetadata>>() {
                });

        assertEquals(1, serviceMetadata.size());
        assertEquals(0, serviceMetadata.get(0).getMetrics().scheduledMetadata.dir);
        assertEquals(0, serviceMetadata.get(0).getMetrics().scheduledMetadata.spd);
        assertEquals(2, serviceMetadata.get(0).getData().getObservations());
        assertEquals(2, serviceMetadata.get(0).getData().getInputSchema().getItems().get("age").getValues().size());
        assertFalse(serviceMetadata.get(0).getData().getOutputSchema().getItems().isEmpty());
        assertFalse(serviceMetadata.get(0).getData().getInputSchema().getItems().isEmpty());
        assertEquals(dataframe.getInputNames()
                .stream()
                .filter(name -> !name.equals(MetadataUtils.ID_FIELD))
                .filter(name -> !name.equals(MetadataUtils.TIMESTAMP_FIELD)).collect(Collectors.toSet()),
                serviceMetadata.get(0).getData().getInputSchema().getItems().keySet());
        assertEquals(
                new HashSet<>(dataframe.getOutputNames()),
                serviceMetadata.get(0).getData().getOutputSchema().getItems().keySet());
    }

    @Test
    void getThousandObservations() throws JsonProcessingException {
        final Dataframe dataframe = datasource.get().generateRandomDataframe(1000, 50);
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);

        final List<ServiceMetadata> serviceMetadata = given()
                .when().get()
                .then()
                .statusCode(200)
                .extract()
                .body().as(new TypeRef<List<ServiceMetadata>>() {
                });

        assertEquals(1, serviceMetadata.size());
        assertEquals(0, serviceMetadata.get(0).getMetrics().scheduledMetadata.dir);
        assertEquals(0, serviceMetadata.get(0).getMetrics().scheduledMetadata.spd);
        assertEquals(1000, serviceMetadata.get(0).getData().getObservations());

        // check column values
        assertEquals(50, serviceMetadata.get(0).getData().getInputSchema().getItems().get("age").getValues().size());
        assertEquals(2, serviceMetadata.get(0).getData().getInputSchema().getItems().get("race").getValues().size());
        assertFalse(serviceMetadata.get(0).getData().getOutputSchema().getItems().isEmpty());
        assertFalse(serviceMetadata.get(0).getData().getInputSchema().getItems().isEmpty());
    }

    @Test
    void getThousandDiverseObservations() throws JsonProcessingException {
        final Dataframe dataframe = datasource.get().generateRandomDataframe(1000, 1000);
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);

        final List<ServiceMetadata> serviceMetadata = given()
                .when().get()
                .then()
                .statusCode(200)
                .extract()
                .body().as(new TypeRef<List<ServiceMetadata>>() {
                });

        assertEquals(1, serviceMetadata.size());
        assertEquals(0, serviceMetadata.get(0).getMetrics().scheduledMetadata.dir);
        assertEquals(0, serviceMetadata.get(0).getMetrics().scheduledMetadata.spd);
        assertEquals(1000, serviceMetadata.get(0).getData().getObservations());

        // check column values
        assertEquals(null, serviceMetadata.get(0).getData().getInputSchema().getItems().get("age").getValues());
        assertEquals(2, serviceMetadata.get(0).getData().getInputSchema().getItems().get("race").getValues().size());
        assertFalse(serviceMetadata.get(0).getData().getOutputSchema().getItems().isEmpty());
        assertFalse(serviceMetadata.get(0).getData().getInputSchema().getItems().isEmpty());
    }

    @Test
    void getNoObservations() {
        datasource.get().empty();
        final List<ServiceMetadata> serviceMetadata = given()
                .when().get()
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body().as(new TypeRef<List<ServiceMetadata>>() {
                });

        assertEquals(0, serviceMetadata.size());
    }

    @Test
    void setNameMapping() throws JsonProcessingException {
        final Dataframe dataframe = datasource.get().generateRandomDataframe(1000, 10);
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);

        HashMap<String, String> inputMapping = new HashMap<>();
        HashMap<String, String> outputMapping = new HashMap<>();
        inputMapping.put("age", "Age Mapped");
        inputMapping.put("gender", "Gender Mapped");
        inputMapping.put("race", "Race Mapped");

        outputMapping.put("income", "Income Mapped");
        NameMapping nameMapping = new NameMapping(MODEL_ID, inputMapping, outputMapping);

        given()
                .contentType(ContentType.JSON)
                .body(nameMapping)
                .when().post()
                .then()
                .statusCode(200)
                .body(is("Feature and output name mapping successfully applied."));

        final List<ServiceMetadata> serviceMetadata = given()
                .when().get()
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body().as(new TypeRef<List<ServiceMetadata>>() {
                });

        for (String value : serviceMetadata.get(0).getData().getInputSchema().getNameMapping().values()) {
            assertTrue(value.contains("Mapped"));
        }

        for (String value : serviceMetadata.get(0).getData().getOutputSchema().getNameMapping().values()) {
            assertTrue(value.contains("Mapped"));
        }
    }

    @Test
    void setNameMappingPartial() throws JsonProcessingException {
        final Dataframe dataframe = datasource.get().generateRandomDataframe(1000, 10);
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);

        HashMap<String, String> inputMapping = new HashMap<>();
        HashMap<String, String> outputMapping = new HashMap<>();
        inputMapping.put("age", "Age Mapped");
        inputMapping.put("gender", "Gender Mapped");

        NameMapping nameMapping = new NameMapping(MODEL_ID, inputMapping, outputMapping);

        given()
                .contentType(ContentType.JSON)
                .body(nameMapping)
                .when().post()
                .then()
                .statusCode(200)
                .body(is("Feature and output name mapping successfully applied."));

        final List<ServiceMetadata> serviceMetadata = given()
                .when().get()
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body().as(new TypeRef<List<ServiceMetadata>>() {
                });

        for (String value : serviceMetadata.get(0).getData().getInputSchema().getNameMapping().values()) {
            assertTrue(value.contains("Mapped"));
        }
        for (String value : serviceMetadata.get(0).getData().getInputSchema().getNameMapping().keySet()) {
            assertFalse(value.contains("race"));
        }

        assertEquals(0, serviceMetadata.get(0).getData().getOutputSchema().getNameMapping().size());
    }

    @Test
    void setNameMappingWrongInputs() throws JsonProcessingException {
        final Dataframe dataframe = datasource.get().generateRandomDataframe(1000, 10);
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);

        HashMap<String, String> inputMapping = new HashMap<>();
        HashMap<String, String> outputMapping = new HashMap<>();
        inputMapping.put("age123", "Age Mapped");
        NameMapping nameMapping = new NameMapping(MODEL_ID, inputMapping, outputMapping);

        given()
                .contentType(ContentType.JSON)
                .body(nameMapping)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                .body(Matchers.containsString("Not all mapped input fields exist in model metadata"));
    }

    @Test
    void setNameMappingWrongOutputs() throws JsonProcessingException {
        final Dataframe dataframe = datasource.get().generateRandomDataframe(1000, 10);
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);

        HashMap<String, String> inputMapping = new HashMap<>();
        HashMap<String, String> outputMapping = new HashMap<>();
        outputMapping.put("age123", "Age Mapped");
        NameMapping nameMapping = new NameMapping(MODEL_ID, inputMapping, outputMapping);

        given()
                .contentType(ContentType.JSON)
                .body(nameMapping)
                .when().post()
                .then()
                .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                .body(Matchers.containsString("Not all mapped output fields exist in model metadata"));
    }

}
