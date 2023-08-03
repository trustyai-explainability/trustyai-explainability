package org.kie.trustyai.service.validators.metrics.fairness.group;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.jboss.logging.Logger;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.payloads.PayloadConverter;
import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;
import org.kie.trustyai.service.payloads.service.SchemaItem;

@ApplicationScoped
public class GroupMetricRequestValidator implements ConstraintValidator<ValidGroupMetricRequest, GroupMetricRequest> {
    private static final Logger LOG = Logger.getLogger(GroupMetricRequestValidator.class);

    @Inject
    Instance<DataSource> dataSource;

    @Override
    public void initialize(ValidGroupMetricRequest constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    // check to see if the provided output name is in the output schema
    private boolean validateOutputName(ConstraintValidatorContext context, Metadata metadata, String outcomeName, String modelId) {
        if (!metadata.getOutputSchema().retrieveNameMappedItems().containsKey(outcomeName)) {
            context.buildConstraintViolationWithTemplate("No output found with name=" + outcomeName + " for model=" + modelId)
                    .addPropertyNode(modelId)
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

    // check to see if the provided output value has a compatible type
    private boolean validateOutput(GroupMetricRequest request, ConstraintValidatorContext context, Metadata metadata, String outcomeName, String modelId) {
        // Output name guaranteed to exist
        final SchemaItem outcomeSchema = metadata.getOutputSchema().retrieveNameMappedItems().get(outcomeName);
        if (!PayloadConverter.checkValueType(outcomeSchema.getType(), request.getFavorableOutcome().getRawValueNode())) {
            context.buildConstraintViolationWithTemplate(
                    String.format(
                            "Invalid type for output: got '%s', expected object compatible with '%s'",
                            request.getFavorableOutcome().getRawValueNode().asText(), outcomeSchema.getType().toString()))
                    .addPropertyNode(modelId)
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

    // check to see if the provided attribute name is in the input schema
    private boolean validateAttributeName(ConstraintValidatorContext context, Metadata metadata, String protectedAttribute, String modelId) {
        if (!metadata.getInputSchema().retrieveNameMappedItems().containsKey(protectedAttribute)) {
            context.buildConstraintViolationWithTemplate("No protected attribute found with name=" + protectedAttribute + " for model=" + modelId)
                    .addPropertyNode(modelId)
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

    // check to see if the provided attribute values have a compatible type
    private boolean validateAttribute(GroupMetricRequest request, ConstraintValidatorContext context, Metadata metadata, String protectedAttribute, String modelId) {
        // Protected attribute guaranteed to exist
        final SchemaItem protectedAttrSchema = metadata.getInputSchema().retrieveNameMappedItems().get(protectedAttribute);
        boolean result = true;
        if (!PayloadConverter.checkValueType(protectedAttrSchema.getType(), request.getPrivilegedAttribute().getRawValueNode())) {
            context.buildConstraintViolationWithTemplate(
                    String.format(
                            "Received invalid type for privileged attribute: got '%s', expected object compatible with '%s'",
                            request.getPrivilegedAttribute().getRawValueNode().asText(), protectedAttrSchema.getType().toString()))
                    .addPropertyNode(modelId)
                    .addPropertyNode(protectedAttribute)
                    .addConstraintViolation();
            result = false;
        }
        if (!PayloadConverter.checkValueType(protectedAttrSchema.getType(), request.getUnprivilegedAttribute().getRawValueNode())) {
            context.buildConstraintViolationWithTemplate(
                    String.format(
                            "Received invalid type for unprivileged attribute: got '%s', expected object compatible with '%s'",
                            request.getUnprivilegedAttribute().getRawValueNode().asText(), protectedAttrSchema.getType().toString()))
                    .addPropertyNode(modelId)
                    .addPropertyNode(protectedAttribute)
                    .addConstraintViolation();
            result = false;
        }
        return result;
    }

    @Override
    public boolean isValid(GroupMetricRequest request, ConstraintValidatorContext context) {
        final String modelId = request.getModelId();
        boolean result = true;

        if (!dataSource.get().hasMetadata(modelId)) {
            context.buildConstraintViolationWithTemplate("No metadata found for model=" + modelId)
                    .addConstraintViolation();
            result = false;
        } else {
            final Metadata metadata = dataSource.get().getMetadata(modelId);
            final String outcomeName = request.getOutcomeName();
            final String protectedAttribute = request.getProtectedAttribute();

            // Outcome name or attribute name not present
            boolean validOutputName = validateOutputName(context, metadata, outcomeName, modelId);
            boolean validAttributeName = validateAttributeName(context, metadata, protectedAttribute, modelId);

            // set result to failure if either above are false
            result = validOutputName && validAttributeName;

            if (validOutputName) {
                result = validateOutput(request, context, metadata, outcomeName, modelId) && result;
            }

            if (validAttributeName) {
                result = validateAttribute(request, context, metadata, protectedAttribute, modelId) && result;
            }
            if (Objects.nonNull(request.getBatchSize()) && request.getBatchSize() <= 0) {
                context.buildConstraintViolationWithTemplate("Request batch size must be bigger than 0.").addConstraintViolation();
                result = false;
            }
        }
        return result;
    }
}
