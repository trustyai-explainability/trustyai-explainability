import pandas as pd
from sklearn.ensemble import RandomForestRegressor
import numpy as np

class RandomForestUniVariateForecaster:
    def __init__(self, n_past=4, n_future=1, params={"n_estimators": 250}):
        self.n_past = n_past
        self.n_future = n_future
        self.model = RandomForestRegressor(**params)

    def fit(self, X):
        train = self._series_to_supervised(X, n_in=self.n_past, n_out=self.n_future)
        trainX, trainy = train[:, : -self.n_future], train[:, -self.n_future :]
        self.model = self.model.fit(trainX, trainy)
        return self

    def _series_to_supervised(self, data, n_in=1, n_out=1, dropnan=True):
        n_vars = 1 if type(data) is list else data.shape[1]
        df = pd.DataFrame(data)
        cols = list()

        # input sequence (t-n, ... t-1)
        for i in range(n_in, 0, -1):
            cols.append(df.shift(i))
        # forecast sequence (t, t+1, ... t+n)
        for i in range(0, n_out):
            cols.append(df.shift(-i))
        # put it all together
        agg = pd.concat(cols, axis=1)
        # drop rows with NaN values
        if dropnan:
            agg.dropna(inplace=True)
        return agg.values

    def predict(self, X):
        row = X[-self.n_past :].flatten()
        y_pred = self.model.predict(np.asarray([row]))
        return y_pred
    
from aix360.datasets import SunspotDataset
from aix360.algorithms.tsutils.tsframe import tsFrame
from sklearn.model_selection import train_test_split

def create_model():
    df, schema = SunspotDataset().load_data()
    sunspot_ts_frame = tsFrame(df, timestamp_column=schema["timestamp"], columns=schema["targets"])

    ts_train, ts_test = train_test_split(sunspot_ts_frame,
                                        shuffle=False,
                                        stratify=None,
                                        test_size=0.15,
                                        train_size=None,
                                        )
    
    input_length = 144 # 12 years
    forecast_horizon = 12 # 1 year
    forecaster = RandomForestUniVariateForecaster(
    n_past=input_length, n_future=forecast_horizon)

    _ = forecaster.fit(ts_train)
    return forecaster