# TrustyQL: Querying from the Data Download Endpoint
Querying data from the TrustyAI inference data is straightforward, involving sending a `DataRequestPayload`
to the `/data/download` endpoint. The `DataRequestPayload` first requires a `modelId` (such as to determine which dataset to extract from)
which must match an existing `modelId` within the `TrustyAI` metadata, which can be viewed from the `/info` endpoint.

Next, the `DataRequestPayload` asks for three lists of `RowMatcher`, the `All`, `Any`, and `None` lists. 

# RowMatcher
A row matcher has three fields:
 * `columnName`: the column of the dataset to perform the matching operation over
 * `operation`: one of `EQUALS` or `BETWEEN`
 * `values`: a list of values to define the match

The row matcher can be thought of as performing an SQL-esque query:

## For `operation=EQUALS`:
```json
{
  "columnName": "age",
  "operation": "EQUALS",
  "values": [30, 31, 52]
}
```
This will match all rows where the value of the provided column is contained anywhere within the `values` list.
If `columnName="age'` and `values=[30, 31, 52]`, all rows where age=30, 31, **or** 52 would match.

## For `operation=BETWEEN`:
```json
{
  "columnName": "age",
  "operation": "BETWEEN",
  "values": [30, 35]
}
```
This will match all rows where the value of the provided column is in the range `[values[0], values[1])`
If `columnName="age'` and `values=[30, 35]`, all rows where 30<=age<35 would match. 

The `BETWEEN` operation is only valid for data types with strict ordering, such as numbers or datetimes.

# ALL, ANY, NONE
`RowMatchers` can be composed in the `matchAll`, `matchAny`, and `matchNone` lists within the 
`DataRequestPayload`. The request will return all rows of the `modelId` data wherein all of the following are true:

* All of the `matchAll` `rowMatcher` match
* Any of the `matchAny` `rowMatcher` match
* None of the `matchNone` `rowMatchers` match

# Special Columns
A few special columns are provided, to allow for querying over data metadata such as timestamp or tag:

* `trustyai.INDEX`: the raw numeric index of the row
* `trustyai.ID`: the string UUID of the datapoint
* `trustyai.TIMESTAMP`: the `LocalDateTime` that the datapoint was received
* `trustyai.TAG`: the tag associated with this datapoint, as specified in the `/info/tags` endpoint (such as `TRAINING` or `SYNTHETIC`)

# Example
## Entire Dataset
To retrieve the entire, unfiltered dataset, simply pass a request with no specified `match*` lists:
```json
{
  "modelId": "example-model-dataset"
}
```

## Filtering a Dataset
```json
{
    "modelId": "name-age-nationality",
    "matchAll": [ 
        {"columnName": "age", "operation": "BETWEEN", "values": [50, 75]},
        {"columnName": "nationality", "operation": "EQUALS", "values": ["Italian", "French"]},
        {"columnName": "trustyai.TIMESTAMP", "operation": "BETWEEN", "values": ["2023-01-01T00:00:01.00", "French"]}
    ], 
    "matchAny": [ 
         {"columnName": "favoriteFood", "operation": "EQUALS", "values": ["Pizza"]},
         {"columnName": "favoriteColor", "operation": "EQUALS", "values": ["Red"]}
    ],
    "matchNone": [
        {"columnName": "age", "operation": "BETWEEN", "values": [60, 65]},
        {"columnName": "name", "operation": "EQUALS", "values": ["Pierre", "James"]}
    ]
}
```
This query would return every datapoint in the dataset `name-age-nationality` wherein all the following is true:
* Their `age` is between 50 and 75, but *not between* 60 or 65.
* Their `nationality` is one of `Italian` or `French`
* Their `favoriteFood` is `Pizza` or their `favoriteColor` is `Red`
* Their `name` is *not* `Pierre` nor `James`