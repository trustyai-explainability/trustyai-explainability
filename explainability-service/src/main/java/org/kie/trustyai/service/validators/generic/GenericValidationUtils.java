package org.kie.trustyai.service.validators.generic;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.datasources.DataSource;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.payloads.PayloadConverter;
import org.kie.trustyai.service.payloads.service.SchemaItem;

import com.fasterxml.jackson.databind.node.ValueNode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.validation.ConstraintValidatorContext;

@ApplicationScoped
public class GenericValidationUtils {

    private static String getEnumerateMessage(Set<String> names, String objectName) {
        if (names.size() > 10) {
            return String.format(
                    "The valid list of %s names is too long to display (length=%d), please query TrustyAI's metadata endpoint (/info) to see the list in full.", objectName, names.size());
        } else {
            return "The valid list of " + objectName + " names is as follows: " + names.toString();
        }
    }

    public static boolean validateModelId(ConstraintValidatorContext context, Instance<DataSource> dataSource, String modelId) {
        if (!dataSource.get().hasMetadata(modelId)) {
            context.buildConstraintViolationWithTemplate("No metadata found for model=" + modelId + ". This can happen if TrustyAI has not yet logged any inferences from this model.")
                    .addPropertyNode(modelId)
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

    public static boolean validateFeatureColumnName(ConstraintValidatorContext context, StorageMetadata metadata, String modelId, String columnName, String objectName) {
        if (!metadata.getInputSchema().getNameMappedItems().containsKey(columnName)) {
            context.buildConstraintViolationWithTemplate(
                    String.format(
                            "No %s found with name=%s. %s", objectName, columnName, getEnumerateMessage(metadata.getInputSchema().getNameMappedKeySet(), objectName)))
                    .addPropertyNode(modelId)
                    .addPropertyNode(columnName)
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

    public static boolean validateFeatureColumnName(ConstraintValidatorContext context, StorageMetadata storageMetadata, String modelId, String columnName) {
        return validateFeatureColumnName(context, storageMetadata, modelId, columnName, "feature");
    }

    public static boolean validateOutputColumnName(ConstraintValidatorContext context, StorageMetadata metadata, String modelId, String columnName, String objectName) {
        if (!metadata.getOutputSchema().getNameMappedItems().containsKey(columnName)) {
            context.buildConstraintViolationWithTemplate(
                    String.format(
                            "No %s found with name=%s. %s", objectName, columnName, getEnumerateMessage(metadata.getOutputSchema().getNameMappedKeySet(), objectName)))
                    .addPropertyNode(modelId)
                    .addPropertyNode(columnName)
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

    public static boolean validateOutputColumnName(ConstraintValidatorContext context, StorageMetadata storageMetadata, String modelId, String columnName) {
        return validateOutputColumnName(context, storageMetadata, modelId, columnName, "output");
    }

    public static boolean validateColumnName(ConstraintValidatorContext context, StorageMetadata metadata, String modelId, String columnName) {
        String objectName = "feature or output";
        if (!metadata.getOutputSchema().getNameMappedItems().containsKey(columnName) && !metadata.getInputSchema().getNameMappedItems().containsKey(columnName)) {
            Set<String> nameSet = new HashSet<>(metadata.getInputSchema().getNameMappedKeySet());
            nameSet.addAll(metadata.getOutputSchema().getNameMappedKeySet());
            context.buildConstraintViolationWithTemplate(String.format(
                    "No %s found with name=%s. %s", objectName, columnName, getEnumerateMessage(nameSet, objectName)))
                    .addPropertyNode(modelId)
                    .addPropertyNode(columnName)
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

    // check to see if the provided output value has a compatible type
    public static boolean validateOutputColumnType(ConstraintValidatorContext context, StorageMetadata storageMetadata, String modelId, String columnName, List<ValueNode> valueNodes,
            String objectName) {
        // Output name guaranteed to exist
        final SchemaItem outcomeSchema = storageMetadata.getOutputSchema().getNameMappedItems().get(columnName);
        boolean result = true;

        for (ValueNode subNode : valueNodes) {
            if (!PayloadConverter.checkValueType(outcomeSchema.getType(), subNode)) {
                context.buildConstraintViolationWithTemplate(
                        String.format(
                                "Invalid type for %s=%s: got '%s', expected object compatible with '%s'",
                                objectName,
                                columnName,
                                subNode.asText(),
                                outcomeSchema.getType().toString()))
                        .addPropertyNode(modelId)
                        .addPropertyNode(columnName)
                        .addConstraintViolation();
                result = false;
            }
        }
        return result;
    }

    public static boolean validateOutputColumnType(ConstraintValidatorContext context, StorageMetadata storageMetadata, String modelId, String columnName, List<ValueNode> valueNodes) {
        return validateOutputColumnType(context, storageMetadata, modelId, columnName, valueNodes, "output");
    }

    // check to see if the provided attribute values have a compatible type
    public static boolean validateFeatureColumnType(ConstraintValidatorContext context, StorageMetadata storageMetadata, String modelId, String columnName, List<ValueNode> valueNodes,
            String objectName) {
        // Protected attribute guaranteed to exist
        final SchemaItem protectedAttrSchema = storageMetadata.getInputSchema().getNameMappedItems().get(columnName);
        boolean result = true;

        for (ValueNode subNode : valueNodes) {
            if (!PayloadConverter.checkValueType(protectedAttrSchema.getType(), (ValueNode) subNode)) {
                context.buildConstraintViolationWithTemplate(
                        String.format(
                                "Received invalid type for %s=%s: got '%s', expected object compatible with '%s'",
                                objectName,
                                columnName,
                                subNode.asText(),
                                protectedAttrSchema.getType().toString()))
                        .addPropertyNode(modelId)
                        .addPropertyNode(columnName)
                        .addConstraintViolation();
                result = false;
            }
        }
        return result;
    }

    public static boolean validateFeatureColumnType(ConstraintValidatorContext context, StorageMetadata storageMetadata, String modelId, String columnName, List<ValueNode> valueNodes) {
        return validateFeatureColumnType(context, storageMetadata, modelId, columnName, valueNodes, "feature");
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

    // validate actual dataframe columns, not mapping
    public static boolean validateNonMappedColumnName(ConstraintValidatorContext context, StorageMetadata metadata, String modelId, String columnName) {
        String objectName = "feature or output";
        if (!metadata.getOutputSchema().getItems().containsKey(columnName) && !metadata.getInputSchema().getItems().containsKey(columnName)) {
            Set<String> nameSet = new HashSet<>(metadata.getInputSchema().getItems().keySet());
            nameSet.addAll(metadata.getOutputSchema().getItems().keySet());
            context.buildConstraintViolationWithTemplate(String.format(
                    "No %s found in original dataframe columns with name=%s. %s", objectName, columnName, getEnumerateMessage(nameSet, objectName)))
                    .addPropertyNode(modelId)
                    .addPropertyNode(columnName)
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
