package org.kie.trustyai.service.validators;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.payloads.BaseMetricRequest;
import org.kie.trustyai.service.payloads.PayloadConverter;
import org.kie.trustyai.service.payloads.service.SchemaItem;

@ApplicationScoped
public class BaseMetricRequestValidator implements ConstraintValidator<ValidBaseMetricRequest, BaseMetricRequest> {
    @Inject
    Instance<DataSource> dataSource;

    @Override
    public void initialize(ValidBaseMetricRequest constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(BaseMetricRequest request, ConstraintValidatorContext context) {
        final String modelId = request.getModelId();
        boolean result = true;
        boolean validOutput = false;
        boolean validAttribute = false;

        if (!dataSource.get().hasMetadata(modelId)) {
            context.buildConstraintViolationWithTemplate("No metadata found for model=" + modelId)
                    .addConstraintViolation();
            result = false;
        } else {
            final Metadata metadata = dataSource.get().getMetadata(modelId);
            final String outcomeName = request.getOutcomeName();
            // Outcome name is not present
            if (!metadata.getOutputSchema().getItems().containsKey(outcomeName)) {
                context.buildConstraintViolationWithTemplate("No output found with name=" + outcomeName + " for model=" + modelId)
                        .addPropertyNode(modelId)
                        .addConstraintViolation();
                result = false;
            } else {
                validOutput = true;
            }
            final String protectedAttribute = request.getProtectedAttribute();
            if (!metadata.getInputSchema().getItems().containsKey(protectedAttribute)) {
                context.buildConstraintViolationWithTemplate("No protected attribute found with name=" + protectedAttribute + " for model=" + modelId)
                        .addPropertyNode(modelId)
                        .addConstraintViolation();
                result = false;
            } else {
                validAttribute = true;
            }

            if (validOutput) {
                // Output name guaranteed to exist
                final SchemaItem outcomeSchema = metadata.getOutputSchema().getItems().get(outcomeName);
                if (!PayloadConverter.checkValueType(outcomeSchema.getType(), request.getFavorableOutcome())) {
                    context.buildConstraintViolationWithTemplate(
                            String.format(
                                    "Invalid type for output: got '%s', expected object compatible with '%s'",
                                    request.getFavorableOutcome().asText(), outcomeSchema.getType().toString()))
                            .addPropertyNode(modelId)
                            .addConstraintViolation();
                    result = false;
                }
            }

            if (validAttribute) {
                // Protected attribute guaranteed to exist
                final SchemaItem protectedAttrSchema = metadata.getInputSchema().getItems().get(protectedAttribute);
                if (!PayloadConverter.checkValueType(protectedAttrSchema.getType(), request.getPrivilegedAttribute())) {
                    context.buildConstraintViolationWithTemplate(
                            String.format(
                                    "Received invalid type for privileged attribute: got '%s', expected object compatible with '%s'",
                                    request.getPrivilegedAttribute().asText(), protectedAttrSchema.getType().toString()))
                            .addPropertyNode(modelId)
                            .addPropertyNode(protectedAttribute)
                            .addConstraintViolation();
                    result = false;
                }
                if (!PayloadConverter.checkValueType(protectedAttrSchema.getType(), request.getUnprivilegedAttribute())) {
                    context.buildConstraintViolationWithTemplate(
                            String.format(
                                    "Received invalid type for unprivileged attribute: got '%s', expected object compatible with '%s'",
                                    request.getUnprivilegedAttribute().asText(), protectedAttrSchema.getType().toString()))
                            .addPropertyNode(modelId)
                            .addPropertyNode(protectedAttribute)
                            .addConstraintViolation();
                    result = false;
                }
            }
            if (Objects.nonNull(request.getBatchSize()) && request.getBatchSize() <= 0) {
                context.buildConstraintViolationWithTemplate("Request batch size must be bigger than 0.").addConstraintViolation();
                result = false;
            }
        }
        return result;
    }
}
