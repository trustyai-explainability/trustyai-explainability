package org.kie.trustyai.service.payloads.requests.explainers.tslime;

/**
 * Parameters for the TSSaliency explainer.
 */
public class TSLimeParameters {

    private int inputLength;
    private int nPerturbations = 10;
    private int relevantHistory;

    public int getInputLength() {
        return inputLength;
    }

    public void setInputLength(int inputLength) {
        this.inputLength = inputLength;
    }

    public int getnPerturbations() {
        return nPerturbations;
    }

    public void setnPerturbations(int nPerturbations) {
        this.nPerturbations = nPerturbations;
    }

    public int getRelevantHistory() {
        return relevantHistory;
    }
}
