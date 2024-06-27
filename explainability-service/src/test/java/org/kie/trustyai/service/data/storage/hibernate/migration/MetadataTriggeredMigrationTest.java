package org.kie.trustyai.service.data.storage.hibernate.migration;

import java.util.List;

import org.jboss.resteasy.reactive.RestResponse;
import org.kie.trustyai.service.payloads.service.ServiceMetadata;
import org.kie.trustyai.service.profiles.hibernate.migration.silos.MigrationTestProfileMetadataTriggered;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.common.mapper.TypeRef;

import static io.restassured.RestAssured.given;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestProfile(MigrationTestProfileMetadataTriggered.class)
class MetadataTriggeredMigrationTest extends BaseMigrationTest {
    @Override
    void triggerMigration() {
        final List<ServiceMetadata> serviceMetadata = given()
                .when().get("/info")
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body().as(new TypeRef<List<ServiceMetadata>>() {
                });
    }
}
