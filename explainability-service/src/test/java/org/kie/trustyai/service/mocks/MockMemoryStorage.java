package org.kie.trustyai.service.mocks;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

import org.kie.trustyai.service.data.storage.MemoryStorage;

import io.quarkus.test.Mock;

@Mock
@Alternative
@ApplicationScoped
public class MockMemoryStorage extends MemoryStorage {

    public MockMemoryStorage() {
        super(new MockMemoryStorageConfig());

    }

    public void emptyStorage() {
        this.data.clear();
    }

}
