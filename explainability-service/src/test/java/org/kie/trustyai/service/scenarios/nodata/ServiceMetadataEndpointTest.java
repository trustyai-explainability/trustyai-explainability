package org.kie.trustyai.service.scenarios.nodata;

import java.util.Map;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.service.endpoints.service.ServiceMetadataEndpoint;
import org.kie.trustyai.service.mocks.flatfile.MockCSVDatasource;
import org.kie.trustyai.service.mocks.flatfile.MockMemoryStorage;
import org.kie.trustyai.service.payloads.service.ServiceMetadata;
import org.kie.trustyai.service.profiles.flatfile.MemoryTestProfile;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.common.mapper.TypeRef;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(MemoryTestProfile.class)
@TestHTTPEndpoint(ServiceMetadataEndpoint.class)
class ServiceMetadataEndpointTest {

    @Inject
    Instance<MockMemoryStorage> storage;

    @Inject
    Instance<MockCSVDatasource> datasource;

    @BeforeEach
    void emptyStorage() {
        storage.get().emptyStorage();
    }

    @Test
    void get() throws JsonProcessingException {
        datasource.get().reset();
        final Map<String, ServiceMetadata> serviceMetadata = given()
                .when().get()
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body().as(new TypeRef<Map<String, ServiceMetadata>>() {
                });

        assertEquals(0, serviceMetadata.size());
    }

}
