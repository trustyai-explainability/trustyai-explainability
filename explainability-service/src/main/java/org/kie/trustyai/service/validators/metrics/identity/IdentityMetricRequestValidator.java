package org.kie.trustyai.service.validators.metrics.identity;

import java.util.Objects;

import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.payloads.metrics.identity.IdentityMetricRequest;
import org.kie.trustyai.service.validators.generic.GenericValidationUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

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
            final StorageMetadata storageMetadata = dataSource.get().getMetadata(modelId);
            final String columnName = request.getColumnName();
            // Outcome name is not present
            boolean columnValidation = GenericValidationUtils.validateColumnName(context, storageMetadata, modelId, columnName);

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
