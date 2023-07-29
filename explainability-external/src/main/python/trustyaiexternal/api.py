import logging
from abc import ABC, abstractmethod
from typing import List, Union
import requests
import numpy as np
import pandas as pd

from aix360.algorithms.tsutils.tsframe import tsFrame

# Set up logging
logging.basicConfig(level=logging.WARNING)
logging.warning(__file__)


class Metric(ABC):
    """Base class for all metrics"""

    @abstractmethod
    def calculate(self, df: pd.DataFrame) -> float:
        pass


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


class Converter:
    """Converts between different Python and Java data types"""

    @staticmethod
    def dataframe_from_array(x: np.ndarray, names: List[str]) -> pd.DataFrame:
        """Converts a numpy array to a pandas dataframe with the given column names
        
        Args:
            x (np.ndarray): numpy array
            names (List[str]): column names
        """
        return pd.DataFrame(data=x, columns=names)

    @staticmethod
    def convert_java_map_to_dict(x: dict) -> dict:
        """Converts a Java map to a Python dictionary
        
        Args:
            x (dict): Java map
        """
        real_x = {}
        x.forEach(lambda k, v: real_x.update({k: v}))
        x = real_x
        return x

    @staticmethod
    def dataframe_from_map(x: dict) -> pd.DataFrame:
        """Converts a Java map to a Pandas dataframe
        
        Args:
            x (dict): Java map
        """
        return pd.DataFrame(data=Converter.convert_java_map_to_dict(x))

    @staticmethod
    def tsframe_from_java(column_names: List[str], values: List[List], timestamp: str, format: str) -> pd.DataFrame:
        """Converts a Java map to a time-series dataframe"""
        df = pd.DataFrame(dict(zip(column_names, values)))
        df[timestamp] = pd.to_datetime(df[timestamp], format=format)
        result = tsFrame(df=df, timestamp_column=timestamp)  # type: ignore
        return result

    @staticmethod
    def tsframe_from_dataframe(x: dict, timestamp_column: str, format: str) -> pd.DataFrame:
        """Converts a Java map to a time-series dataframe
        
        Args:
            x (dict): Java map
            timestamp_column (str): name of the timestamp column
            format (str): format of the timestamp column
        """
        logging.info("Converting")
        logging.info(x)
        df = pd.DataFrame(data=Converter.convert_java_map_to_dict(x))
        df[timestamp_column] = pd.to_datetime(df[timestamp_column], format=format)
        result = tsFrame(df=df, timestamp_column=timestamp_column)  # type: ignore
        logging.info(result)
        return result


class DummyModel:
    def predict(self, x: pd.DataFrame) -> pd.DataFrame:
        logging.warning(f"Dummy model received data: {x}")
        return x.tail(12)


class ModelMeshWrapper:
    def __init__(self,
                 target: str,
                 model_name: str,
                 model_version: str = None):
        self.model_name = model_name
        self.model_version = model_version
        self._url = f"http://{target}/v2/models/{model_name}/infer"

    def _build_request(self, inputs: Union[pd.DataFrame, np.ndarray, List[List[float]]]):
        if isinstance(inputs, np.ndarray):
            shape = list(inputs.shape)
            data = inputs.tolist()
        elif isinstance(inputs, pd.DataFrame):
            shape = [inputs.shape[0], inputs.shape[1]]
            data = inputs.values.tolist()
        else:
            raise TypeError("Unsupported data type. Please provide a numpy array or a pandas DataFrame.")

        # TODO: Add parameters to avoid bias contamination
        return {
            'inputs': [
                {
                    "name": "input",
                    "shape": shape,
                    "datatype": "FP32",  # TODO: Take from the data
                    "data": data
                }
            ]
        }

    def _parse_output(self, response) -> np.ndarray:
        data = {}
        for output in response['outputs']:
            # reshape data according to its shape, then flatten it
            data_array = np.array(output['data']).reshape(output['shape']).flatten()
            data[output['name']] = data_array.tolist()

        # construct DataFrame
        df = pd.DataFrame(data)
        return df.to_numpy()

    def predict(self, inputs: Union[pd.DataFrame, np.ndarray, List[List[float]]]) -> np.ndarray:
        request = self._build_request(inputs)
        response = requests.post(self._url, json=request)
        result = self._parse_output(response.json())
        return result
