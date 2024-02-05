package org.kie.trustyai.service.mocks.kserve;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.funqy.knative.events.CloudEvent;

public abstract class MockKServePayload<T> implements CloudEvent<T> {
    private String id;
    private T data;
    private Map<String, String> extensions = new HashMap<>();

    @Override
    public String id() {
        return id;
    }

    @Override
    public String specVersion() {
        return null;
    }

    @Override
    public String source() {
        return null;
    }

    @Override
    public String type() {
        return null;
    }

    @Override
    public String subject() {
        return null;
    }

    @Override
    public OffsetDateTime time() {
        return null;
    }

    @Override
    public Map<String, String> extensions() {
        return extensions;
    }

    @Override
    public String dataSchema() {
        return null;
    }

    @Override
    public String dataContentType() {
        return null;
    }

    @Override
    public T data() {
        return data;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setData(T data) {
        this.data = data;
    }
}
