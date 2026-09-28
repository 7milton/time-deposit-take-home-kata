package org.ikigaidigital.domain;

import org.ikigaidigital.TimeDeposit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TimeDepositCalculatorTest {
    private final TimeDepositCalculator calculator = new TimeDepositCalculator(List.of(
        new StudentInterestPolicy(), new PremiumInterestPolicy(), new BasicInterestPolicy()));

    @ParameterizedTest
    @CsvSource({
        "basic, 0, 1000.00, 1000.00",
        "basic, 29, 1000.00, 1000.00",
        "student, 364, 1000.00, 1002.50",
        "student, 367, 1000.00, 1000.00",
        "premium, 44, 1000.00, 1000.00",
        "premium, 47, 1000.00, 1004.17",
        "basic, 31, 5.99, 5.99",
        "basic, 31, 6.00, 6.01",
        "basic, 31, 6.01, 6.02",
        "basic, 31, 18.00, 18.01"
    })
    void coversAdjacentDaysAndCentRounding(String plan, int days, double balance, double expected) {
        TimeDeposit deposit = new TimeDeposit(1, plan, balance, days);

        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isCloseTo(expected, within(1e-9));
    }

    @Test
    void preservesPlanThresholdsAndUnknownPlanBehavior() {
        List<TimeDeposit> deposits = List.of(
            new TimeDeposit(1, "basic", 1000.00, 30),
            new TimeDeposit(2, "basic", 1000.00, 31),
            new TimeDeposit(3, "student", 1000.00, 31),
            new TimeDeposit(4, "student", 1000.00, 365),
            new TimeDeposit(5, "student", 1000.00, 366),
            new TimeDeposit(6, "premium", 1000.00, 45),
            new TimeDeposit(7, "premium", 1000.00, 46),
            new TimeDeposit(8, "BASIC", 1000.00, 46),
            new TimeDeposit(9, "unknown", 1000.00, 46)
        );

        calculator.updateBalance(deposits);

        assertThat(deposits).extracting(TimeDeposit::getBalance)
            .containsExactly(1000.00, 1000.83, 1002.50, 1002.50, 1000.00,
                1000.00, 1004.17, 1000.00, 1000.00);
        assertThat(deposits).extracting(TimeDeposit::getDays)
            .containsExactly(30, 31, 31, 365, 366, 45, 46, 46, 46);
    }

    @Test
    void eachInvocationAccruesAgainOnTheUpdatedBalance() {
        TimeDeposit deposit = new TimeDeposit(1, "basic", 1000.00, 45);

        calculator.updateBalance(List.of(deposit));
        assertThat(deposit.getBalance()).isEqualTo(1000.83);

        calculator.updateBalance(List.of(deposit));
        assertThat(deposit.getBalance()).isCloseTo(1001.66, within(1e-9));
        assertThat(deposit.getDays()).isEqualTo(45);
    }

    @Test
    void aNewPlanCanBeAddedWithoutEditingTheCalculator() {
        InterestPolicy flexiblePlan = new InterestPolicy() {
            @Override
            public boolean appliesTo(TimeDeposit deposit) {
                return deposit.getPlanType().equals("flexible");
            }

            @Override
            public double monthlyInterest(TimeDeposit deposit) {
                return deposit.getBalance() * 0.10;
            }
        };
        TimeDeposit deposit = new TimeDeposit(1, "flexible", 100.00, 31);

        new TimeDepositCalculator(List.of(flexiblePlan)).updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(110.00);
    }

    @Test
    void aZeroInterestFromTheFirstMatchingPolicyDoesNotFallThrough() {
        InterestPolicy first = new InterestPolicy() {
            @Override
            public boolean appliesTo(TimeDeposit deposit) {
                return true;
            }

            @Override
            public double monthlyInterest(TimeDeposit deposit) {
                return 0;
            }
        };
        InterestPolicy second = new InterestPolicy() {
            @Override
            public boolean appliesTo(TimeDeposit deposit) {
                return true;
            }

            @Override
            public double monthlyInterest(TimeDeposit deposit) {
                return 100;
            }
        };
        TimeDeposit deposit = new TimeDeposit(1, "basic", 1000.00, 45);

        new TimeDepositCalculator(List.of(first, second)).updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(1000.00);
    }
}
