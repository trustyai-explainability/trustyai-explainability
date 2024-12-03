package org.kie.trustyai.metrics.anomaly;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.SimplePrediction;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.metrics.utils.PregeneratedNormalData;

import static org.junit.jupiter.api.Assertions.*;

class GaussianAnomalyDetectionTest {

    double[][] cols = PregeneratedNormalData.getData();
    int nReferenceCols = cols.length;

    double[] expected1StdProbs = {
            0.02713572967314215, 0.01548555645215699, 0.007774042014177973, 0.0020601005586896592, 0.13796701569562597, 3.0851485330885e-06, 0.11304512642023479, 0.026737676397666332,
            4.363432737353179e-08, 0.204955955592258, 4.532914331889515e-06, 0.003407331769895383, 0.1581476067680453, 0.0009014216024946231, 0.08126555740285857, 0.1406482704321791,
            3.6854159299881672e-06, 0.203426462379647, 0.0073614265759841535, 0.15560700268499683, 0.010604516546431242, 0.017639095307377506, 0.08553458133885972, 0.01431881104106103,
            0.006071801726143744
    };
    double[] expected2StdProbs = {
            0.1777509956416291, 0.12362999747831194, 0.07793160369866337, 0.030830672303424844, 0.46615230681298014, 0.00021539465543751746, 0.41799135262827714, 0.17608759914055017,
            6.76307021285627e-06, 0.5731185381645365, 0.0002926568251407913, 0.04403712041588692, 0.5013799883663284, 0.016966173231817705, 0.3466645172102646, 0.4710101004316163,
            0.00024820477597586876, 0.5709605037828422, 0.07508888279981263, 0.4971070579705881, 0.09612635514074641, 0.13464773782804285, 0.35704771833236204, 0.1174062017370181, 0.06580885438204975
    };
    double[] expectedTenthStdProbs = {
            0.0011242410253656798, 0.000554787449550731, 0.0002350155606486437, 4.591300132938958e-05, 0.009129896034754892, 2.0199791728181538e-08, 0.007031354479021257, 0.0011034165551345732,
            1.4796930347671378e-10, 0.015412207164206992, 3.1634807773350815e-08, 8.49718164651625e-05, 0.010928913553429176, 1.682341095010642e-05, 0.004576057693319369, 0.009363954706667843,
            2.4849586610642405e-08, 0.015259262456872147, 0.0002196524123235788, 0.010697767361418586, 0.00034569285061925026, 0.0006532269038627136, 0.004890150995929932, 0.0005029488006941252,
            0.00017307060103166627
    };

