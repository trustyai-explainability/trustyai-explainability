package org.kie.trustyai.service.mocks.memory;

import org.kie.trustyai.service.data.storage.flatfile.MemoryStorage;

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
        super(new MockMemoryServiceConfig(), new MockMemoryStorageConfig());
    }

    public void emptyStorage() {
        this.data.clear();
    }

}
