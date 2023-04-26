# E2E Tests
This script tests the current ODH-Manifests master integration of TrustyAI.

# Usage
1. Install the ODH operator onto your cluster
2. Make sure you logged into the oc CLI: `oc login ...` 
3. (If you've run a script before, run):
   1. `./script.sh clean` to clean the previous test, such that you can run another one again
   2. `./script.sh reset` to completely remove all test objects
4. Run `./[script].sh`
5. At a certain point in the script, it will say:
```
Ensure metrics are in Prometheus
Please check that trustyai_spd data exists at the following endpoint:
[URL]
```
You'll need to click on that url, which will open the cluster login page. Once logged in, you'll see the Prometheus UI:

![Prometheus Splash](images/prometheus_splash.png)
Enter `trustyai_spd` into the "Expression" field and hit `Execute`. You (should) then see the query result:


![Prometheus Query Result](images/prometheus_spd.png)


Hit the `Graph` tab and make sure there is data populated there:

![Prometheus Graph Result](images/prometheus_graph.png)

If those steps proceeded without issue, return to the terminal and hit enter.

4. The script will finish and clean up the cluster.

# Testing your own image of TrustyAI
1) Build and push your `trustyai-service` image
2) Edit `e2e_tests/resources/manifests/trustyai-service/default/trustyai-deployment.yaml`, line 120 to point to your 
image repo.
3) Push the `trustyai-explainability` repo (including your changes to the `trustyai-deployment`file) to a branch
4) Edit  `e2e_tests/resources/common/odh-core.yaml`, line 98 to point to your pushed branch
5) Run the test scripts