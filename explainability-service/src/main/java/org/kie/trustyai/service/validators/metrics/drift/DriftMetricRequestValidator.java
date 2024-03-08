package org.kie.trustyai.service.validators.metrics.drift;

import java.util.Objects;
import java.util.Optional;

import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.payloads.metrics.drift.DriftMetricRequest;
import org.kie.trustyai.service.validators.generic.GenericValidationUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

@ApplicationScoped
public class DriftMetricRequestValidator implements ConstraintValidator<ValidDriftMetricRequest, DriftMetricRequest> {
    @Inject
    Instance<DataSource> dataSource;

    @Override
    public void initialize(ValidDriftMetricRequest constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(DriftMetricRequest request, ConstraintValidatorContext context) {
        final String modelId = request.getModelId();

        if (!GenericValidationUtils.validateModelId(context, dataSource, modelId)) {
            return false;
        } else {
            final StorageMetadata storageMetadata = dataSource.get().getMetadata(modelId);

            // check that the fitting is valid, if provided
            boolean columnValidation = true;
            if (request.getFitColumns() != null) {
                for (String columnName : request.getFitColumns()) {
                    columnValidation = GenericValidationUtils.validateColumnName(context, storageMetadata, modelId, columnName) && columnValidation;
                }
            }
            boolean batchValidation = true;
            if (Objects.nonNull(request.getBatchSize()) && request.getBatchSize() <= 0) {
                context.buildConstraintViolationWithTemplate(
                        "Request batch size must be bigger than 0.")
                        .addConstraintViolation();
                batchValidation = false;
            }

            boolean tagValidation = true;
            if (Objects.nonNull(request.getReferenceTag())) {
                Optional<String> tagValidationErrorMessage = GenericValidationUtils.validateDataTag(request.getReferenceTag());
                if (tagValidationErrorMessage.isPresent()) {
                    context.buildConstraintViolationWithTemplate(tagValidationErrorMessage.get())
                            .addConstraintViolation();
                    tagValidation = false;
                }
            } else {
                context.buildConstraintViolationWithTemplate(
                        "Must provide a reference tag in request defining the original data distribution. This is done by passing a field \"referenceTag\": \"tagString\" in the request body.")
                        .addConstraintViolation();
                tagValidation = false;
            }

            return columnValidation && batchValidation && tagValidation;
        }
    }
}
