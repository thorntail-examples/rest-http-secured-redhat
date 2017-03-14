# Introduction

## Basic Functionality

This project exposes a simple REST endpoint where the service `greeting` is available, but properly secured, at this address `http://hostname:port/greeting`
and returns a json Greeting message after the application issuing the call to the REST endpoint has been granted to access the service.

```json
{
    "content": "Hello, World!",
    "id": 1
}
```

The id of the message is incremented for each request. To customize the message, you can pass as parameter the name of the person that you want to send your greeting.

## Security Constraints

To manage the security, roles & permissions to access the service, a [Red Hat SSO](https://access.redhat.com/documentation/en/red-hat-single-sign-on/7.0/securing-applications-and-services-guide/securing-applications-and-services-guide) backend will be installed and configured for this project.
It relies on the Keycloak project which implements the OpenId connect specification which is an extension of the Oauth2 protocol.

After a successful login, the application will receive an `identity token` and an `access token`.
The identity token contains information about the user such as username, email, and other profile information.
The access token is digitally signed by the realm and contains access information (like user role mappings).

This `access token` is typically formatted as a JSON Token that the WildflySwarm application will use with its Keycloak adapter to determine what resources it is allowed to access on the application.
The configuration of the adapter is defined within the `app/src/main/resources/keycloak.json` file using these properties:

```
{
  "realm": "${sso.realm:realm}",
  "resource": "${sso.clientId:demoapp}",
  "realm-public-key": "${sso.realm-public-key:...}",
  "auth-server-url": "${sso.auth-server-url:http://localhost:8180/auth}",  
  "ssl-required": "external",  
  "credentials": {
    "secret": "${sso.secret:...}"
  }  
}

```

## Red Hat SSO

The security context is managed by Red Hat SSO using a realm (defined using the `sso.realm` property) where the adapter to establish a trusted TLS connection will use the Realm Public key defined using the `sso.realm-public-key` property.
To access the server, the parameter `sso.auth-server-url` is defined using the TLS address of the host followed with `/auth`.
To manage different clients or applications, a resource has been created for the realm using the property `sso.clientId`.
This parameter, combined with the `credentials.secret` property, will be used during the authentication phase to log in the application.
If, it has been successfully granted, then a token will be issued that the application will use for the subsequent calls.

The request that is issued to authenticate the application is:

```
https://<RED_HAT_SSO>/auth/realms/<REALM>/protocol/openid-connect/token?client_secret=<SECRET>&grant_type=password&client_id=CLIENT_APP
```

And the HTTP requests accessing the endpoint/Service will include the following Bearer Token:

```
http://<SWARM_SERVICE>/greeting -H "Authorization:Bearer <ACCESS_TOKEN>"
```

The project is split into two Apache Maven modules - `app` and `sso`.
The `App` module exposes the REST Service using WildflySwarm.
The `sso` module contains the OpenShift objects required to deploy the Red Hat SSO Server 7.0.

The goal of this project is to deploy the quickstart in an OpenShift environment (online, dedicated, ...).

# Prerequisites

To get started with these quickstarts you'll need the following prerequisites:

Name | Description | Version
--- | --- | ---
[java][1] | Java JDK | 8
[maven][2] | Apache Maven | 3.2.x
[oc][3] | OpenShift Client | v3.3.x
[git][4] | Git version management | 2.x

[1]: http://www.oracle.com/technetwork/java/javase/downloads/
[2]: https://maven.apache.org/download.cgi?Preferred=ftp://mirror.reverse.net/pub/apache/
[3]: https://docs.openshift.com/enterprise/3.2/cli_reference/get_started_cli.html
[4]: https://git-scm.com/book/en/v2/Getting-Started-Installing-Git

In order to build and deploy this project, you must have an account on an OpenShift Online (OSO): https://console.dev-preview-int.openshift.com/ instance.

# OpenShift Online

1. Using OpenShift Online or Dedicated, log on to the OpenShift Server.

    ```bash
    oc login https://<OPENSHIFT_ADDRESS> --token=MYTOKEN
    ```

2. Create a new project on OpenShift.

    ```bash
    oc new-project <PROJECT_NAME>
    ```

# Build and deploy the Application

1. Build the top level project once

  ```
  mvn clean install
  ```
  
2. To deploy Red Hat SSO, move to `sso` folder and then use the Fabric8 Maven Plugin to deploy the SSO server:

    ```bash
    cd sso
    mvn fabric8:deploy -Popenshift
    ```

3. Next, the WildFly Swarm application should be packaged and deployed. This process will generate the uber jar file, the OpenShift resources
   and deploy them within the namespace of the OpenShift Server

    ```
    cd app
    mvn fabric8:deploy -Popenshift
    ```

4. Make yourself familiar with the routes to the Red Hat SSO server and the WildflySwarm service. We will refer to these two addresses as `<RED_HAT_SSO>` and `<SWARM_SERVICE>` respectively.

  You can do this using the OpenShift web console or using the command line:

  ```
  oc get routes

  NAME                 HOST/PORT                                                 PATH      SERVICES             PORT      TERMINATION
secure-sso           <RED_HAT_SSO>                     secure-sso           <all>     passthrough
secured-swarm-rest   <SWARM_SERVICE>             secured-swarm-rest   8080      
  ```

5. Until [CLOUD-1166](https://issues.jboss.org/browse/CLOUD-1166) is fixed, we need to fix the redirect-uri in RH-SSO admin console, to point to our app's route (`<SWARM_SERVICE>` above).

  To do that, access the Red Hat SSO Admin Console at `http://<RED_HAT_SSO>/auth`, login with `admin:admin` and update the `demoapp` settings to allow redirects from `http://<SWARM_SERVICE>/*`:

  ```
  Clients > Demoapp > Settings > Valid Redirect URIs     
  ```

6. To specify the Red Hat SSO URL to be used by the WildFly Swarm application for authentication,
you must change the `SSO_AUTH_SERVER_URL` env variable for the pods to the `<RED_HAT_SSO>` value identified before:

    ```
    oc env dc/secured-swarm-rest SSO_AUTH_SERVER_URL=https://<RED_HAT_SSO>/auth

    ```    

# Access the service

## Browser based access

Pointing your browser to `http://<SWARM_SERVICE>/greeting` should redirect to Red Hat SSO for authentication.
You can login with `admin:admin` and Red Hat SSO redirects you the REst endpoint. BUt it may the previous session for user `admin`
is still active. In that case no authentication will be prompted again.

If everything works well you should receive a JSON response:

```
{"id":2,"content":"Hello World"}   
```
