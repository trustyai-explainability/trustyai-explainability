from abc import ABC, abstractmethod

import pandas as pd

from trustyaiexternal.api.models import ModelMeshWrapper


class Explainer(ABC):
    """Base class for all explainers"""

    def __init__(self,
                 model_name: str,
                 model_version: str,
                 target: str):
        self._model = ModelMeshWrapper(model_name=model_name,
                                       model_version=model_version,
                                       target=target)

    @abstractmethod
    def explain(self, point: pd.DataFrame) -> dict:
        pass