    double[] expected1StdNormalizedProbs = {
            0.039748275000098025, 0.02268316215572782, 0.01138737611127157, 0.003017624531235623, 0.20209336350517876, 4.5191094467131385e-06, 0.1655879103490508, 0.03916520864260986,
            6.391533468156838e-08, 0.30021841254750453, 6.639789222036676e-06, 0.00499104176809445, 0.2316537878340287, 0.0013203976520464963, 0.1190373637485843, 0.2060208514296812,
            5.398377992389145e-06, 0.29797801888358116, 0.010782979173943342, 0.22793232425166804, 0.01553344041261708, 0.025837654615365795, 0.12529060769794909, 0.020974119575559214,
            0.008893943434132292
    };
    double[] expected2StdNormalizedProbs = {
            0.18622424807284443, 0.1295233438020437, 0.08164654294907148, 0.03230034659755755, 0.4883734266034038, 0.00022566235200522828, 0.43791667699622155, 0.18448155874757644,
            7.085460537122581e-06, 0.6004386554406612, 0.0003066075495583102, 0.046136335873334676, 0.525280384479737, 0.017774937582565747, 0.3631897465214417, 0.4934627874852267,
            0.0002600365056035156, 0.5981777492297244, 0.07866831174446687, 0.52080376679812, 0.10070862411459987, 0.14106629130950593, 0.3740679068072503, 0.12300286453329086, 0.06894591155225221
    };
    double[] expectedTenthStdNormalizedProbs = {
            0.014113759398305246, 0.006964820179562059, 0.0029503932012922875, 0.0005763933528455769, 0.1146170198905155, 2.5358886031996424e-07, 0.08827185907828147, 0.013852328303186289,
            1.8576115801554735e-09, 0.1934853637294549, 3.9714443384547563e-07, 0.0010667390231878175, 0.13720194593308374, 0.00021120166321219572, 0.05744798118825605, 0.11755540027864601,
            3.1196254064458826e-07, 0.19156529076301418, 0.002757523723868695, 0.1343001289149663, 0.0043398395978009995, 0.008200632378292196, 0.061391119004475345, 0.00631403604965772,
            0.0021727341084057062
    };
    double[] expectedNormalizedDeviations = {
            2.9239682640094418, 3.1570296521773478, 3.4191226303853806, 3.8687230962137535, 2.084890164320013, 5.520465942997591, 2.2070016327281983, 2.930376814106019, 6.351415574164953,
            1.8155073852317374, 5.4383301265047255, 3.705644695119264, 1.996460290836387, 4.120874710389635, 2.394326303215475, 2.07267256897155, 5.4826863719918295, 1.8210106785462719,
            3.4389036642531274, 2.007174576382869, 3.303943569022889, 3.104685688098057, 2.3663444422150035, 3.1880527807027255, 3.5077530396806247
    };

    // Expected results generated via scipy.stats.norm
    @Test
    void testBoundedProbabilities() {
        for (int i = 0; i < nReferenceCols; i++) {
            Dataframe df1 = PregeneratedNormalData.generate(i);
            GaussianAnomalyDetection gad = new GaussianAnomalyDetection(df1);
            Prediction p = new SimplePrediction(
                    new PredictionInput(List.of(FeatureFactory.newNumericalFeature("0", 20))),
                    new PredictionOutput(List.of()));
            assertEquals(expected1StdProbs[i], gad.calculateBoundedProbability(p).get("0"), 1e-5);
            assertEquals(expected2StdProbs[i], gad.calculateBoundedProbability(p, 2).get("0"), 1e-5);
            assertEquals(expectedTenthStdProbs[i], gad.calculateBoundedProbability(p, 0.1).get("0"), 1e-5);
        }
    }

    // Expected results generated via scipy.stats.norm
    @Test
    void testNormalizedBoundedProbabilities() {
        for (int i = 0; i < nReferenceCols; i++) {
            Dataframe df1 = PregeneratedNormalData.generate(i);
            GaussianAnomalyDetection gad = new GaussianAnomalyDetection(df1);
            Prediction p = new SimplePrediction(
                    new PredictionInput(List.of(FeatureFactory.newNumericalFeature("0", 20))),
                    new PredictionOutput(List.of()));
            assertEquals(expected1StdNormalizedProbs[i], gad.calculateNormalizedBoundedProbability(p).get("0"), 1e-5);
            assertEquals(expected2StdNormalizedProbs[i], gad.calculateNormalizedBoundedProbability(p, 2).get("0"), 1e-5);
            assertEquals(expectedTenthStdNormalizedProbs[i], gad.calculateNormalizedBoundedProbability(p, 0.1).get("0"), 1e-5);
        }
    }

    // Expected results generated via scipy.stats.norm
    @Test
    void testNormalizedDeviations() {
        for (int i = 0; i < nReferenceCols; i++) {
            Dataframe df1 = PregeneratedNormalData.generate(i);
            GaussianAnomalyDetection gad = new GaussianAnomalyDetection(df1);
            Prediction p = new SimplePrediction(
                    new PredictionInput(List.of(FeatureFactory.newNumericalFeature("0", 20))),
                    new PredictionOutput(List.of()));
            assertEquals(expectedNormalizedDeviations[i], gad.calculateNormalizedDeviation(p).get("0"), 1e-5);
        }
    }
}
