from typing import Optional, Union, List

import pandas as pd
from aix360.algorithms.tslime import TSLimeExplainer as _TSLimeExplainer
from aix360.algorithms.tslime.surrogate import LinearSurrogateModel
from aix360.algorithms.tsutils.tsperturbers import TSPerturber

import logging

from trustyaiexternal.api.explainers import Explainer

# Set up logging
logging.basicConfig(level=logging.WARNING)
logging.warning(__file__)


class TSLimeExplainer(Explainer):
    """Time Series Lime Explainer"""

    def __init__(self, model_name: str,
                 model_version: str,
                 input_length: int,
                 n_perturbations: int = 2000,
                 relevant_history: int = None,
                 perturbers: List[Union[TSPerturber, dict]] = None,
                 local_interpretable_model: LinearSurrogateModel = None,
                 random_seed: int = None,
                 target: str = '0.0.0.0:8080'
                 ):
        super().__init__(model_name=model_name, model_version=model_version, target=target)
        self.input_length = input_length
        self.n_perturbations = n_perturbations
        self.relevant_history = relevant_history
        self.perturbers = perturbers
        self.local_interpretable_model = local_interpretable_model
        self.random_seed = random_seed

        self._explainer = _TSLimeExplainer(model=self._model.predict,
                                           input_length=self.input_length,
                                           n_perturbations=self.n_perturbations,
                                           relevant_history=self.relevant_history,
                                           perturbers=self.perturbers,
                                           local_interpretable_model=self.local_interpretable_model,
                                           random_seed=self.random_seed)
        logging.warning("Created LIME explainer")

    def explain(self, point: pd.DataFrame) -> dict:
        """Explains a single prediction.

        Args:
            point: A single prediction. Expected to be a time series with a timestamp column.
        """
        logging.warning("Explain")
        logging.warning(point)
        return self._explainer.explain_instance(ts=point)