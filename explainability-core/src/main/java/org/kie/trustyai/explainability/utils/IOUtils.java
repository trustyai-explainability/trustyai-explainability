/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.trustyai.explainability.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureImportance;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Saliency;
import org.kie.trustyai.explainability.model.Type;

public class IOUtils {
    // === ROUMDING HELPERS ============================================================================================
    /**
     * Round a given double to N decimal places
     * 
     * @param d: Double to round
     * @param decimalPlaces: decimal places to round to
     *
     * @return A string representation of the rounded double
     */
    public static String roundedString(double d, int decimalPlaces) {
        return String.format(String.format("%%.%df", decimalPlaces), d);
    }

    /**
     * Round a given Feature to N decimal places, if applicable
     * 
     * @param f: Feature to round
     * @param decimalPlaces: decimal places to round to
     *
     * @return A string representation of the rounded feature
     */
    public static String roundedString(Feature f, int decimalPlaces) {
        if (f.getType().equals(Type.NUMBER)) {
            return roundedString(f.getValue().asNumber(), decimalPlaces);
        } else {
            return f.getValue().getUnderlyingObject().toString();
        }
    }

    /**
     * Round a given output to N decimal places, if applicable
     * 
     * @param o: Output to round
     * @param decimalPlaces: decimal places to round to
     *
     * @return A string representation of the rounded output
     */
    public static String roundedString(Output o, int decimalPlaces) {
        if (o.getType().equals(Type.NUMBER)) {
            return roundedString(o.getValue().asNumber(), decimalPlaces);
        } else {
            return o.getValue().getUnderlyingObject().toString();
        }
    }

    // === TABLE GENERATORS ============================================================================================
    /**
     * Generate a well-formatted table
     * 
     * @param heading: The table name
     * @param columns: A list of columns, each a list of column values
     *
     * @return Pair<String, Integer> of the formatted table and its max line width
     */
    public static Pair<String, Integer> generateTable(String heading, List<List<String>> columns) {
        List<String> separators = IntStream.range(0, columns.size() - 1).mapToObj(i -> " |").collect(Collectors.toList());
        return generateTable(List.of(heading), List.of(0), List.of(), columns, separators);
    }

    /**
     * Generate a well-formatted table
     * 
     * @param headings: The table names (one for each headingPosition)
     * @param headingPositions: A list of integers, describing which column indeces represent column names
     *        For example, headingPositions = [0, 3], columns = [[Column Name 1, value, value, Column Name 2, value, etc]]
     * @param lineSeparatorPositions: A list of integers, describing after which rows to place table seperator
     * @param columns: A list of columns, each a list of column values
     * @param separators: A list of strings to be used as the column separators
     *
     * @return Pair<String, Integer> of the formatted table and its max line width
     */
    public static Pair<String, Integer> generateTable(List<String> headings, List<Integer> headingPositions,
            List<Integer> lineSeparatorPositions, List<List<String>> columns,
            List<String> separators) {
        int[] largestCellWidths = new int[columns.size() + separators.size()];
        StringBuilder formatter = new StringBuilder();
        int spacers = 0;
        for (int i = 0; i < columns.size(); i++) {
            int largestCellWidth = columns.get(i).stream().mapToInt(String::length).max().getAsInt() + 1;
            largestCellWidths[i] = largestCellWidth;
            formatter.append(String.format("%%%ds", largestCellWidth));
            if (i != columns.size() - 1) {
                formatter.append(separators.get(i));
                spacers += separators.get(i).length();
            }
        }

        formatter.append("%n");
        String formatString = formatter.toString();
        int totalWidth = Arrays.stream(largestCellWidths).sum() + spacers;

        StringBuilder table = new StringBuilder();
        int headingIdx = 0;
        for (int i = 0; i < columns.get(0).size(); i++) {
            int finalI = i;
            if (headingPositions.contains(i)) {
                // print header
                String heading = headings.get(headingIdx);
                table.append(
                        String.format(
                                "=== %s %s%n",
                                heading,
                                StringUtils.repeat("=", Math.max(0, totalWidth - heading.length() - 5))));

                // print row names
                table.append(String.format(formatString, columns.stream().map(cs -> cs.get(finalI)).toArray()));
                table.append(StringUtils.repeat("-", totalWidth)).append(String.format("%n"));
                headingIdx += 1;
            } else {
                // print cell values
                table.append(String.format(formatString, columns.stream().map(cs -> cs.get(finalI)).toArray()));
            }

            // print header
            if (headingPositions.contains(i + 1)) {
                table.append(StringUtils.repeat("=", totalWidth)).append(String.format("%n"));
            }
            if (lineSeparatorPositions.contains(i + 1)) {
                table.append(StringUtils.repeat("-", totalWidth)).append(String.format("%n"));
            }
            if (i == columns.get(0).size() - 1) {
                table.append(StringUtils.repeat("=", totalWidth));
            }
        }
        return new Pair<>(table.toString(), totalWidth);
    }

    // === LIME I/O ====================================================================================================
    /**
     * Represent LIME results as a string
     *
     * @return LIME results string
     */
    public static String LimeResultsAsTable(Map<String, Saliency> results) {
        return LimeResultsAsTable(results, 3);
    }

    /**
     * Represent LIME results as a string
     * 
     * @param decimalPlaces The decimal places to round all numeric values in the table to
     *
     * @return LIME results string
     */
    public static String LimeResultsAsTable(Map<String, Saliency> results, int decimalPlaces) {
        List<String> featureNames = new ArrayList<>();
        List<String> featureValues = new ArrayList<>();
        List<String> limeScores = new ArrayList<>();

        List<String> headers = new ArrayList<>();
        List<Integer> headerPositions = new ArrayList<>();
        List<Integer> lineSeparatorPositions = new ArrayList<>();
        int lineIDX = 0;

        List<Map.Entry<String, Saliency>> entries = results.entrySet().stream().collect(Collectors.toList());

        for (int s = 0; s < entries.size(); s++) {
            Saliency saliency = entries.get(s).getValue();
            List<FeatureImportance> pfis = saliency.getPerFeatureImportance();
            headers.add(saliency.getOutput().getName() + " LIME Scores");
            headerPositions.add(lineIDX);

            featureNames.add("Feature");
            featureValues.add("Value");
            limeScores.add("Saliency ");
            lineIDX++;

            for (int i = 0; i < pfis.size(); i++) {
                featureNames.add(pfis.get(i).getFeature().getName() + " = ");
                featureValues.add(IOUtils.roundedString(pfis.get(i).getFeature(), decimalPlaces));
                limeScores.add(IOUtils.roundedString(pfis.get(i).getScore(), decimalPlaces));
                lineIDX++;
            }

            lineSeparatorPositions.add(lineIDX);
            featureNames.add("");
            featureValues.add("Prediction");
            limeScores.add(IOUtils.roundedString(saliency.getOutput(), decimalPlaces));
            lineIDX++;
        }
        return IOUtils.generateTable(
                headers,
                headerPositions,
                lineSeparatorPositions,
                List.of(featureNames, featureValues, limeScores),
                List.of("", " | ")).getFirst();
    }
}
