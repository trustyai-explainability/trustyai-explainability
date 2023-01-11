# Changelog

All notable changes to this project will be documented in this file.

## [unreleased]


### Bug Fixes


- Fix tests ([#684](https://github.com/trustyai-explainability/trustyai-explainability/pull/684))


- Fixed missing runCounterfactuals in CFTest

- Fixed bug attaching solverconfig to cfconfig

- Fixed infinite look in cf generator ([#33](https://github.com/trustyai-explainability/trustyai-explainability/pull/33))




### [FAI-363](https://issues.redhat.com/browse/FAI-363)


- Shap Kernel Explainer ([#874](https://github.com/trustyai-explainability/trustyai-explainability/pull/874))


### [FAI-439](https://issues.redhat.com/browse/FAI-439)


- CF: TrustyService handling of CF resultsets ([#799](https://github.com/trustyai-explainability/trustyai-explainability/pull/799))


### [FAI-456](https://issues.redhat.com/browse/FAI-456)


- CF: ExplainabilityService handling of CF requests ([#752](https://github.com/trustyai-explainability/trustyai-explainability/pull/752))


### [FAI-464](https://issues.redhat.com/browse/FAI-464)


- MatrixUtils Extensions ([#793](https://github.com/trustyai-explainability/trustyai-explainability/pull/793))


### [FAI-473](https://issues.redhat.com/browse/FAI-473)


- CounterfactualExplainer: Add CF support for composite features ([#1236](https://github.com/trustyai-explainability/trustyai-explainability/pull/1236))


### [FAI-483](https://issues.redhat.com/browse/FAI-483)


- Use constant values for constrained features in CF search ([#822](https://github.com/trustyai-explainability/trustyai-explainability/pull/822))


### [FAI-488](https://issues.redhat.com/browse/FAI-488)


- Counterfactual results: Limit number returned? ([#888](https://github.com/trustyai-explainability/trustyai-explainability/pull/888))


### [FAI-500](https://issues.redhat.com/browse/FAI-500)


- Duplicate counterfactual result id created for final result ([#887](https://github.com/trustyai-explainability/trustyai-explainability/pull/887))


### [FAI-501](https://issues.redhat.com/browse/FAI-501)


- Invalid CF results not being filtered out from consumers ([#873](https://github.com/trustyai-explainability/trustyai-explainability/pull/873))


### [FAI-502](https://issues.redhat.com/browse/FAI-502)


- Implement outcome match threshold for numerical features ([#858](https://github.com/trustyai-explainability/trustyai-explainability/pull/858))


### [FAI-512](https://issues.redhat.com/browse/FAI-512)


- Invalid size comparison between CF goals and model outputs ([#884](https://github.com/trustyai-explainability/trustyai-explainability/pull/884))


### [FAI-515](https://issues.redhat.com/browse/FAI-515)


- Implement Gower distance as the score metric for CFs ([#911](https://github.com/trustyai-explainability/trustyai-explainability/pull/911))


### [FAI-534](https://issues.redhat.com/browse/FAI-534)


- SHAP Error Bounds ([#966](https://github.com/trustyai-explainability/trustyai-explainability/pull/966))


### [FAI-538](https://issues.redhat.com/browse/FAI-538)


- Use configuration class instead of Builder for CFs ([#967](https://github.com/trustyai-explainability/trustyai-explainability/pull/967))


### [FAI-547](https://issues.redhat.com/browse/FAI-547)


- Fix flaky test TrafficViolationDmnLimeExplainerTest.testExplanationWeightedStabilityWithOptimization ([#988](https://github.com/trustyai-explainability/trustyai-explainability/pull/988))


### [FAI-548](https://issues.redhat.com/browse/FAI-548)


- Return ShapResults from SHAP that contains model null along with shap values ([#989](https://github.com/trustyai-explainability/trustyai-explainability/pull/989))


### [FAI-551](https://issues.redhat.com/browse/FAI-551)


- CF UI - Configuration of progress bar duration ([#1020](https://github.com/trustyai-explainability/trustyai-explainability/pull/1020))


### [FAI-556](https://issues.redhat.com/browse/FAI-556)


- Increase CF test coverage by including DMN models ([#1042](https://github.com/trustyai-explainability/trustyai-explainability/pull/1042))


### [FAI-561](https://issues.redhat.com/browse/FAI-561)


- Add support in CF for remaining ordinal feature types ([#1229](https://github.com/trustyai-explainability/trustyai-explainability/pull/1229))


### [FAI-595](https://issues.redhat.com/browse/FAI-595)


- Support for text features in CF goals ([#1034](https://github.com/trustyai-explainability/trustyai-explainability/pull/1034))


### [FAI-615](https://issues.redhat.com/browse/FAI-615)


- Handling of null values in CF score calculation ([#1055](https://github.com/trustyai-explainability/trustyai-explainability/pull/1055))


### [FAI-621](https://issues.redhat.com/browse/FAI-621)


- SHAP batching options ([#1264](https://github.com/trustyai-explainability/trustyai-explainability/pull/1264))


### [FAI-660](https://issues.redhat.com/browse/FAI-660)


- LARSPath and LassoLarsIC for future SHAP regularization ([#1098](https://github.com/trustyai-explainability/trustyai-explainability/pull/1098))


### [FAI-661](https://issues.redhat.com/browse/FAI-661)


- Move SHAP to Apache Commons MatrixUtils ([#1192](https://github.com/trustyai-explainability/trustyai-explainability/pull/1192))


### [FAI-663](https://issues.redhat.com/browse/FAI-663)


- Add initial support for remaining input features in CF as constrained ([#1091](https://github.com/trustyai-explainability/trustyai-explainability/pull/1091))


### [FAI-668](https://issues.redhat.com/browse/FAI-668)


- Explainability: Native build is broken ([#1143](https://github.com/trustyai-explainability/trustyai-explainability/pull/1143))


### [FAI-671](https://issues.redhat.com/browse/FAI-671)


- Convert WLR to use Apache MatrixUtils ([#1181](https://github.com/trustyai-explainability/trustyai-explainability/pull/1181))


### [FAI-692](https://issues.redhat.com/browse/FAI-692)


- Integration of regularizers into SHAP ([#1209](https://github.com/trustyai-explainability/trustyai-explainability/pull/1209))


### [FAI-701](https://issues.redhat.com/browse/FAI-701)


- Fix edge case in CF primary soft score calculation ([#1208](https://github.com/trustyai-explainability/trustyai-explainability/pull/1208))


### [FAI-721](https://issues.redhat.com/browse/FAI-721)


- Scaled back the SHAP tests causing heap space issues #1240


### [FAI-725](https://issues.redhat.com/browse/FAI-725)


- Fix SHAP subset sampler out-of-range bug ([#1249](https://github.com/trustyai-explainability/trustyai-explainability/pull/1249))


### [FAI-739](https://issues.redhat.com/browse/FAI-739)


- Add per-feature filtering to Datasets ([#1266](https://github.com/trustyai-explainability/trustyai-explainability/pull/1266))


### [FAI-742](https://issues.redhat.com/browse/FAI-742)


- Add counterfactual entity factory case for Long feature values ([#1270](https://github.com/trustyai-explainability/trustyai-explainability/pull/1270))


### [FAI-745](https://issues.redhat.com/browse/FAI-745)


- SHAPResults toString function ([#1275](https://github.com/trustyai-explainability/trustyai-explainability/pull/1275))


### [FAI-751](https://issues.redhat.com/browse/FAI-751)


- Background generators first commit. CF generator is failing tests


### [FAI-756](https://issues.redhat.com/browse/FAI-756)


- Optimize test suite for faster builds


### [FAI-796](https://issues.redhat.com/browse/FAI-796)


- Added toString to all FeatureDomains and Explainers


### [FAI-855](https://issues.redhat.com/browse/FAI-855)


- Move arrow-converters into exp-core ([#34](https://github.com/trustyai-explainability/trustyai-explainability/pull/34))


### [FAI-862](https://issues.redhat.com/browse/FAI-862)


- Update Kogito and OptaPlanner versions ([#38](https://github.com/trustyai-explainability/trustyai-explainability/pull/38))


### [FAI-870](https://issues.redhat.com/browse/FAI-870)


- Swapped DummyModels to use more predictable models ([#36](https://github.com/trustyai-explainability/trustyai-explainability/pull/36))


### [FAI-873](https://issues.redhat.com/browse/FAI-873)


- Prepare for JBoss deployment ([#40](https://github.com/trustyai-explainability/trustyai-explainability/pull/40))


### [FAI-874](https://issues.redhat.com/browse/FAI-874)


- Add argument for Java 17 JPMS Arrow tests exception ([#43](https://github.com/trustyai-explainability/trustyai-explainability/pull/43))


### [FAI-884](https://issues.redhat.com/browse/FAI-884)


- Added kPerGoal flag to chained counterfactual generation ([#47](https://github.com/trustyai-explainability/trustyai-explainability/pull/47))


### [FAI-890](https://issues.redhat.com/browse/FAI-890)


- Add initial Dataframe implementation ([#48](https://github.com/trustyai-explainability/trustyai-explainability/pull/48))


### [FAI-902](https://issues.redhat.com/browse/FAI-902)


- Initial implementation of data-based fairness metrics ([#51](https://github.com/trustyai-explainability/trustyai-explainability/pull/51))


### [KOGITO-2914](https://issues.redhat.com/browse/KOGITO-2914)


- Connect Trusty service and Explainability service ([#410](https://github.com/trustyai-explainability/trustyai-explainability/pull/410))


### Refactor


- Refactored ShapResults to return dict of outputname, saliency like LIME



### Security


- Weighted random sampler ([#755](https://github.com/trustyai-explainability/trustyai-explainability/pull/755))


### Testing


- Tested

- Tested shapresultstest

- Tested after removing contamination


