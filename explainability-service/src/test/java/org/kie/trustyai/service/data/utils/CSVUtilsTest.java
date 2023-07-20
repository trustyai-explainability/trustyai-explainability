package org.kie.trustyai.service.data.utils;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CSVUtilsTest {

    @Test
    void testParseRaw() throws IOException {
        String inputString = "false,1,2022-04-01\ntrue,2,1970-12-31";
        List<List<Value>> lists = CSVUtils.parseRaw(inputString);
        assertNotNull(lists);
        assertEquals(2, lists.size());
        List<Value> firstRow = lists.get(0);
        List<Value> secondRow = lists.get(1);
        assertNotNull(firstRow);
        assertEquals(3, firstRow.size());
        assertEquals(new Value("false"), firstRow.get(0));
        assertEquals(new Value("1"), firstRow.get(1));
        assertEquals(new Value("2022-04-01"), firstRow.get(2));
        assertEquals(new Value("true"), secondRow.get(0));
        assertEquals(new Value("2"), secondRow.get(1));
        assertEquals(new Value("1970-12-31"), secondRow.get(2));
    }
}