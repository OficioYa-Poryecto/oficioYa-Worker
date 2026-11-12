package com.oficioya.worker.integration;

import com.oficioya.worker.exception.ExternalServiceUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.SocketTimeoutException;
import java.util.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OrchestratorRestClientTest {

    private MockRestServiceServer server;
    private OrchestratorRestClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://orchestrator/api");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OrchestratorRestClient(builder.build());
    }

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

        @Test
    void existAll_returnsTrueWhenAllZonesAreFound() {
        server.expect(method(HttpMethod.GET))
                .andExpect(request -> assertThat(request.getURI().getPath()).isEqualTo("/api/zones"))
                .andExpect(queryParam("ids", "1,2"))
                .andExpect(headerDoesNotExist("Authorization"))
                .andRespond(withSuccess("[{\"id\":1},{\"id\":2}]", MediaType.APPLICATION_JSON));

        assertThat(client.existAll(new LinkedHashSet<>(List.of(1L, 2L)))).isTrue();
        server.verify();
    }

    @Test
    void existAll_returnsFalseWhenSomeZoneIsMissing() {
        server.expect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"id\":1}]", MediaType.APPLICATION_JSON));

        assertThat(client.existAll(new LinkedHashSet<>(List.of(1L, 2L)))).isFalse();
    }

    @Test
    void existAll_withEmptySet_doesNotCallTheServer() {
        assertThat(client.existAll(Set.of())).isTrue();
        server.verify();
    }

    @Test
    void existAll_onServerError_throwsUnavailable() {
        server.expect(method(HttpMethod.GET)).andRespond(withServerError());

        assertThatThrownBy(() -> client.existAll(Set.of(1L)))
                .isInstanceOf(ExternalServiceUnavailableException.class);
    }

    @Test
    void existAll_onTimeout_throwsUnavailable() {
        server.expect(method(HttpMethod.GET)).andRespond(request -> {
            throw new SocketTimeoutException("Read timed out");
        });

        assertThatThrownBy(() -> client.existAll(Set.of(1L)))
                .isInstanceOf(ExternalServiceUnavailableException.class);
    }

        @Test
    void findActiveIds_forwardsTokenAndReturnsOnlyReturnedUsers() {
        UUID active = UUID.randomUUID();
        UUID inactive = UUID.randomUUID();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requestWithToken("Bearer abc")));

        server.expect(method(HttpMethod.GET))
                .andExpect(request -> assertThat(request.getURI().getPath()).isEqualTo("/api/users"))
                .andExpect(header("Authorization", "Bearer abc"))
                .andRespond(withSuccess("[{\"id\":\"" + active + "\",\"name\":\"Ana\"}]", MediaType.APPLICATION_JSON));

        assertThat(client.findActiveIds(List.of(active, inactive))).containsExactly(active);
        server.verify();
    }

    @Test
    void findActiveIds_withoutRequest_sendsNoToken() {
        UUID id = UUID.randomUUID();
        server.expect(method(HttpMethod.GET))
                .andExpect(headerDoesNotExist("Authorization"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.findActiveIds(List.of(id))).isEmpty();
    }

    @Test
    void findActiveIds_splitsInBatchesOf50() {
        List<UUID> ids = IntStream.range(0, 51).mapToObj(i -> UUID.randomUUID()).toList();

        server.expect(request -> assertThat(idsIn(request.getURI())).hasSize(50))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(request -> assertThat(idsIn(request.getURI())).hasSize(1))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.findActiveIds(ids);
        server.verify();
    }

    @Test
    void findActiveIds_onServerError_throwsUnavailable() {
        server.expect(method(HttpMethod.GET)).andRespond(withServerError());

        assertThatThrownBy(() -> client.findActiveIds(List.of(UUID.randomUUID())))
                .isInstanceOf(ExternalServiceUnavailableException.class);
    }

    @Test
    void findActiveIds_onTimeout_throwsUnavailable() {
        server.expect(method(HttpMethod.GET)).andRespond(request -> {
            throw new SocketTimeoutException("Read timed out");
        });

        assertThatThrownBy(() -> client.findActiveIds(List.of(UUID.randomUUID())))
                .isInstanceOf(ExternalServiceUnavailableException.class);
    }

    private static MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", token);
        return request;
    }

    private static String[] idsIn(java.net.URI uri) {
        return UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("ids").split(",");
    }
}