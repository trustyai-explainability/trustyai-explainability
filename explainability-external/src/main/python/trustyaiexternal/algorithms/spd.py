import logging

import pandas as pd
from aif360.datasets import BinaryLabelDataset
from aif360.metrics import BinaryLabelDatasetMetric

from trustyaiexternal.api import Metric

logging.basicConfig(level=logging.WARNING)


class SPD(Metric):

    def __init__(self, protected_attribute_names, label_names, favorable_label, unfavorable_label,
                 privileged_protected_attributes, unprivileged_protected_attributes,
                 unprivileged_groups, privileged_groups):
        self.privileged_groups = [{k: v} for k, v in zip(protected_attribute_names, privileged_groups)]
        self.unprivileged_groups = [{k: v} for k, v in zip(protected_attribute_names, unprivileged_groups)]
        self.unprivileged_protected_attributes = unprivileged_protected_attributes
        self.privileged_protected_attributes = privileged_protected_attributes
        self.unfavorable_label = unfavorable_label
        self.favorable_label = favorable_label
        self.label_names = label_names
        self.protected_attribute_names = protected_attribute_names
        logging.warning(unprivileged_groups)
        logging.warning(privileged_groups)

    def calculate(self, df: pd.DataFrame) -> float:
        ds = BinaryLabelDataset(df=df,
                                protected_attribute_names=self.protected_attribute_names,
                                label_names=self.label_names,
                                favorable_label=self.favorable_label, unfavorable_label=self.unfavorable_label,
                                privileged_protected_attributes=self.privileged_protected_attributes,
                                unprivileged_protected_attributes=self.unprivileged_protected_attributes
                                )
        blds = BinaryLabelDatasetMetric(ds, unprivileged_groups=self.unprivileged_groups,
                                        privileged_groups=self.privileged_groups)
        return blds.statistical_parity_difference()
