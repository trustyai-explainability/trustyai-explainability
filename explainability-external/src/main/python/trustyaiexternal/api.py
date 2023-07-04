import logging
from abc import ABC, abstractmethod
from typing import List, Union

import grpc
import numpy as np
import pandas as pd

import trustyaiexternal.proto_pb2 as proto_pb2
import trustyaiexternal.proto_pb2_grpc as proto_pb2_grpc

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
        self._model = GRPCModel(model_name=model_name, model_version=model_version, target=target)

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
        x.forEach(lambda k,v: real_x.update({k:v}))
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
    def tsframe_from_dataframe(x: dict, timestamp_column: str, format: str) -> pd.DataFrame:
        """Converts a Java map to a time-series dataframe
        
        Args:
            x (dict): Java map
            timestamp_column (str): name of the timestamp column
            format (str): format of the timestamp column
        """
        df = pd.DataFrame(data=Converter.convert_java_map_to_dict(x))
        df[timestamp_column] = pd.to_datetime(df[timestamp_column], format=format)
        return tsFrame(df=df, timestamp_column=timestamp_column) # type: ignore

class DummyModel:
    def predict(self, x: pd.DataFrame) -> pd.DataFrame:
        logging.warning(f"Dummy model received data: {x}")
        return x.tail(12)

class GRPCModel:
    """Wrapper for a GRPC model
    
    Connects to a GRPC model and provides a `predict` method to make predictions.

    Args:

        model_name (str): name of the model
        model_version (str): version of the model
        target (str): target of the model (gRPC host and port)
    """
    def __init__(self, model_name: str, model_version: str, target: str = '0.0.0.0:8081'):
        self.channel = grpc.insecure_channel(target)
        self.stub = proto_pb2_grpc.GRPCInferenceServiceStub(self.channel)
        self._model_name = model_name
        self._model_version = model_version

    def predict(self, x: Union[pd.DataFrame, np.ndarray]) -> np.ndarray:
        """Generic predict method for GRPC models.
        Aims at replacing a standard scikit-learn predict method.

        Args:
            x (Union[pd.DataFrame, np.ndarray]): input data, either a pandas dataframe or a numpy array
        """
        request = proto_pb2.ModelInferRequest()
        request.model_name = self._model_name # type: ignore
        request.model_version = self._model_version # type: ignore
        request.id = "request_id" # type: ignore

        if isinstance(x, pd.DataFrame):
            data = x.to_numpy()
        else:
            data = x

        data = data.T
        # print(data)
        # print([data.shape[0], data.shape[1]])
        input = proto_pb2.ModelInferRequest().InferInputTensor()
        input.name = "input"
        input.datatype = "FP64"
        input.shape.extend([data.shape[1], data.shape[0]])

        logging.warning(f"Data shape is: {data.shape}")

        input_contents = proto_pb2.InferTensorContents(fp64_contents=[1.0]*144)
        input.contents.MergeFrom(input_contents)

        request.inputs.extend([input])
        print(request)
        response = self.stub.ModelInfer(request)

        output = response.outputs[0]
        contents = output.contents.fp64_contents
        return np.array(output.contents.fp64_contents).T

