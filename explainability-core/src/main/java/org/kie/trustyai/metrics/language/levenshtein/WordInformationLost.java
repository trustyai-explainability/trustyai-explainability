package org.kie.trustyai.metrics.language.levenshtein;

import java.util.List;

/**
 * Word Information Lost (WIL)
 *
 * Morris, Andrew and Maier, Viktoria and Green, Phil (2004), "From WER and RIL to MER and WIL: improved evaluation measures for connected speech recognition."
 */
public class WordInformationLost extends WordInformationPreserved {

    @Override
    public ErrorRateResult calculate(List<String> tokenizedReference, List<String> tokenizedHypothesis) {

        final ErrorRateResult wip = super.calculate(tokenizedReference, tokenizedHypothesis);
        return new ErrorRateResult(1.0 - wip.getValue(), wip.getAlignmentCounters());
    }

}
