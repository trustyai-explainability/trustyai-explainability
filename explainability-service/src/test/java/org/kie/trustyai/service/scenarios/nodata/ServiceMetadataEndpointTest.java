package org.kie.trustyai.service.scenarios.nodata;

import java.util.List;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.service.BaseTestProfile;
import org.kie.trustyai.service.endpoints.service.ServiceMetadataEndpoint;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.payloads.service.ServiceMetadata;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.common.mapper.TypeRef;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(BaseTestProfile.class)
@TestHTTPEndpoint(ServiceMetadataEndpoint.class)
class ServiceMetadataEndpointTest {

    @Inject
    Instance<MockMemoryStorage> storage;

    @Inject
    Instance<MockDatasource> datasource;

    @BeforeEach
    void emptyStorage() {
        storage.get().emptyStorage();
    }

    @Test
    void get() {
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

}