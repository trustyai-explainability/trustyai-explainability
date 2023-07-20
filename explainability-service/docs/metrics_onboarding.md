# Metrics Onboarding Guide

## New Metric Category
To add a new metric category (i.e., adding a metric that does not exist in the current metric taxonomy), follow
these steps

#### 1: Metric Request Payloads
* Create a package `payloads/metrics/{$taxonomy/path}`.

This will contain the request payload super+subclass(es) for metrics within the new category, defining how the endpoints will
receive the necessary information to correctly process the metric requests. 

* Create a general/abstract class for all metric requests of the new category: `payloads/metrics/{$taxonomy/path}/$GENERIC_TAXONOMY_REQUEST.java`
* Create extending/implementing classes for individual metrics within the category, if needed:
  * `payloads/metrics/{$taxonomy/path}/$TAXONOMY_METRIC_1_REQUEST.java` extends/implements `$GENERIC_TAXONOMY_REQUEST`
  * `payloads/metrics/{$taxonomy/path}/$TAXONOMY_METRIC_2_REQUEST.java` extends/implements `$GENERIC_TAXONOMY_REQUEST`
  * etc

Within the request classes, there are two special classes to use to indicate that this field refers to specific, typed
values within a model metadata. This constuction prevents requesters from having to provide type information in their 
metric requests; instead, it is automatically reconciled against the known column types within the model metadata. 
* `ReconcilableFeature`: Indicates that this field will contain a value within a Feature column of the model metadata, and
should therefore have the same type (`INT32`, `BOOL`, `STRING`, etc) as that column.
* `ReconcilableOutput`: Indicates that this field will contain a value within an Output column of the model metadata, should therefore have the same type (`INT32`, `BOOL`, `STRING`, etc) as that column.

Then, annotate these fields with the `@ReconcilerMatcher` annotation. This annotation provides information as to the getter of 
the _dataframe column name_ of the received value. For example, if I have some request class where I am specifying a `ReconcilableFeature`:

```java
private String protectedAttributeName;
public ReconcilableFeature privilegedAttributeValue;
```        

it would need to be annotated as:
```java
@ReconcilerMatcher(nameProvider = "getProtectedAttributeName")
public ReconcilableFeature privilegedAttributeValue
```
where the class has some getter
```java
public String getProtectedAttributeName() {
    return protectedAttributeName;
}
```
therefore providing every `ReconcilableFeature/Output` with a corresponding source of dataframe column names. 


#### 2: Endpoints
* Create a package `endpoints/metrics/{$taxonomy/path}`

This will contain the endpoint super+subclass(es) for your metric category, defining the REST API to access the metrics within
this taxonomy. 

* Create a general/abstract class for all endpoint class within the new category: `endpoints/metrics/{$taxonomy/path}/$GENERIC_TAXONOMY_ENDPOINT.java`.
NOTE: this class *must* extend `BaseEndpoint<$GENERIC_TAXONOMY_REQUEST>`, defining that your endpoint expects your `$GENERIC_TAXONOMY_REQUEST` class or subclasses. This extension is how your metric calculation function will be registered within the metric scheduler, to enable scheduled computation of your metric if so desired by requesters. 
* Create extending/implementing classes for individual metrics within the category, if needed:
   * `endpoints/metrics/{$taxonomy/path}/$TAXONOMY_METRIC_1_ENDPOINT.java` extends/implements `$GENERIC_TAXONOMY_ENDPOINT`
   * `endpoints/metrics/{$taxonomy/path}/$TAXONOMY_METRIC_2_ENDPOINT.java` extends/implements `$GENERIC_TAXONOMY_ENDPOINT`
   * etc

#### 3. Validators
* Create a package `validators/metrics/{$taxonomy/path}`

This will contain the validator(s) for all metric requests within your metric category:
* `validators/metrics/{$taxonomy/path}/TAXONOMY_METRIC_REQUEST_VALIDATOR.java`
* `validators/metrics/{$taxonomy/path}/VALID_TAXONOMY_METRIC_REQUEST.java`
