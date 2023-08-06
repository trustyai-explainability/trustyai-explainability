import numpy as np
import pandas as pd
from aif360.datasets import BinaryLabelDataset
from aif360.metrics import BinaryLabelDatasetMetric

from trustyaiexternal.api import Metric


class DIR(Metric):
    def calculate(self, df: pd.DataFrame) -> float:
        ds = BinaryLabelDataset(df=df,
                                protected_attribute_names=["sex"],
                                label_names=["income"],
                                favorable_label=1., unfavorable_label=0.,
                                privileged_protected_attributes=[np.array([1.0])],
                                unprivileged_protected_attributes=[np.array([0.0])]
                                )
        blds = BinaryLabelDatasetMetric(ds, unprivileged_groups=[{'sex': 0.0}],
                                        privileged_groups=[{'sex': 1.0}])
        return blds.disparate_impact()
