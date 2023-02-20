package org.kie.trustyai.service.payloads.service;

import java.util.ArrayList;
import java.util.List;

public class DataMetadata {
    public int observations = 0;
    public List<SchemaItem> inputs = new ArrayList<>();
    public List<SchemaItem> outputs = new ArrayList<>();
}
