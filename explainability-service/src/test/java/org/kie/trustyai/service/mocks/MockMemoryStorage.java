package org.kie.trustyai.service.mocks;

import org.kie.trustyai.service.data.storage.MemoryStorage;

import io.quarkus.test.Mock;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

@Mock
@Alternative
@ApplicationScoped
@Priority(1)
public class MockMemoryStorage extends MemoryStorage {

    public MockMemoryStorage() {
        super(new MockMemoryServiceConfig(), new MockMemoryStorageConfig());
    }

    public void emptyStorage() {
        this.data.clear();
    }

}
