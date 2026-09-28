package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.port.in.TimeDepositUseCase;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.ikigaidigital.domain.BasicInterestPolicy;
import org.ikigaidigital.domain.TimeDepositCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TimeDepositFacadeTest {
    @Test
    void runsUseCaseWithAnInMemoryRepository() {
        TimeDepositRepository repository = new TimeDepositRepository() {
            private double persistedBalance = 1000.00;

            @Override
            public List<TimeDeposit> lockAllForAccrual() {
                return List.of(new TimeDeposit(1, "basic", persistedBalance, 45));
            }

            @Override
            public void saveBalances(List<TimeDeposit> deposits) {
                persistedBalance = deposits.getFirst().getBalance();
            }

            @Override
            public List<TimeDepositView> findAll() {
                return List.of(new TimeDepositView(1, "basic", BigDecimal.valueOf(persistedBalance),
                    45, List.of()));
            }
        };
        TimeDepositUseCase facade = new TimeDepositFacade(repository,
            new TimeDepositCalculator(List.of(new BasicInterestPolicy())));

        facade.accrueAllBalances();
        facade.accrueAllBalances();

        assertThat(facade.listDeposits()).singleElement().satisfies(deposit -> {
            assertThat(deposit.balance().doubleValue()).isCloseTo(1001.66, within(1e-9));
            assertThat(deposit.days()).isEqualTo(45);
        });
    }
}
