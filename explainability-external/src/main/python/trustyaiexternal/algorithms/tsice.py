from ast import List
from typing import Callable, Optional, Union
import pandas as pd
from aix360.algorithms.tsice import TSICEExplainer as _TSICEExplainer
from aix360.algorithms.tsutils.tsperturbers import TSPerturber

from trustyaiexternal.api import Explainer

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
                 target: str = '0.0.0.0:8081',
                 ):
        super().__init__(model_name, model_version, target)
        self.input_length = input_length
        self.forecast_lookahead = forecast_lookahead
        self.n_variables = n_variables
        self.n_exogs = n_exogs
        self.n_perturbations = n_perturbations
        self.features_to_analyze = features_to_analyze
        self.perturbers = perturbers
        self.explanation_window_start = explanation_window_start
        self.explanation_window_length = explanation_window_length
        

    def explain(self, point: pd.DataFrame, model = None) -> dict:
        """Explains a single prediction.
        
        If model is None, the model passed to the constructor is used as a remote gRPC rmodel.
        """
        if model is None:
            model = self._model
        explainer = _TSICEExplainer(forecaster=model.predict,
                            input_length=self.input_length,
                            forecast_lookahead=self.forecast_lookahead,
                            n_variables=self.n_variables,
                            n_exogs=self.n_exogs,
                            n_perturbations=self.n_perturbations,
                            features_to_analyze=self.features_to_analyze,
                            perturbers=self.perturbers,
                            explanation_window_start=self.explanation_window_start,
                            explanation_window_length=self.explanation_window_length)
        return explainer.explain_instance(ts=point)