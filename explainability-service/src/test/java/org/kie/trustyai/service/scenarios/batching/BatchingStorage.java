package org.kie.trustyai.service.scenarios.batching;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class BatchingStorage {

    private int observations = 20;
    private List<String> names = List.of("x-1", "x-2", "x-3");
    private boolean withHeader = true;

    public void setWithHeader(boolean withHeader) {
        this.withHeader = withHeader;
    }

    public String generateLines(List<String> featureNames, int observations) {
        final int nFeatures = featureNames.size();
        final StringBuilder builder = new StringBuilder();
        // add header
        if (withHeader) {
            builder.append(String.join(",", featureNames.stream().map(name -> "\"" + name + "\"").collect(Collectors.toList()))).append("\n");
        }
        for (int i = 0; i < observations; i++) {
            final double counter = (double) i;
            builder.append(String.join(",", new Random().doubles(nFeatures).boxed().map(v -> v + counter).map(String::valueOf).collect(Collectors.toList()))).append("\n");
        }
        return builder.toString();
    }

    public InputStream getDataStream() throws FileNotFoundException {
        final String lines = generateLines(names, observations);
        return new ByteArrayInputStream(lines.getBytes());
    }

    public void setObservations(int observations) {
        this.observations = observations;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

}
