package org.kie.trustyai.metrics.language.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

public class SentenceUtils {
    private SentenceUtils() {
    }

    public static String[] splitSentences(String text) {
        try (InputStream modelIn = new FileInputStream("/opennlp/models/en-token.bin")) {
            SentenceDetectorME detector = new SentenceDetectorME(new SentenceModel(modelIn));
            return detector.sentDetect(text);
        } catch (IOException e) {
            String[] res = text.split("[.,!?:;]+\\s*");
            return res;
        }
    }
}
