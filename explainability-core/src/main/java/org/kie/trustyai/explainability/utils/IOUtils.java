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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.Output;
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
     * @param heading: The table name
     * @param columns: A list of columns, each a list of column values
     * @param separators: A list of strings to be used as the column separators
     *
     * @return Pair<String, Integer> of the formatted table and its max line width
     */
    public static Pair<String, Integer> generateTable(String heading, List<List<String>> columns, List<String> separators) {
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

    /**
     * Generate a table comparing two unequal feature lists
     *
     * @param featureList1 the first list of features
     * @param featureList2 the second list of features
     * @param featureList1Name the name of the first feature list
     * @param featureList1Name the name of the second feature list
     *
     * @return Pair<String, Integer> of the formatted table and its max line width
     */
    public static Pair<String, Integer> featureListComparison(List<Feature> featureList1, List<Feature> featureList2, String featureList1Name, String featureList2Name, boolean considerValues) {

        int f1Size = featureList1.size();
        int f2Size = featureList2.size();
        int maxLen = Math.max(f1Size, f2Size);

        List<String> colIndex = new ArrayList<>(List.of("Index"));
        List<String> colFeatures1 = new ArrayList<>(List.of(featureList1Name));
        List<String> colFeatures2 = new ArrayList<>(List.of(featureList2Name));
        for (int i = 0; i < maxLen; i++) {
            String f1Msg;
            String f2Msg;

            if (i >= f1Size && i < f2Size) {
                f1Msg = "n/a";
                f2Msg = featureList2.get(i).toString();
            } else if (i < f1Size && i >= f2Size) {
                f1Msg = featureList1.get(i).toString();
                f2Msg = "n/a";
            } else {
                List<String> f1Msgs = new ArrayList<>();
                List<String> f2Msgs = new ArrayList<>();
                Feature f1 = featureList1.get(i);
                Feature f2 = featureList2.get(i);
                if (!f1.getName().equals(f2.getName())) {
                    f1Msgs.add("Name=" + f1.getName());
                    f2Msgs.add("Name=" + f2.getName());
                }
                if (!f1.getType().equals(f2.getType())) {
                    f1Msgs.add("TrustyAI Type=" + f1.getType());
                    f2Msgs.add("TrustyAI Type=" + f2.getType());
                }
                if (!f1.getValue().getUnderlyingObject().getClass().equals(f2.getValue().getUnderlyingObject().getClass())) {
                    f1Msgs.add("Class=" + f1.getValue().getUnderlyingObject().getClass().getSimpleName());
                    f2Msgs.add("Class=" + f2.getValue().getUnderlyingObject().getClass().getSimpleName());
                }
                if (considerValues && !f1.getValue().equals(f2.getValue())) {
                    f1Msgs.add("Value=" + f1.getValue());
                    f2Msgs.add("Value=" + f2.getValue());
                }
                f1Msg = String.join(", ", f1Msgs);
                f2Msg = String.join(", ", f2Msgs);
            }

            if (!f1Msg.isEmpty() || !f1Msg.isEmpty()) {
                colIndex.add(String.valueOf(i));
                colFeatures1.add(f1Msg);
                colFeatures2.add(f2Msg);
            }
        }
        return generateTable("Feature Mismatches", List.of(colIndex, colFeatures1, colFeatures2), List.of(" |", " !="));
    }

    /**
     * Generate a table comparing two unequal output lists
     *
     * @param outputList1 the first list of outputs
     * @param outputList2 the second list of outputs
     * @param outputList1Name the name of the first output list
     * @param outputList1Name the name of the second output list
     *
     * @return Pair<String, Integer> of the formatted table and its max line width
     */
    public static Pair<String, Integer> outputListComparison(List<Output> outputList1, List<Output> outputList2, String outputList1Name, String outputList2Name, boolean considerValues) {

        int o1Size = outputList1.size();
        int o2Size = outputList2.size();
        int maxLen = Math.max(o1Size, o2Size);

        List<String> colIndex = new ArrayList<>(List.of("Index"));
        List<String> colOutputs1 = new ArrayList<>(List.of(outputList1Name));
        List<String> colOutputs2 = new ArrayList<>(List.of(outputList2Name));
        for (int i = 0; i < maxLen; i++) {
            String o1Msg;
            String o2Msg;

            if (i >= o1Size && i < o2Size) {
                o1Msg = "n/a";
                o2Msg = outputList2.get(i).toString();
            } else if (i < o1Size && i >= o2Size) {
                o1Msg = outputList1.get(i).toString();
                o2Msg = "n/a";
            } else {
                List<String> o1Msgs = new ArrayList<>();
                List<String> o2Msgs = new ArrayList<>();
                Output o1 = outputList1.get(i);
                Output o2 = outputList2.get(i);
                if (!o1.getName().equals(o2.getName())) {
                    o1Msgs.add("Name=" + o1.getName());
                    o2Msgs.add("Name=" + o2.getName());
                }
                if (!o1.getType().equals(o2.getType())) {
                    o1Msgs.add("TrustyAI Type=" + o1.getType());
                    o2Msgs.add("TrustyAI Type=" + o2.getType());
                }
                if (!o1.getValue().getUnderlyingObject().getClass().equals(o2.getValue().getUnderlyingObject().getClass())) {
                    o1Msgs.add("Class=" + o1.getValue().getUnderlyingObject().getClass().getSimpleName());
                    o2Msgs.add("Class=" + o2.getValue().getUnderlyingObject().getClass().getSimpleName());
                }
                if (considerValues && !o1.getValue().equals(o2.getValue())) {
                    o1Msgs.add("Value=" + o1.getValue());
                    o2Msgs.add("Value=" + o2.getValue());
                }
                o1Msg = String.join(", ", o1Msgs);
                o2Msg = String.join(", ", o2Msgs);
            }

            if (!o1Msg.isEmpty() || !o1Msg.isEmpty()) {
                colIndex.add(String.valueOf(i));
                colOutputs1.add(o1Msg);
                colOutputs2.add(o2Msg);
            }
        }
        return generateTable("Output Mismatches", List.of(colIndex, colOutputs1, colOutputs2), List.of(" |", " !="));
    }
}
