package org.kie.trustyai.service.data.storage.hibernate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.SimplePrediction;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.explainability.model.tensor.Tensor1D;
import org.kie.trustyai.explainability.model.tensor.Tensor2D;
import org.kie.trustyai.explainability.model.tensor.Tensor3D;
import org.kie.trustyai.explainability.model.tensor.Tensor4D;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateStorage;
import org.kie.trustyai.service.profiles.hibernate.HibernateTestProfile;
import org.kie.trustyai.service.utils.DataframeGenerators;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(HibernateTestProfile.class)
@QuarkusTestResource(H2DatabaseTestResource.class)
class HibernateTensorStorageTest {
    @Inject
    Instance<MockHibernateStorage> storage;

    String MODEL_ID = "example_model";

    @BeforeEach
    void emptyStorage() {
        storage.get().clearData(MODEL_ID);
    }

    Random rng = new Random(0L);

    public <T> Tensor1D<T> generate(int i, Supplier<T> generator) {
        return new Tensor1D<>(IntStream.range(0, i).mapToObj(ignored -> generator.get()).collect(Collectors.toList()));
    }

    public <T> Tensor2D<T> generate(int i, int ii, Supplier<T> generator) {
        return new Tensor2D<>(IntStream.range(0, i).mapToObj(ignored -> generate(ii, generator)).collect(Collectors.toList()));
    }

    public <T> Tensor3D<T> generate(int i, int ii, int iii, Supplier<T> generator) {
        return new Tensor3D<>(IntStream.range(0, i).mapToObj(ignored -> generate(ii, ii, generator)).collect(Collectors.toList()));
    }

    public <T> Tensor4D<T> generate(int i, int ii, int iii, int iv, Supplier<T> generator) {
        return new Tensor4D<>(IntStream.range(0, i).mapToObj(ignored -> generate(ii, iii, iv, generator)).collect(Collectors.toList()));
    }

    @Test
    void test1DReadWrite() {
        int nrows = 10;
        int[] shape = { 256 };

        List<Prediction> ps = new ArrayList<>();
        for (int i = 0; i < nrows; i++) {
            Tensor1D<Double> tensor = generate(shape[0], () -> rng.nextDouble());
            Feature f = new Feature("image", Type.TENSOR, new Value(tensor));
            Output o = new Output("prediction", Type.NUMBER, new Value(1.), 1.);
            ps.add(new SimplePrediction(
                    new PredictionInput(List.of(f)),
                    new PredictionOutput(List.of(o))));
        }
        Dataframe original = Dataframe.createFrom(ps);
        storage.get().saveDataframe(original, MODEL_ID);

        assertEquals(nrows, storage.get().rowCount(MODEL_ID));
        DataframeGenerators.roughEqualityCheck(original, storage.get().readDataframe(MODEL_ID));
    }

    @Test
    void test2DReadWrite() {
        int nrows = 10;
        int[] shape = { 64, 64 };

        List<Prediction> ps = new ArrayList<>();
        for (int i = 0; i < nrows; i++) {
            Tensor2D<Double> tensor = generate(shape[0], shape[1], () -> rng.nextDouble());
            Feature f = new Feature("image", Type.TENSOR, new Value(tensor));
            Output o = new Output("prediction", Type.NUMBER, new Value(1.), 1.);
            ps.add(new SimplePrediction(
                    new PredictionInput(List.of(f)),
                    new PredictionOutput(List.of(o))));
        }
        Dataframe original = Dataframe.createFrom(ps);
        storage.get().saveDataframe(original, MODEL_ID);

        assertEquals(nrows, storage.get().rowCount(MODEL_ID));
        DataframeGenerators.roughEqualityCheck(original, storage.get().readDataframe(MODEL_ID));
    }

