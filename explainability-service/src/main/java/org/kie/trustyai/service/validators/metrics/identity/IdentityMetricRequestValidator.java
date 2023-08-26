package org.kie.trustyai.service.validators.metrics.identity;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.payloads.metrics.identity.IdentityMetricRequest;
import org.kie.trustyai.service.validators.generic.GenericValidationUtils;

@ApplicationScoped
public class IdentityMetricRequestValidator implements ConstraintValidator<ValidIdentityMetricRequest, IdentityMetricRequest> {
    @Inject
    Instance<DataSource> dataSource;

    @Override
    public void initialize(ValidIdentityMetricRequest constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(IdentityMetricRequest request, ConstraintValidatorContext context) {
        final String modelId = request.getModelId();

        if (!GenericValidationUtils.validateModelId(context, dataSource, modelId)) {
            return false;
        } else {
            final Metadata metadata = dataSource.get().getMetadata(modelId);
            final String columnName = request.getColumnName();
            // Outcome name is not present
            boolean columnValidation = GenericValidationUtils.validateColumnName(context, metadata, modelId, columnName);

            boolean batchValidation = true;
            if (Objects.nonNull(request.getBatchSize()) && request.getBatchSize() <= 0) {
                context.buildConstraintViolationWithTemplate(
                        "Request batch size must be bigger than 0.")
                        .addConstraintViolation();
                batchValidation = false;
            }
            return columnValidation && batchValidation;
        }
    }
}
