# from aif360.algorithms.preprocessing.optim_preproc_helpers.data_preproc_functions import load_preproc_data_adult

# dataset_orig = load_preproc_data_adult()
# dataset_orig_train, dataset_orig_test = dataset_orig.split([0.7], shuffle=True)
# print(dataset_orig_train)
# print(type(dataset_orig_train))
# print(dataset_orig_train.features)
# print(type(dataset_orig_train.features))

# # print out some labels, names, etc.
# print("#### Training Dataset shape")
# print(dataset_orig_train.features.shape)
# print("#### Favorable and unfavorable labels")
# print(dataset_orig_train.favorable_label, dataset_orig_train.unfavorable_label)
# print("#### Protected attribute names")
# print(dataset_orig_train.protected_attribute_names)
# print("#### Privileged and unprivileged protected attribute values")
# print(dataset_orig_train.privileged_protected_attributes,
#       dataset_orig_train.unprivileged_protected_attributes)
# print("#### Dataset feature names")
# print(dataset_orig_train.feature_names)
# print(dataset_orig_train.labels)

# import pandas as pd

# df = pd.DataFrame(dataset_orig_train.features)
# df['income'] = dataset_orig_train.labels

# df.to_csv("adult.csv", header=False, index=False)

import numpy as np
import pandas as pd
from sklearn.datasets import make_classification
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score
import joblib

def generate_for_fit():
    # Required Libraries
      # Creating synthetic dataset with 5 features
      X, y = make_classification(n_samples=10000, n_features=5, n_informative=3, n_redundant=0,
                              n_clusters_per_class=1, weights=[0.99], flip_y=0, random_state=1)

      # Creating a DataFrame
      df = pd.DataFrame(X, columns=['Age', 'Sex', 'Race', 'Salary', 'Location'])
      df['Approved'] = y

      # Create a bias in the data
      df.loc[df['Sex'] > 0.2, 'Approved'] = 1
      df.loc[df['Salary'] < 0.2, 'Approved'] = 0

      return df

from aix360.datasets import SunspotDataset

def convert_sun_spots():
      df, schema = SunspotDataset().load_data()
      df.to_csv("../../resources/python/sunspots.csv", header=False, index=False)

if __name__ == "__main__":
      convert_sun_spots()
