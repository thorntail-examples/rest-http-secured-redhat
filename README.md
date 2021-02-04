# Thorntail REST HTTP Secured Example

## Purpose

This example demonstrates how to secure a simple HTTP endpoint using Keycloak SSO.

## Prerequisites

* Log into an OpenShift cluster of your choice: `oc login ...`.
* Select a project in which the services will be deployed: `oc project ...`.

## Deployment

Run the following commands to configure and deploy the applications.

### Deployment using S2I

Not supported.

### Deployment with the JKube OpenShift Maven Plugin

```bash
oc apply -f service.sso.yaml

mvn clean oc:deploy -Popenshift -DSSO_AUTH_SERVER_URL=$(oc get route secure-sso -o jsonpath='https://{.spec.host}/auth')
```

## Test everything

This is completely self-contained and doesn't require the application to be deployed in advance.
Note that this may delete anything and everything in the OpenShift project.

```bash
oc apply -f service.sso.yaml

mvn clean verify -Popenshift,openshift-it -DSSO_AUTH_SERVER_URL=$(oc get route secure-sso -o jsonpath='https://{.spec.host}/auth')

oc delete -f service.sso.yaml
```
