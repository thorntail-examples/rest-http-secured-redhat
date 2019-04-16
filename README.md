# Documentation

https://launcher.fabric8.io/docs/thorntail-runtime.html#mission-rest-http-secured-wf-swarm

# Integration test

```bash
oc apply -f service.sso.yaml

mvn clean verify -Popenshift,openshift-it -DSSO_AUTH_SERVER_URL=$(oc get route secure-sso -o jsonpath='{"https://"}{.spec.host}{"/auth\n"}')

oc delete -f service.sso.yaml
```
