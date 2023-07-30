from typing import List

import numpy as np
import pandas as pd
import logging
from aix360.algorithms.tsutils.tsframe import tsFrame


logging.basicConfig(level=logging.WARNING)
logging.warning(__file__)


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
