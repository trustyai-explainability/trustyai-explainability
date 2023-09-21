package org.kie.trustyai.service.validators.generic;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.validation.ConstraintValidatorContext;

import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.payloads.PayloadConverter;
import org.kie.trustyai.service.payloads.service.SchemaItem;

import com.fasterxml.jackson.databind.node.ValueNode;

@ApplicationScoped
public class GenericValidationUtils {
    public static boolean validateModelId(ConstraintValidatorContext context, Instance<DataSource> dataSource, String modelId) {
        if (!dataSource.get().hasMetadata(modelId)) {
            context.buildConstraintViolationWithTemplate("No metadata found for model=" + modelId)
                    .addPropertyNode(modelId)
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

    public static boolean validateFeatureColumnName(ConstraintValidatorContext context, Metadata metadata, String modelId, String columnName, String objectName) {
        if (!metadata.getInputSchema().retrieveNameMappedItems().containsKey(columnName)) {
            context.buildConstraintViolationWithTemplate("No " + objectName + " found with name=" + columnName)
                    .addPropertyNode(modelId)
                    .addPropertyNode(columnName)
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

    public static boolean validateFeatureColumnName(ConstraintValidatorContext context, Metadata metadata, String modelId, String columnName) {
        return validateFeatureColumnName(context, metadata, modelId, columnName, "feature");
    }

    public static boolean validateOutputColumnName(ConstraintValidatorContext context, Metadata metadata, String modelId, String columnName, String objectName) {
        if (!metadata.getOutputSchema().retrieveNameMappedItems().containsKey(columnName)) {
            context.buildConstraintViolationWithTemplate("No " + objectName + " found with name=" + columnName)
                    .addPropertyNode(modelId)
                    .addPropertyNode(columnName)
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

    public static boolean validateOutputColumnName(ConstraintValidatorContext context, Metadata metadata, String modelId, String columnName) {
        return validateOutputColumnName(context, metadata, modelId, columnName, "output");
    }

    public static boolean validateColumnName(ConstraintValidatorContext context, Metadata metadata, String modelId, String columnName) {
        if (!metadata.getOutputSchema().retrieveNameMappedItems().containsKey(columnName) && !metadata.getInputSchema().retrieveNameMappedItems().containsKey(columnName)) {
            context.buildConstraintViolationWithTemplate("No feature or output found with name=" + columnName)
                    .addPropertyNode(modelId)
                    .addPropertyNode(columnName)
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

    // check to see if the provided output value has a compatible type
    public static boolean validateOutputColumnType(ConstraintValidatorContext context, Metadata metadata, String modelId, String columnName, ValueNode valueNode, String objectName) {
        // Output name guaranteed to exist
        final SchemaItem outcomeSchema = metadata.getOutputSchema().retrieveNameMappedItems().get(columnName);
        if (!PayloadConverter.checkValueType(outcomeSchema.getType(), valueNode)) {
            context.buildConstraintViolationWithTemplate(
                    String.format(
                            "Invalid type for %s=%s: got '%s', expected object compatible with '%s'",
                            objectName,
                            columnName,
                            valueNode.asText(),
                            outcomeSchema.getType().toString()))
                    .addPropertyNode(modelId)
                    .addPropertyNode(columnName)
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

    public static boolean validateOutputColumnType(ConstraintValidatorContext context, Metadata metadata, String modelId, String columnName, ValueNode valueNode) {
        return validateOutputColumnType(context, metadata, modelId, columnName, valueNode, "output");
    }

    // check to see if the provided attribute values have a compatible type
    public static boolean validateFeatureColumnType(ConstraintValidatorContext context, Metadata metadata, String modelId, String columnName, ValueNode valueNode, String objectName) {
        // Protected attribute guaranteed to exist
        final SchemaItem protectedAttrSchema = metadata.getInputSchema().retrieveNameMappedItems().get(columnName);
        boolean result = true;
        if (!PayloadConverter.checkValueType(protectedAttrSchema.getType(), valueNode)) {
            context.buildConstraintViolationWithTemplate(
                    String.format(
                            "Received invalid type for %s=%s: got '%s', expected object compatible with '%s'",
                            objectName,
                            columnName,
                            valueNode.asText(),
                            protectedAttrSchema.getType().toString()))
                    .addPropertyNode(modelId)
                    .addPropertyNode(columnName)
                    .addConstraintViolation();
        }
        return result;
    }

    public static boolean validateFeatureColumnType(ConstraintValidatorContext context, Metadata metadata, String modelId, String columnName, ValueNode valueNode) {
        return validateFeatureColumnType(context, metadata, modelId, columnName, valueNode, "feature");
    }

    // if tag is invalid, return error string. Else return nothing
    public static Optional<String> validateDataTag(String dataTag) {
        if (dataTag.startsWith(Dataframe.TRUSTYAI_INTERNAL_TAG_PREFIX)) {
            return Optional.of(String.format(
                    "The tag prefix '%s' is reserved for internal TrustyAI use only. Provided tag '%s' violates this restriction.",
                    Dataframe.TRUSTYAI_INTERNAL_TAG_PREFIX,
                    dataTag));
        }
        return Optional.empty();
    }
}
