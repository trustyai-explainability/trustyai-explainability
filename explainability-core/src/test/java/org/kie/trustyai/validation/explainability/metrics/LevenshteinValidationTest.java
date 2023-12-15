package org.kie.trustyai.validation.explainability.metrics;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.text.StringTokenizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.metrics.language.distance.Levenshtein;
import org.kie.trustyai.metrics.language.levenshtein.MatchErrorRate;
import org.kie.trustyai.metrics.language.levenshtein.WordErrorRate;
import org.kie.trustyai.metrics.language.levenshtein.WordInformationLost;
import org.kie.trustyai.metrics.language.levenshtein.WordInformationPreserved;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LevenshteinValidationTest {

    private final List<String> references = new ArrayList<>();
    private final List<String> hypotheses = new ArrayList<>();
    private final List<Double> expectedWER = new ArrayList<>();
    private final List<Double> expectedMER = new ArrayList<>();
    private final List<Double> expectedWIL = new ArrayList<>();
    private final List<Double> expectedWIP = new ArrayList<>();

    private final List<Integer> expectedLevenshteinChar = new ArrayList<>();
    private final List<Integer> expectedLevenshteinWord = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        final ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream("validation/metrics/language/glue.csv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            CSVParser parser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .parse(reader);

            for (CSVRecord record : parser) {
                references.add(record.get("reference"));
                hypotheses.add(record.get("hypothesis"));
                expectedWER.add(Double.parseDouble(record.get("wer")));
                expectedMER.add(Double.parseDouble(record.get("mer")));
                expectedWIL.add(Double.parseDouble(record.get("wil")));
                expectedWIP.add(Double.parseDouble(record.get("wip")));
                expectedLevenshteinChar.add(Integer.parseInt(record.get("levenshtein_char")));
                expectedLevenshteinWord.add(Integer.parseInt(record.get("levenshtein_word")));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("WER validation with GLUE data")
    void testWERValidationGLUE() {
        final int N = references.size();

        IntStream.range(0, N).forEach(i -> {
            final List<String> referenceTokens = new StringTokenizer(references.get(i)).getTokenList();
            final List<String> hypothesisTokens = new StringTokenizer(hypotheses.get(i)).getTokenList();
            final double wer = new WordErrorRate().calculate(referenceTokens, hypothesisTokens).getValue();
            assertEquals(expectedWER.get(i), wer, 1e-5, "Expect WER of " + expectedWER.get(i) + " got " + wer + ", for " + references.get(i) + "/" + hypotheses.get(i));
        });
    }

    @Test
    @DisplayName("MER validation with GLUE data")
    void testMERValidationGLUE() {
        final int N = references.size();

        IntStream.range(0, N).forEach(i -> {
            final List<String> referenceTokens = new StringTokenizer(references.get(i)).getTokenList();
            final List<String> hypothesisTokens = new StringTokenizer(hypotheses.get(i)).getTokenList();
            final double mer = new MatchErrorRate().calculate(referenceTokens, hypothesisTokens).getValue();
            assertEquals(expectedMER.get(i), mer, 1e-5, "Expect MER of " + expectedMER.get(i) + " got " + mer + ", for " + references.get(i) + "/" + hypotheses.get(i));
        });
    }

    @Test
    @DisplayName("WIP validation with GLUE data")
    void testWIPValidationGLUE() {
        final int N = references.size();

        IntStream.range(0, N).forEach(i -> {
            final List<String> referenceTokens = new StringTokenizer(references.get(i)).getTokenList();
            final List<String> hypothesisTokens = new StringTokenizer(hypotheses.get(i)).getTokenList();
            final double wip = new WordInformationPreserved().calculate(referenceTokens, hypothesisTokens).getValue();
            assertEquals(expectedWIP.get(i), wip, 1e-5, "Expect WIP of " + expectedWIP.get(i) + " got " + wip + ", for " + references.get(i) + "/" + hypotheses.get(i));
        });
    }

    @Test
    @DisplayName("WIL validation with GLUE data")
    void testWILValidationGLUE() {
        final int N = references.size();

        IntStream.range(0, N).forEach(i -> {
            final List<String> referenceTokens = new StringTokenizer(references.get(i)).getTokenList();
            final List<String> hypothesisTokens = new StringTokenizer(hypotheses.get(i)).getTokenList();
            final double wil = new WordInformationLost().calculate(referenceTokens, hypothesisTokens).getValue();
            assertEquals(expectedWIL.get(i), wil, 1e-5, "Expect WIL of " + expectedWIL.get(i) + " got " + wil + ", for " + references.get(i) + "/" + hypotheses.get(i));
        });
    }

    @Test
    @DisplayName("Levenshtein-Word validation with GLUE data")
    void testLevenshteinValidationGLUE() {
        final int N = references.size();

        IntStream.range(0, N).forEach(i -> {
            final List<String> referenceTokens = new StringTokenizer(references.get(i)).getTokenList();
            final List<String> hypothesisTokens = new StringTokenizer(hypotheses.get(i)).getTokenList();
            final int wil = Levenshtein.calculateToken(referenceTokens, hypothesisTokens).getDistance();
            assertEquals(expectedLevenshteinWord.get(i), wil, "Expect Levenshtein-Word distance of " + expectedLevenshteinWord.get(i) + " got " + wil + ", for " + references.get(i) + "/" + hypotheses.get(i));
        });
    }

}
