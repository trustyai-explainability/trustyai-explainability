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
}
