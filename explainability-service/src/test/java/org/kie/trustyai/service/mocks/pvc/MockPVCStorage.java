package org.kie.trustyai.service.mocks.pvc;

import java.io.File;

import org.kie.trustyai.service.data.storage.flatfile.PVCStorage;

import io.quarkus.test.Mock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

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
