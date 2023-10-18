package org.kie.trustyai.service.data.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelMeshInferencePayloadReconcilerTest {
    @Test
    public void testModelIdStandardizeNoISVC() {
        assertEquals("modelId", ModelMeshInferencePayloadReconciler.standardizeModelId("modelId"));
    }

    @Test
    public void testModelIdStandardizeWithISVC() {
        assertEquals("modelId", ModelMeshInferencePayloadReconciler.standardizeModelId("modelId__isvc-3a07dca0ce"));
    }

    @Test
    public void testModelIdStandardizeWithMultipleISVC() {
        assertEquals("model__isvc_is_my_model_Id", ModelMeshInferencePayloadReconciler.standardizeModelId("model__isvc_is_my_model_Id__isvc-3a07dca0ce"));
    }
}