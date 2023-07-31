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
        if (!dataSource.get().hasMetadata(modelId)) {
            context.buildConstraintViolationWithTemplate("No metadata found for model=" + modelId).addConstraintViolation();
            return false;
        } else {
            final Metadata metadata = dataSource.get().getMetadata(modelId);
            final String columnName = request.getColumnName();
            // Outcome name is not present
            if (!metadata.getOutputSchema().getNameMappedItems().containsKey(columnName) && !metadata.getInputSchema().getNameMappedItems().containsKey(columnName)) {
                context.buildConstraintViolationWithTemplate("No feature or output found with name=" + columnName).addConstraintViolation();
                return false;
            }
            if (Objects.nonNull(request.getBatchSize()) && request.getBatchSize() <= 0) {
                context.buildConstraintViolationWithTemplate(
                        "Request batch size must be bigger than 0.")
                        .addConstraintViolation();
                return false;
            }
        }
        return true;
    }
}
