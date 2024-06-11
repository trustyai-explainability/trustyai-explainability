package org.kie.trustyai.service.mocks.flatfile;

import java.io.File;

import org.kie.trustyai.service.data.storage.flatfile.PVCStorage;
import org.kie.trustyai.service.mocks.MockServiceConfig;
import org.kie.trustyai.service.mocks.MockStorageConfig;

import io.quarkus.test.Mock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

@Mock
@Alternative
@ApplicationScoped
public class MockPVCStorage extends PVCStorage {

    public MockPVCStorage() {
        super(new MockServiceConfig(), new MockStorageConfig());
    }

    public boolean emptyStorage(String filepath) {
        final File file = new File(filepath);
        return file.delete();
    }
}
