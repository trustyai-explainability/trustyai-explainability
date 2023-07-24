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

    @Override
    public boolean isValid(GroupMetricRequest request, ConstraintValidatorContext context) {
        final String modelId = request.getModelId();
        if (!dataSource.get().hasMetadata(modelId)) {
            context.buildConstraintViolationWithTemplate("No metadadata found for model=" + modelId).addConstraintViolation();
            return false;
        } else {
            final Metadata metadata = dataSource.get().getMetadata(modelId);
            final String outcomeName = request.getOutcomeName();

            // Outcome name is not present
            System.out.println(metadata.getInputSchema().retrieveNameMappedItems().keySet());
            System.out.println(metadata.getOutputSchema().retrieveNameMappedItems().keySet());
            if (!metadata.getOutputSchema().retrieveNameMappedItems().containsKey(outcomeName)) {
                context.buildConstraintViolationWithTemplate("No outcome found with name=" + outcomeName).addConstraintViolation();
                return false;
            }
            final String protectedAttribute = request.getProtectedAttribute();
            if (!metadata.getInputSchema().retrieveNameMappedItems().containsKey(protectedAttribute)) {
                context.buildConstraintViolationWithTemplate("No protected attribute found with name=" + protectedAttribute).addConstraintViolation();
                return false;
            }
            // Outcome name guaranteed to exist
            final SchemaItem outcomeSchema = metadata.getOutputSchema().retrieveNameMappedItems().get(outcomeName);
            LOG.info("trying to validate: " + request.getFavorableOutcome());
            if (!PayloadConverter.checkValueType(outcomeSchema.getType(), request.getFavorableOutcome().getRawValueNode())) {
                context.buildConstraintViolationWithTemplate(
                        "Invalid type for outcome. Got '" + request.getFavorableOutcome() + "', expected object compatible with '" + outcomeSchema.getType().toString() + "'").addConstraintViolation();
                return false;
            }
            // Protected attribute guaranteed to exist
            final SchemaItem protectedAttrSchema = metadata.getInputSchema().retrieveNameMappedItems().get(protectedAttribute);
            if (!PayloadConverter.checkValueType(protectedAttrSchema.getType(), request.getPrivilegedAttribute().getRawValueNode())) {
                context.buildConstraintViolationWithTemplate(
                        "Invalid type for privileged attribute. Got '" + request.getPrivilegedAttribute() + "', expected object compatible with ''" + protectedAttrSchema.getType().toString() + "'")
                        .addConstraintViolation();
                return false;
            }
            if (!PayloadConverter.checkValueType(protectedAttrSchema.getType(), request.getUnprivilegedAttribute().getRawValueNode())) {
                context.buildConstraintViolationWithTemplate(
                        "Invalid type for unprivileged attribute. Got '" + request.getUnprivilegedAttribute() + "', expected object compatible with ''" + protectedAttrSchema.getType().toString()
                                + "'")
                        .addConstraintViolation();
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
