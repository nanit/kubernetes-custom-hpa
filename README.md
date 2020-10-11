# Kubernetes Custom HPA

The Kubernetes Custom HPA extends the basic HPA capabilities by facilitating behavioral configuration.

## Motivation

Kubernetes HPA scale behavior cannot be modified per resource. The default scale tolerance
prevents scale for less than 10%, and Some HPA configuration can only be set in the cluster level.

We wanted a fine-tuned HPA to be able to scale faster/slower according to our needs without affecting other HPAs in 
the cluster.

## Basic Usage

Add the Nanit helm charts repo as follows:

    $ helm repo add nanit https://nanit.github.io/helm-charts
    
Then you can install the chart by running:

    helm install nanit/custom-hpa \ 
        --version 1.0.0 \
        --set target.deployment=DEPLOYMENT \
        --set target.namespace=NAMESPACE \
        --set target.value=100 \
        --set minReplicas=10 \
        --set maxReplicas=50 \
        --set prometheus.url=http://localhost \
        --set prometheus.port=80 \
        --set prometheus.query=up

## Configuration

The custom HPA behavior can be configured by the following values:

| Parameter  | Description | Default |
| ---------- | ----------- | ------- |
| `controlLoopPeriod`  | Seconds to wait between each control loop period  | `15`
| `behavior.scaleUpCooldown` | Seconds to wait between scale up events | `15`
| `behavior.scaleDownCooldown` | Seconds to wait between scale down events | `15`
| `behavior.scaleUpMinFactor` | The Minimum factor for scale up event | `0.1`
| `behavior.scaleUpMaxFactor` | The Maximum factor for scale up event | `1.0`
| `behavior.scaleDownMinFactor` | The Minimum factor for scale down event | `0.1`
| `behavior.scaleDownMaxFactor` | The Maximum factor for scale down event | `1.0` 

## Custom HPA Algorithm

The custom HPA runs the following every `controlLoopPeriod` seconds:
* Fetch a prometheus metric sample
* Calculate the scale factor
* If scale permitted
    * set the desired number of pods to `scale_factor * (current number of pods)`

### Scale Factor

The basic scale factor is calculated as `metric_sample * target.value`. The result indicates whether we are going to 
perform a scale up (`> 1.0`) or down (`<= 1.0`).

The basic scale factor can be limited by `behavior.scaleUpMaxFactor` and `behavior.scaleDownMaxFactor` parameters.

#### Examples

1. `scale_factor = 1.3` and `behavior.scaleUpMaxFactor = 0.5` will result `1.3`, since the scale up is by 30% and max factor is 50%.
2. `scale_factor = 1.6` and `behavior.scaleUpMaxFactor = 0.5` will result `1.5`, since the scale up is by 60%, but max factor is 50%.
3. `scale_factor = 0.7` and `behavior.scaleDownMaxFactor = 0.5` will result `0.7` since the scale down is by 30% and max factor is 50%.
3. `scale_factor = 0.4` and `behavior.scaleDownMaxFactor = 0.5` will result `0.5` since the scale down is by 60%, but max factor is 50%. 

### Permit Scale

The custom HPA uses 2 parameters to determine if it is permitted to scale: cooldown and minimum scale factor.

Cooldown parameter indicates how many seconds the custom HPA needs to wait until it can perform the same scale event again.
The parameters can be set using `behavior.scaleUpCooldown` and `behavior.scaleDownCooldown`.

Minimum scale factor used the same way as the maximum scale factors and can be set using 
`behavior.scaleUpMinFactor` and `behavior.scaleDownMinFactor`.

#### Examples

1. `scale_factor = 1.3` and `behavior.scaleUpMinFactor = 0.2` will permit scale up since scale is by 30% and min factor is 20%.
2. `scale_factor = 1.1` and `behavior.scaleUpMinFactor = 0.2` will not permit scale up since scale is by 10%, but min factor is 20%.
3. `scale_factor = 0.6` and `behavior.scaleDownMinFactor = 0.2` will permit scale down since scale is by 40% and min factor is 20%.
3. `scale_factor = 0.9` and `behavior.scaleDownMinFactor = 0.2` will not permit scale down since scale is by 10%, but min factor is 20%.

## Development

The custom HPA is developed in Clojure. Enter a REPL by running `make dev`. 