    @Test
    void test3DReadWrite() {
        int nrows = 10;
        int[] shape = { 3, 28, 28 };

        List<Prediction> ps = new ArrayList<>();
        for (int i = 0; i < nrows; i++) {
            Tensor3D<Double> tensor = generate(shape[0], shape[1], shape[2], () -> rng.nextDouble());
            Feature f = new Feature("image", Type.TENSOR, new Value(tensor));
            Output o = new Output("prediction", Type.NUMBER, new Value(1.), 1.);
            ps.add(new SimplePrediction(
                    new PredictionInput(List.of(f)),
                    new PredictionOutput(List.of(o))));
        }
        Dataframe original = Dataframe.createFrom(ps);
        storage.get().saveDataframe(original, MODEL_ID);

        assertEquals(nrows, storage.get().rowCount(MODEL_ID));
        DataframeGenerators.roughEqualityCheck(original, storage.get().readDataframe(MODEL_ID));
    }

    @Test
    void test4DReadWrite() {
        int nrows = 10;
        int[] shape = { 1, 3, 28, 28 };

        List<Prediction> ps = new ArrayList<>();
        for (int i = 0; i < nrows; i++) {
            Tensor4D<Double> tensor = generate(shape[0], shape[1], shape[2], shape[3], () -> rng.nextDouble());
            Feature f = new Feature("image", Type.TENSOR, new Value(tensor));
            Output o = new Output("prediction", Type.NUMBER, new Value(1.), 1.);
            ps.add(new SimplePrediction(
                    new PredictionInput(List.of(f)),
                    new PredictionOutput(List.of(o))));
        }
        Dataframe original = Dataframe.createFrom(ps);
        storage.get().saveDataframe(original, MODEL_ID);

        assertEquals(nrows, storage.get().rowCount(MODEL_ID));
        DataframeGenerators.roughEqualityCheck(original, storage.get().readDataframe(MODEL_ID));
    }

    @Test
    void test4DReadWriteFlat() {
        int nrows = 10;
        int[] shape = { 1, 3, 28, 28 };

        List<Prediction> ps = new ArrayList<>();
        for (int r = 0; r < nrows; r++) {
            List<Feature> fs = new ArrayList<>();
            for (int i = 0; i < shape[0]; i++) {
                for (int ii = 0; ii < shape[1]; ii++) {
                    for (int iii = 0; iii < shape[2]; iii++) {
                        for (int iv = 0; iv < shape[3]; iv++) {
                            fs.add(new Feature("f" + i + "," + ii + "," + ii + "," + iv, Type.NUMBER, new Value(rng.nextDouble())));
                        }
                    }
                }
            }
            Output o = new Output("prediction", Type.NUMBER, new Value(1.), 1.);
            ps.add(new SimplePrediction(
                    new PredictionInput(fs),
                    new PredictionOutput(List.of(o))));
        }
        Dataframe original = Dataframe.createFrom(ps);
        storage.get().saveDataframe(original, MODEL_ID);
        assertEquals(nrows, storage.get().rowCount(MODEL_ID));
        DataframeGenerators.roughEqualityCheck(original, storage.get().readDataframe(MODEL_ID));
    }

    @Test
    void test4DReadWriteArray() {
        int nrows = 10;
        int[] shape = { 1, 3, 28, 28 };

        List<Prediction> ps = new ArrayList<>();
        for (int r = 0; r < nrows; r++) {
            Double[][][][] data = new Double[shape[0]][shape[1]][shape[2]][shape[3]];
            for (int i = 0; i < shape[0]; i++) {
                for (int ii = 0; ii < shape[1]; ii++) {
                    for (int iii = 0; iii < shape[2]; iii++) {
                        for (int iv = 0; iv < shape[3]; iv++) {
                            data[i][ii][iii][iv] = rng.nextDouble();
                        }
                    }
                }
            }
            Feature f = new Feature("image", Type.TENSOR, new Value(data));
            Output o = new Output("prediction", Type.NUMBER, new Value(1.), 1.);
            ps.add(new SimplePrediction(
                    new PredictionInput(List.of(f)),
                    new PredictionOutput(List.of(o))));
        }
        Dataframe original = Dataframe.createFrom(ps);
        storage.get().saveDataframe(original, MODEL_ID);
        assertEquals(nrows, storage.get().rowCount(MODEL_ID));
        DataframeGenerators.roughEqualityCheck(original, storage.get().readDataframe(MODEL_ID));
    }

