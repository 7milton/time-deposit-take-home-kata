package org.ikigaidigital;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import org.ikigaidigital.adapter.out.persistence.TimeDepositEntity;
import org.ikigaidigital.adapter.out.persistence.TimeDepositJpaRepository;
import org.ikigaidigital.adapter.out.persistence.WithdrawalEntity;
import org.ikigaidigital.adapter.out.persistence.WithdrawalJpaRepository;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TimeDepositApiTest {
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
    TimeDepositRepository repository;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    RequestMappingHandlerMapping routes;

    @LocalServerPort
    int port;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void emptyDatabase() {
        withdrawals.deleteAllInBatch();
        deposits.deleteAllInBatch();
    }

    @Test
    void exposesExactlyTheTwoRequiredApiOperations() {
        var applicationMappings = routes.getHandlerMethods().entrySet().stream()
            .filter(entry -> entry.getValue().getBeanType().getPackageName().startsWith("org.ikigaidigital."))
            .map(Map.Entry::getKey)
            .toList();

        assertThat(applicationMappings)
            .extracting(mapping -> mapping.getMethodsCondition().getMethods(),
                mapping -> mapping.getPatternValues())
            .containsExactlyInAnyOrder(
                tuple(Set.of(RequestMethod.GET), Set.of("/api/time-deposits")),
                tuple(Set.of(RequestMethod.POST), Set.of("/api/time-deposits/update-balances")));
    }

    @Test
    void bothEndpointsUseTheDatabaseAndIncludeWithdrawals() throws Exception {
        TimeDepositEntity basic = deposit("basic", "1000.00", 45);
        TimeDepositEntity student = deposit("student", "500.00", 100);
        withdrawal(basic, "25.00", LocalDate.of(2026, 1, 15));

        HttpResponse<String> before = getDeposits();
        assertThat(before.statusCode()).isEqualTo(200);
        JsonNode all = json.readTree(before.body());
        assertThat(all.size()).isEqualTo(2);
        JsonNode first = all.get(0);
        assertThat(first.get("id").asInt()).isEqualTo(basic.getId());
        assertThat(first.get("planType").asText()).isEqualTo("basic");
        assertThat(first.get("balance").decimalValue()).isEqualByComparingTo("1000.00");
        assertThat(first.get("days").asInt()).isEqualTo(45);
        assertThat(first.get("withdrawals").size()).isEqualTo(1);
        assertThat(first.get("withdrawals").get(0).get("amount").decimalValue())
            .isEqualByComparingTo("25.00");
        assertThat(first.get("withdrawals").get(0).get("date").asText())
            .isEqualTo("2026-01-15");

        assertThat(postUpdate().statusCode()).isEqualTo(204);
        assertThat(balance(basic.getId())).isEqualByComparingTo("1000.83");
        assertThat(balance(student.getId())).isEqualByComparingTo("501.25");
        assertThat(postUpdate().statusCode()).isEqualTo(204);
        assertThat(balance(basic.getId())).isEqualByComparingTo("1001.66");
        assertThat(balance(student.getId())).isEqualByComparingTo("502.50");
        assertThat(json.readTree(getDeposits().body()).get(0).get("days").asInt()).isEqualTo(45);
    }

    @Test
    void aDepositWithoutWithdrawalsReturnsAnEmptyArray() throws Exception {
        deposit("student", "500.00", 366);

        JsonNode first = json.readTree(getDeposits().body()).get(0);

        assertThat(first.get("withdrawals").isArray()).isTrue();
        assertThat(first.get("withdrawals").size()).isZero();
        assertThat(postUpdate().statusCode()).isEqualTo(204);
        assertThat(balance(first.get("id").asInt())).isEqualByComparingTo("500.00");
    }

    @Test
    void withdrawalsStayWithTheirDepositsAndAreOrderedById() throws Exception {
        TimeDepositEntity first = deposit("basic", "100.00", 31);
        TimeDepositEntity second = deposit("premium", "200.00", 46);
        withdrawal(second, "20.00", LocalDate.of(2026, 2, 1));
        withdrawal(first, "10.00", LocalDate.of(2026, 2, 3));
        withdrawal(first, "5.00", LocalDate.of(2026, 2, 2));

        JsonNode deposits = json.readTree(getDeposits().body());

        assertThat(deposits.size()).isEqualTo(2);
        assertThat(deposits.get(0).get("id").asInt()).isEqualTo(first.getId());
        assertThat(deposits.get(1).get("id").asInt()).isEqualTo(second.getId());
        JsonNode firstWithdrawals = deposits.get(0).get("withdrawals");
        assertThat(firstWithdrawals.size()).isEqualTo(2);
        assertThat(firstWithdrawals.get(0).get("amount").decimalValue()).isEqualByComparingTo("10.00");
        assertThat(firstWithdrawals.get(1).get("amount").decimalValue()).isEqualByComparingTo("5.00");
        assertThat(deposits.get(1).get("withdrawals").get(0).get("amount").decimalValue())
            .isEqualByComparingTo("20.00");
        assertThat(deposits.get(0).get("balance").decimalValue()).isEqualByComparingTo("100.00");
        assertThat(deposits.get(1).get("balance").decimalValue()).isEqualByComparingTo("200.00");
    }

    @Test
    void failedBalanceUpdateRollsBackEarlierDeposits() throws Exception {
        TimeDepositEntity first = deposit("basic", "1000.00", 45);
        TimeDepositEntity overflowing = deposit("premium", "99999999999999999.00", 46);

        assertThat(postUpdate().statusCode()).isEqualTo(500);
        assertThat(balance(first.getId())).isEqualByComparingTo("1000.00");
        assertThat(balance(overflowing.getId())).isEqualByComparingTo("99999999999999999.00");
    }

    @Test
    void accrualKeepsWriteLocksUntilTheTransactionCompletes() {
        TimeDepositEntity deposit = deposit("basic", "1000.00", 45);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            repository.lockAllForAccrual();

            assertThatThrownBy(() -> acquireIndependentWriteLock(deposit.getId()))
                .isInstanceOfAny(LockTimeoutException.class, PessimisticLockException.class);
        });

        assertThatCode(() -> acquireIndependentWriteLock(deposit.getId()))
            .doesNotThrowAnyException();
    }

    @Test
    void defaultProfileDoesNotEnableDemoCors() throws Exception {
        HttpResponse<String> response = preflight("/api/time-deposits", "GET");

        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
    }

    @Test
    void emptyDatabaseAndForeignKeyBehaveAsSpecified() throws Exception {
        assertThat(json.readTree(getDeposits().body()).size()).isZero();
        assertThat(postUpdate().statusCode()).isEqualTo(204);
        TimeDepositEntity missing = deposits.getReferenceById(999);
        assertThatThrownBy(() -> withdrawals.saveAndFlush(WithdrawalEntity.builder()
            .deposit(missing).amount(new BigDecimal("1.00")).date(LocalDate.of(2026, 1, 15)).build()))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private BigDecimal balance(int id) {
        return deposits.findById(id).orElseThrow().getBalance();
    }

    private void acquireIndependentWriteLock(int id) {
        // A separate persistence context creates a competing database transaction.
        try (var entityManager = entityManagerFactory.createEntityManager()) {
            var transaction = entityManager.getTransaction();
            transaction.begin();
            try {
                entityManager.find(TimeDepositEntity.class, id, LockModeType.PESSIMISTIC_WRITE,
                    Map.of("jakarta.persistence.lock.timeout", 0));
            } finally {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
            }
        }
    }

    private TimeDepositEntity deposit(String planType, String balance, int days) {
        return deposits.save(TimeDepositEntity.builder()
            .planType(planType).balance(new BigDecimal(balance)).days(days).build());
    }

    private void withdrawal(TimeDepositEntity deposit, String amount, LocalDate date) {
        withdrawals.save(WithdrawalEntity.builder()
            .deposit(deposit).amount(new BigDecimal(amount)).date(date).build());
    }

    private HttpResponse<String> getDeposits() throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/time-deposits"))
            .GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postUpdate() throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/time-deposits/update-balances"))
            .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> preflight(String path, String method) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
            .header("Origin", "http://localhost:8081")
            .header("Access-Control-Request-Method", method)
            .build(), HttpResponse.BodyHandlers.ofString());
    }
}
