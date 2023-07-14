package org.kie.trustyai.service.payloads.metrics;

import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.payloads.values.DataType;
import org.kie.trustyai.service.payloads.values.ReconcilableFeature;
import org.kie.trustyai.service.payloads.values.ReconcilableOutput;
import org.kie.trustyai.service.payloads.values.ReconcilerMatcher;
import org.kie.trustyai.service.payloads.values.TypedValue;

import javax.enterprise.inject.Instance;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

public class RequestReconciler {

    // For feature/output names passed as strings, reconcile the field name with the known data type of the feature/output
    public static void reconcile(BaseMetricRequest request, Instance<DataSource> dataSource) {
        final Metadata metadata = dataSource.get().getMetadata(request.getModelId());
        reconcile(request, metadata);
    }

    public static void reconcile(BaseMetricRequest request, Metadata metadata) {
        for (Field f : request.getClass().getDeclaredFields()) {
            if (f.getType().isAssignableFrom(ReconcilableFeature.class) && f.isAnnotationPresent(ReconcilerMatcher.class)) {
                ReconcilableFeature fieldValue;
                try {
                    fieldValue = (ReconcilableFeature) f.get(request);
                    if (fieldValue.getTypeToReconcile().isPresent()){
                        continue;
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("Field " + f.getName() + "was declared as a reconcilable input but does not have public access.");
                }
                String nameMethod = f.getAnnotation(ReconcilerMatcher.class).nameProvider();

                String name;
                try {
                    name = request.getClass().getDeclaredMethod(nameMethod).invoke(request).toString();
                } catch (NoSuchMethodException e) {
                    throw new IllegalArgumentException("Reconcilable matcher for field " + f.getName() + "gave a name-providing-method that does not exist: " + nameMethod);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("Method " + f.getName() + "was declared as the name source of the reconciled feature, but does not have public access:" + e);
                } catch (InvocationTargetException e) {
                    throw new IllegalArgumentException("Method " + f.getName() + "was declared as the name source of the reconciled feature, but returns exception:" + e);
                }

                DataType fieldDataType = metadata.getInputSchema().getItems().get(name).getType();
                TypedValue tv = new TypedValue();
                tv.setType(fieldDataType);
                tv.setValue(fieldValue.getRawValueNode());

                try {
                    ((ReconcilableFeature) f.get(request)).setTypeToReconcile(Optional.of(tv));
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("Reconciled field " + f.getName() + " does not have public access:" + e);
                }

            } else if (f.getType().isAssignableFrom(ReconcilableOutput.class) && f.isAnnotationPresent(ReconcilerMatcher.class)) {
                ReconcilableOutput fieldValue;
                try {
                    fieldValue = (ReconcilableOutput) f.get(request);
                    if (fieldValue.getTypeToReconcile().isPresent()){
                        continue;
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("Field " + f.getName() + "was declared as a reconcilable output but does not have public access.");
                }
                String nameMethod = f.getAnnotation(ReconcilerMatcher.class).nameProvider();

                String name;
                try {
                    name = request.getClass().getDeclaredMethod(nameMethod).invoke(request).toString();
                } catch (NoSuchMethodException e) {
                    throw new IllegalArgumentException("Reconcilable matcher for field " + f.getName() + "gave a name-providing-method that does not exist: " + nameMethod);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("Method " + f.getName() + "was declared as the name source of the reconciled output, but does not have public access:" + e);
                } catch (InvocationTargetException e) {
                    throw new IllegalArgumentException("Method " + f.getName() + "was declared as the name source of the reconciled output, but returns exception:" + e);
                }

                DataType fieldDataType = metadata.getOutputSchema().getItems().get(name).getType();
                TypedValue tv = new TypedValue();
                tv.setType(fieldDataType);
                tv.setValue(fieldValue.getRawValueNode());

                try {
                    ((ReconcilableOutput) f.get(request)).setTypeToReconcile(Optional.of(tv));
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("Reconciled field " + f.getName() + " does not have public access:" + e);
                }
            }
        }
    }
}
