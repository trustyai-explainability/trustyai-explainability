package org.kie.trustyai.service.validators.serviceRequests;

import java.util.Set;

import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.payloads.service.NameMapping;
import org.kie.trustyai.service.validators.generic.GenericValidationUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

@ApplicationScoped
public class NameMappingRequestValidator implements ConstraintValidator<ValidNameMappingRequest, NameMapping> {
    @Inject
    Instance<DataSource> dataSource;

    @Override
    public void initialize(ValidNameMappingRequest constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(NameMapping request, ConstraintValidatorContext context) {
        final String modelId = request.getModelId();

        if (!GenericValidationUtils.validateModelId(context, dataSource, modelId)) {
            return false;
        } else {
            final StorageMetadata storageMetadata = dataSource.get().getMetadata(modelId);
            final Set<String> inputNames = request.getInputMapping().keySet();
            final Set<String> outputNames = request.getOutputMapping().keySet();

            // Outcome name is not present
            boolean columnValidation = true;
            for (String inputName : inputNames) {
                columnValidation = GenericValidationUtils.validateFeatureColumnName(context, storageMetadata, modelId, inputName) && columnValidation;
            }

            for (String outputName : outputNames) {
                columnValidation = GenericValidationUtils.validateOutputColumnName(context, storageMetadata, modelId, outputName) && columnValidation;
            }

            return columnValidation;
        }
    }
}
