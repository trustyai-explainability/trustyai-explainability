package org.kie.trustyai.service.validators.metrics.fairness.group;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.jboss.logging.Logger;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.payloads.data.download.DataRequestPayload;
import org.kie.trustyai.service.payloads.data.download.ModelDataRequestPayload;
import org.kie.trustyai.service.payloads.metrics.fairness.group.AdvancedGroupMetricRequest;
import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;
import org.kie.trustyai.service.validators.data.DataDownloadRequestValidator;
import org.kie.trustyai.service.validators.data.ValidDataDownloadRequest;
import org.kie.trustyai.service.validators.generic.GenericValidationUtils;
import org.kie.trustyai.service.validators.metrics.ValidReconciledMetricRequest;

import java.util.Objects;
import java.util.Set;

@ApplicationScoped
public class AdvancedGroupMetricRequestValidator implements ConstraintValidator<ValidAdvancedGroupMetricRequest, AdvancedGroupMetricRequest> {
    private static final Logger LOG = Logger.getLogger(AdvancedGroupMetricRequestValidator.class);

    @Inject
    Instance<DataSource> dataSource;

    @Override
    public void initialize(ValidAdvancedGroupMetricRequest constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    public boolean validateDataRequest(String modelId, DataRequestPayload drp, ConstraintValidatorContext context){
        ModelDataRequestPayload mdrp = new ModelDataRequestPayload();
        mdrp.setMatchAll(drp.getMatchAll());
        mdrp.setMatchAny(drp.getMatchAny());
        mdrp.setMatchNone(drp.getMatchNone());
        mdrp.setModelId(modelId);
        return DataDownloadRequestValidator.manualValidation(mdrp, context, dataSource);
    }


    @Override
    public boolean isValid(AdvancedGroupMetricRequest request, ConstraintValidatorContext context) {
        final String modelId = request.getModelId();
        boolean result = true;

        if (!GenericValidationUtils.validateModelId(context, dataSource, modelId)) {
            result = false;
        } else {
            result = validateDataRequest(request.getModelId(), request.getFavorableOutcome(), context) && result;
            result = validateDataRequest(request.getModelId(), request.getPrivilegedAttribute(), context) && result;
            result = validateDataRequest(request.getModelId(), request.getUnprivilegedAttribute(), context) && result;
        }

        return result;
    }
}
