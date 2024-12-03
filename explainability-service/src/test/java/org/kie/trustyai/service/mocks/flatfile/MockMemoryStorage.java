package org.kie.trustyai.service.mocks.flatfile;

import org.kie.trustyai.service.data.storage.flatfile.MemoryStorage;
import org.kie.trustyai.service.mocks.MockServiceConfig;
import org.kie.trustyai.service.mocks.MockStorageConfig;

import io.quarkus.test.Mock;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

@Mock
@Alternative
@ApplicationScoped
@Priority(2)
public class MockMemoryStorage extends MemoryStorage {

    public MockMemoryStorage() {
        super(new MockServiceConfig(), new MockStorageConfig());
    }

    public void emptyStorage() {
        this.data.clear();
    }

}
