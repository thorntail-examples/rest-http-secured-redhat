/*
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.thorntail.example;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.arquillian.cube.openshift.impl.enricher.AwaitRoute;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.util.HttpResponseException;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import javax.net.ssl.SSLContext;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@RunWith(Arquillian.class)
public class OpenshiftIT {
    @RouteURL(value = "secure-sso", path = "/auth")
    private String ssoUrl;

    @RouteURL("${app.name}")
    @AwaitRoute
    private String appUrl;

    private AuthzClient authzClient;

    @Before
    public void setup() throws Exception {
        authzClient = createAuthzClient(ssoUrl);
    }

    private Greeting getGreeting(String token, String from) {
        Client client = ClientBuilder.newClient();
        try {
            WebTarget target = client.target(appUrl);
            target.register((ClientRequestFilter) requestContext -> {
                requestContext.getHeaders().add("Authorization", "Bearer " + token);
            });
            IGreeting greetingClient = ((ResteasyWebTarget) target).proxy(IGreeting.class);
            return greetingClient.greeting(from);
        } finally {
            client.close();
        }
    }

    @Test
    public void defaultUser_defaultFrom() {
        AccessTokenResponse accessTokenResponse = authzClient.obtainAccessToken("alice", "password");

        Greeting greeting = getGreeting(accessTokenResponse.getToken(), null);

        assertThat(greeting).isNotNull();
        assertThat(greeting.getContent()).contains("Hello, World!");
    }

    @Test
    public void defaultUser_customFrom() {
        AccessTokenResponse accessTokenResponse = authzClient.obtainAccessToken("alice", "password");

        Greeting greeting = getGreeting(accessTokenResponse.getToken(), "Scott");

        assertThat(greeting).isNotNull();
        assertThat(greeting.getContent()).contains("Hello, Scott!");
    }

    // This test checks the "authenticated, but not authorized" flow.
    @Test
    public void adminUser() {
        AccessTokenResponse accessTokenResponse = authzClient.obtainAccessToken("admin", "admin");

        try {
            getGreeting(accessTokenResponse.getToken(), null);
            fail("403 Forbidden expected");
        } catch (ClientErrorException e) {
            assertThat(e.getResponse().getStatus()).isEqualTo(403);
        }
    }

    @Test
    public void badPassword() {
        try {
            authzClient.obtainAccessToken("alice", "bad");
            fail("401 Unauthorized expected");
        } catch (HttpResponseException t) {
            assertThat(t.getStatusCode()).isEqualTo(401);
        }
    }

    /**
     * We need a simplified setup that allows us to work with self-signed certificates.
     * To support this we need to provide a custom http client.
     */
    private static AuthzClient createAuthzClient(String ssoAuthUrl) throws Exception {
        InputStream configStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("keycloak.json");
        if (configStream == null) {
            throw new IllegalStateException("Could not find any keycloak.json file in classpath.");
        }

        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial((chain, authType) -> true)
                .build();
        HttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();

        System.setProperty("sso.auth.server.url", ssoAuthUrl);
        Configuration baseline = JsonSerialization.readValue(configStream, Configuration.class, true);

        return AuthzClient.create(
                new Configuration(
                        baseline.getAuthServerUrl(),
                        baseline.getRealm(),
                        baseline.getResource(),
                        baseline.getCredentials(),
                        httpClient
                )
        );
    }
}
