from typing import Optional, Union

import pandas as pd
from aix360.algorithms.tsice import TSICEExplainer as _TSICEExplainer
from aix360.algorithms.tsutils.tsperturbers import TSPerturber

import logging

from trustyaiexternal.api.explainers import Explainer

# Set up logging
logging.basicConfig(level=logging.WARNING)
logging.warning(__file__)


class TSICEExplainer(Explainer):
    """Time Series ICE Explainer"""

    def __init__(self, model_name: str,
                 model_version: str,
                 input_length: int,
                 forecast_lookahead: int,
                 n_variables: int = 1,
                 n_exogs: int = 0,
                 n_perturbations: int = 25,
                 features_to_analyze: Optional[list[str]] = None,
                 perturbers: Optional[list[Union[TSPerturber, dict]]] = None,
                 explanation_window_start: Optional[int] = None,
                 explanation_window_length: int = 10,
                 target: str = '0.0.0.0:8080'
                 ):
        super().__init__(model_name=model_name, model_version=model_version, target=target)
        self.input_length = input_length
        self.forecast_lookahead = forecast_lookahead
        self.n_variables = n_variables
        self.n_exogs = n_exogs
        self.n_perturbations = n_perturbations
        self.features_to_analyze = features_to_analyze
        self.perturbers = perturbers
        self.explanation_window_start = explanation_window_start
        self.explanation_window_length = explanation_window_length
        self._explainer = _TSICEExplainer(forecaster=self._model.predict,
                                          input_length=self.input_length,
                                          forecast_lookahead=self.forecast_lookahead,
                                          n_variables=self.n_variables,
                                          n_exogs=self.n_exogs,
                                          n_perturbations=self.n_perturbations,
                                          features_to_analyze=self.features_to_analyze,
                                          perturbers=self.perturbers,
                                          explanation_window_start=self.explanation_window_start,
                                          explanation_window_length=self.explanation_window_length)
        logging.warning("Create explainer")

    def explain(self, point: pd.DataFrame) -> dict:
        """Explains a single prediction.

        Args:
            point: A single prediction. Expected to be a time series with a timestamp column.
        """
        logging.warning("Explain")
        logging.warning(point)
        return self._explainer.explain_instance(ts=point)
