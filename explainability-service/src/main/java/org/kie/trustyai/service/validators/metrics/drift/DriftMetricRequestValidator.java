package org.kie.trustyai.service.validators.metrics.drift;

import java.util.Arrays;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.kie.trustyai.explainability.model.DatapointSource;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.payloads.metrics.drift.DriftMetricRequest;
import org.kie.trustyai.service.validators.generic.GenericValidationUtils;

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
            final Metadata metadata = dataSource.get().getMetadata(modelId);

            // check that the fitting is valid, if provided
            boolean columnValidation = true;
            if (request.getFitting() != null) {
                for (String columnName : request.getFitting().keySet()) {
                    columnValidation = GenericValidationUtils.validateColumnName(context, metadata, modelId, columnName) && columnValidation;
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
                try {
                    DatapointSource.valueOf(request.getReferenceTag());
                } catch (IllegalArgumentException e) {
                    context.buildConstraintViolationWithTemplate(
                            String.format("%s not a valid data tag, must be one of %s",
                                    request.getReferenceTag(),
                                    Arrays.toString(DatapointSource.values())))
                            .addConstraintViolation();
                    tagValidation = false;
                }
            } else {
                context.buildConstraintViolationWithTemplate(
                        "Must provide a reference tag defining the original data distribution, one of: " + Arrays.toString(DatapointSource.values())).addConstraintViolation();
                tagValidation = false;
            }

            return columnValidation && batchValidation && tagValidation;
        }
    }
}
