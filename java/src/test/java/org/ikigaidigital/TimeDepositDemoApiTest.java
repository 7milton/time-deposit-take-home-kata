package org.ikigaidigital;

import org.ikigaidigital.adapter.out.persistence.DemoDataInitializer;
import org.ikigaidigital.adapter.out.persistence.TimeDepositEntity;
import org.ikigaidigital.adapter.out.persistence.TimeDepositJpaRepository;
import org.ikigaidigital.adapter.out.persistence.WithdrawalJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ActiveProfiles("demo")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TimeDepositDemoApiTest {
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    TimeDepositJpaRepository deposits;

    @Autowired
    WithdrawalJpaRepository withdrawals;

    @Autowired
    DemoDataInitializer initializer;

    @LocalServerPort
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void demoSeedsDepositsAndHistoricalWithdrawalOnlyOnce() {
        assertThat(deposits.count()).isEqualTo(3);
        assertThat(withdrawals.count()).isEqualTo(1);
        TimeDepositEntity basic = deposits.findAllWithWithdrawalsByOrderByIdAsc().stream()
            .filter(deposit -> deposit.getPlanType().equals("basic"))
            .findFirst()
            .orElseThrow();
        assertThat(basic.getWithdrawals()).singleElement().satisfies(withdrawal ->
            assertThat(withdrawal.getAmount()).isEqualByComparingTo("25.00"));

        initializer.run(null);

        assertThat(deposits.count()).isEqualTo(3);
        assertThat(withdrawals.count()).isEqualTo(1);
    }

    @Test
    void swaggerOriginCanPreflightBothOperations() throws Exception {
        HttpResponse<String> get = preflight("/api/time-deposits", "GET", "http://localhost:8081");
        HttpResponse<String> post = preflight("/api/time-deposits/update-balances", "POST", "http://localhost:8081");

        assertThat(get.statusCode()).isEqualTo(200);
        assertThat(get.headers().firstValue("Access-Control-Allow-Origin"))
            .hasValue("http://localhost:8081");
        assertThat(post.statusCode()).isEqualTo(200);
        assertThat(post.headers().firstValue("Access-Control-Allow-Origin"))
            .hasValue("http://localhost:8081");
    }

    @Test
    void demoCorsRejectsOtherOrigins() throws Exception {
        HttpResponse<String> response = preflight("/api/time-deposits", "GET", "https://other.example");

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
    }

    private HttpResponse<String> preflight(String path, String method, String origin) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
            .header("Origin", origin)
            .header("Access-Control-Request-Method", method)
            .build(), HttpResponse.BodyHandlers.ofString());
    }
}