    @Test
    void test3DReadWriteCOCO() {
        int nrows = 5;
        int[] shape = { 3, 640, 640 };

        List<Prediction> ps = new ArrayList<>();
        for (int i = 0; i < nrows; i++) {
            Tensor3D<Double> tensor = generate(shape[0], shape[1], shape[2], () -> rng.nextDouble());
            Feature f = new Feature("image", Type.TENSOR, new Value(tensor));
            Output o = new Output("prediction", Type.NUMBER, new Value(1.), 1.);
            ps.add(new SimplePrediction(
                    new PredictionInput(List.of(f)),
                    new PredictionOutput(List.of(o))));
        }
        Dataframe original = Dataframe.createFrom(ps);
        storage.get().saveDataframe(original, MODEL_ID);

        assertEquals(nrows, storage.get().rowCount(MODEL_ID));
        DataframeGenerators.roughEqualityCheck(original, storage.get().readDataframe(MODEL_ID));
    }

    @Test
    void test3DBatchWrite() {
        int nrows = 5;
        int[] shape = { 3, 256, 256 };
        int batches = 10;

        for (int b = 0; b < batches; b++) {
            List<Prediction> ps = new ArrayList<>();
            for (int i = 0; i < nrows; i++) {
                Tensor3D<Double> tensor = generate(shape[0], shape[1], shape[2], () -> rng.nextDouble());
                Feature f = new Feature("image", Type.TENSOR, new Value(tensor));
                Output o = new Output("prediction", Type.NUMBER, new Value(1.), 1.);
                ps.add(new SimplePrediction(
                        new PredictionInput(List.of(f)),
                        new PredictionOutput(List.of(o))));
            }
            Dataframe original = Dataframe.createFrom(ps);
            storage.get().saveDataframe(original, MODEL_ID);
            DataframeGenerators.roughEqualityCheck(original, storage.get().readDataframe(MODEL_ID, nrows));
        }

        assertEquals(nrows * batches, storage.get().rowCount(MODEL_ID));
    }

    @Test
    void test4DReadWriteInteger() {
        int nrows = 5;
        int[] shape = { 64, 3, 28, 28 };
        int batches = 10;

        for (int b = 0; b < batches; b++) {
            List<Prediction> ps = new ArrayList<>();
            for (int i = 0; i < nrows; i++) {
                Tensor4D<Integer> tensor = generate(shape[0], shape[1], shape[2], shape[3], () -> rng.nextInt());
                Feature f = new Feature("image", Type.TENSOR, new Value(tensor));
                Output o = new Output("prediction", Type.NUMBER, new Value(1.), 1.);
                ps.add(new SimplePrediction(
                        new PredictionInput(List.of(f)),
                        new PredictionOutput(List.of(o))));
            }
            Dataframe original = Dataframe.createFrom(ps);
            storage.get().saveDataframe(original, MODEL_ID);
            DataframeGenerators.roughEqualityCheck(original, storage.get().readDataframe(MODEL_ID, nrows));
        }

        assertEquals(nrows * batches, storage.get().rowCount(MODEL_ID));
    }

    @Test
    void test4DReadWriteString() {
        int nrows = 5;
        int[] shape = { 2, 3, 4, 5 };
        int batches = 10;

        for (int b = 0; b < batches; b++) {
            List<Prediction> ps = new ArrayList<>();
            for (int i = 0; i < nrows; i++) {
                Tensor4D<String> tensor = generate(shape[0], shape[1], shape[2], shape[3], () -> UUID.randomUUID().toString());
                Feature f = new Feature("image", Type.TENSOR, new Value(tensor));
                Output o = new Output("prediction", Type.NUMBER, new Value(1.), 1.);
                ps.add(new SimplePrediction(
                        new PredictionInput(List.of(f)),
                        new PredictionOutput(List.of(o))));
            }
            Dataframe original = Dataframe.createFrom(ps);
            storage.get().saveDataframe(original, MODEL_ID);
            DataframeGenerators.roughEqualityCheck(original, storage.get().readDataframe(MODEL_ID, nrows));
        }

        assertEquals(nrows * batches, storage.get().rowCount(MODEL_ID));
    }
}
