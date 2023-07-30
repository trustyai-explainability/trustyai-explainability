from typing import Union, List

import numpy as np
import pandas as pd
import requests


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
