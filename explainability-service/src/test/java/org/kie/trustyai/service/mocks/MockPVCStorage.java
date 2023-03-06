package org.kie.trustyai.service.mocks;

import java.io.File;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

import org.kie.trustyai.service.data.storage.PVCStorage;

import io.quarkus.test.Mock;

@Mock
@Alternative
@ApplicationScoped
public class MockPVCStorage extends PVCStorage {

    public MockPVCStorage() {
        super(new MockPVCServiceConfig(), new MockPVCStorageConfig());
    }

    public boolean emptyStorage(String filepath) {
        final File file = new File(filepath);
        return file.delete();
    }
}
