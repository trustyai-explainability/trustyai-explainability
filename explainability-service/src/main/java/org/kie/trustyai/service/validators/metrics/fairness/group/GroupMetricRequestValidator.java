package org.kie.trustyai.service.validators.metrics.fairness.group;

import java.util.Objects;

import org.jboss.logging.Logger;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;
import org.kie.trustyai.service.validators.generic.GenericValidationUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

@ApplicationScoped
public class GroupMetricRequestValidator implements ConstraintValidator<ValidGroupMetricRequest, GroupMetricRequest> {
    private static final Logger LOG = Logger.getLogger(GroupMetricRequestValidator.class);

    @Inject
    Instance<DataSource> dataSource;

    @Override
    public void initialize(ValidGroupMetricRequest constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(GroupMetricRequest request, ConstraintValidatorContext context) {
        final String modelId = request.getModelId();
        boolean result;

        if (!GenericValidationUtils.validateModelId(context, dataSource, modelId)) {
            result = false;
        } else {
            final StorageMetadata storageMetadata = dataSource.get().getMetadata(modelId);
            final String outcomeName = request.getOutcomeName();
            final String protectedAttribute = request.getProtectedAttribute();

            // Outcome name or attribute name not present
            boolean validOutputName = GenericValidationUtils.validateOutputColumnName(context, storageMetadata, modelId, outcomeName);
            boolean validAttributeName = GenericValidationUtils.validateFeatureColumnName(context, storageMetadata, modelId, protectedAttribute, "protected attribute");

            // set result to failure if either above are false
            result = validOutputName && validAttributeName;

            if (validOutputName) {
                result = GenericValidationUtils.validateOutputColumnType(context, storageMetadata, modelId, outcomeName, request.getFavorableOutcome().getRawValueNodes()) && result;
            }

            if (validAttributeName) {
                result = GenericValidationUtils.validateFeatureColumnType(context, storageMetadata, modelId, protectedAttribute, request.getPrivilegedAttribute().getRawValueNodes(),
                        "privileged attribute")
                        && result;
                result = GenericValidationUtils.validateFeatureColumnType(context, storageMetadata, modelId, protectedAttribute, request.getUnprivilegedAttribute().getRawValueNodes(),
                        "unprivileged attribute") && result;
            }
            if (Objects.nonNull(request.getBatchSize()) && request.getBatchSize() <= 0) {
                context.buildConstraintViolationWithTemplate("Request batch size must be bigger than 0.").addConstraintViolation();
                result = false;
            }
        }
        return result;
    }
}
