package org.kie.trustyai.metrics.drift.meanshift;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.PredictionInput;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MeanshiftTest {
    List<Double> col1 = List.of(1., 2., 1., 2., 1., 2.);
    List<Double> col2 = List.of(2.99, 3., 3., 3., 3., 3.01);
    List<Double> col3 = List.of(1.4,1.5,1.5,1.5,1.5,1.6);
    List<Double> col4 = List.of(1., 1., 1., 1., 1., 2.);
    List<List<Double>>  cols = List.of(col1, col2, col3, col4);
    int ncols = cols.size();

    double[][] pvalTable = {
            {0.5, 0.0005568075337, 0.5, 0.5},
            {0.0005568075337, 0.5, 0.00000001103451635, 0.00000001103451635},
            {0.5, 0.00000001103451635, 0.5, 0.5},
            {0.1308777346, 0.00005389942698, 0.0512178816, 0.0512178816},
    };

    public Dataframe generate(int col){
        List<PredictionInput> ps = new ArrayList<>();
        for (int i=0; i<6; i++){
            ps.add(new PredictionInput(List.of(FeatureFactory.newNumericalFeature(String.valueOf(i), cols.get(col).get(i)))));
        }
        return Dataframe.createFromInputs(ps);
    }


    @Test
    public void testTTest(){
        for (int i=0; i<ncols; i++){
            for (int j=0; j<ncols; j++){

            }
        }
    }
}