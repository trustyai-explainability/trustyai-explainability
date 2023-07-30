from abc import ABC, abstractmethod

import pandas as pd


class Metric(ABC):
    """Base class for all metrics"""

    @abstractmethod
    def calculate(self, df: pd.DataFrame) -> float:
        pass
