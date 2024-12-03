# TrustyAI Explainability Core
TrustyAI's explainability-core is a Java library for explainable and transparent AI, 
containing XAI algorithms, drift metrics, fairness metrics, and language model accuracy metrics.

A non-exhaustive list of provided algorithms is:

---
## 🔬 Explainability 🔬
### Local
* [LIME](https://arxiv.org/abs/1602.04938)
* [Constraint-based counterfactual search](https://arxiv.org/abs/2104.12717)
* [SHAP](https://arxiv.org/abs/1705.07874)

### Global
* [LIME](https://arxiv.org/abs/1602.04938)
* [Partial Dependency Plot (PDP)](https://www.jstor.org/stable/2699986)

---
## ⚖️ Fairness ⚖️
### Group
* [Disparate Impact Ratio (DIR)](src/main/java/org/kie/trustyai/metrics/fairness/group/DisparateImpactRatio.java)
* [Average Odds Difference](src/main/java/org/kie/trustyai/metrics/fairness/group/GroupAverageOddsDifference.java)
* [Average Predictive Value Difference](src/main/java/org/kie/trustyai/metrics/fairness/group/GroupAveragePredictiveValueDifference.java)
* [Statistical Parity Difference (SPD)](src/main/java/org/kie/trustyai/metrics/fairness/group/GroupStatisticalParityDifference.java)

### Individual
* [Individual Consistency](src/main/java/org/kie/trustyai/metrics/fairness/individual/IndividualConsistency.java)

---
## 📈 Drift 📉
* [Fourier Maximum Mean Discrepancy (FourierMMD)](src/main/java/org/kie/trustyai/metrics/drift/fouriermmd)
* [Jensen-Shannon](src/main/java/org/kie/trustyai/metrics/drift/jensenshannon)
* [Approximate Kolmogorov–Smirnov Test](org/kie/trustyai/metrics/drift/kstest/ApproxKSTest.java)
* [Kolmogorov–Smirnov Test (KS-Test)](src/main/java/org/kie/trustyai/metrics/drift/kstest)
* [Meanshift](src/main/java/org/kie/trustyai/metrics/drift/meanshift)

---
## ⁉️ Anomaly ⁉️
* [Gaussian Anomaly Detection](src/main/java/org/kie/trustyai/metrics/anomaly/GaussianAnomalyDetection.java)

---
## 📝 Language 📝 
* [BLEU](src/main/java/org/kie/trustyai/metrics/language/bleu)
* [Fuzzy Match](src/main/java/org/kie/trustyai/metrics/language/match)
* [Levenshtein](src/main/java/org/kie/trustyai/metrics/language/levenshtein)
* [ROUGE](src/main/java/org/kie/trustyai/metrics/language/rouge)
