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
        if (!dataSource.get().hasMetadata(modelId)) {
            context.buildConstraintViolationWithTemplate("No metadadata found for model=" + modelId).addConstraintViolation();
            return false;
        } else {
            final Metadata metadata = dataSource.get().getMetadata(modelId);
            final String outcomeName = request.getOutcomeName();
            // Outcome name is not present
            if (metadata.getOutputSchema().getItems().stream().noneMatch(item -> Objects.equals(item.getName(), outcomeName))) {
                context.buildConstraintViolationWithTemplate("No outcome found with name=" + outcomeName).addConstraintViolation();
                return false;
            }
            final String protectedAttribute = request.getProtectedAttribute();
            if (metadata.getInputSchema().getItems().stream().noneMatch(item -> Objects.equals(item.getName(), protectedAttribute))) {
                context.buildConstraintViolationWithTemplate("No protected attribute found with name=" + protectedAttribute).addConstraintViolation();
                return false;
            }
            // Outcome name guaranteed to exist
            final SchemaItem outcomeSchema = metadata.getOutputSchema().getItems().stream().filter(item -> item.getName().equals(outcomeName)).findFirst().get();
            if (!outcomeSchema.getType().equals(request.getFavorableOutcome().getType())) {
                context.buildConstraintViolationWithTemplate(
                        "Invalid type for outcome. Got '" + request.getFavorableOutcome().getType().toString() + "', expected '" + outcomeSchema.getType().toString() + "'").addConstraintViolation();
                return false;
            }
            // Protected attribute guaranteed to exist
            final SchemaItem protectedAttrSchema = metadata.getInputSchema().getItems().stream().filter(item -> item.getName().equals(protectedAttribute)).findFirst().get();
            if (!protectedAttrSchema.getType().equals(request.getPrivilegedAttribute().getType())) {
                context.buildConstraintViolationWithTemplate(
                        "Invalid type for privileged attribute. Got '" + request.getPrivilegedAttribute().getType().toString() + "', expected '" + protectedAttrSchema.getType().toString() + "'")
                        .addConstraintViolation();
                return false;
            }
            if (!protectedAttrSchema.getType().equals(request.getUnprivilegedAttribute().getType())) {
                context.buildConstraintViolationWithTemplate(
                        "Invalid type for unprivileged attribute. Got '" + request.getUnprivilegedAttribute().getType().toString() + "', expected '" + protectedAttrSchema.getType().toString() + "'")
                        .addConstraintViolation();
                return false;
            }
        }
        return true;
    }
}
