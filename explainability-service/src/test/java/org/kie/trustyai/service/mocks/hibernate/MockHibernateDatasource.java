package org.kie.trustyai.service.mocks.hibernate;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.test.Mock;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.data.utils.MetadataUtils;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateStorage;
import org.kie.trustyai.service.mocks.memory.MockMemoryStorage;

import static org.kie.trustyai.service.utils.DataframeGenerators.generateRandomDataframe;

@Mock
@Alternative
@ApplicationScoped
@Priority(1)
public class MockHibernateDatasource extends DataSource {
    @Inject
    Instance<MockHibernateStorage> storage;

    public MockHibernateDatasource() {
    }

}
