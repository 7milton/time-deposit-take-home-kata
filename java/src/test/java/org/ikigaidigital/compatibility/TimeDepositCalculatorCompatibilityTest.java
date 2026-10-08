package org.ikigaidigital.compatibility;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

class TimeDepositCalculatorCompatibilityTest {
    // Compile and exercise the original public API without a Spring context.
    private final TimeDepositCalculator calculator = new TimeDepositCalculator();

    @Test
    void originalEntryPointUpdatesAllPlansInPlaceWithLegacyRounding() {
        List<TimeDeposit> deposits = List.of(
            new TimeDeposit(1, "basic", 1000.00, 31),
            new TimeDeposit(2, "student", 1000.00, 365),
            new TimeDeposit(3, "premium", 1000.00, 46),
            new TimeDeposit(4, "basic", 18.00, 31),
            new TimeDeposit(5, "student", 1000.00, 30),
            new TimeDeposit(6, "student", 1000.00, 366),
            new TimeDeposit(7, "premium", 1000.00, 45),
            new TimeDeposit(8, "unknown", 1000.00, 46)
        );

        calculator.updateBalance(deposits);

        assertThat(deposits).extracting(TimeDeposit::getId, TimeDeposit::getPlanType,
                TimeDeposit::getBalance, TimeDeposit::getDays)
            .containsExactly(
                tuple(1, "basic", 1000.83, 31),
                tuple(2, "student", 1002.50, 365),
                tuple(3, "premium", 1004.17, 46),
                tuple(4, "basic", 18.01, 31),
                tuple(5, "student", 1000.00, 30),
                tuple(6, "student", 1000.00, 366),
                tuple(7, "premium", 1000.00, 45),
                tuple(8, "unknown", 1000.00, 46));
    }

    @Test
    void originalEntryPointAccruesAgainOnTheUpdatedBalance() {
        TimeDeposit deposit = new TimeDeposit(1, "basic", 1000.00, 45);

        calculator.updateBalance(List.of(deposit));
        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isCloseTo(1001.66, within(1e-9));
        assertThat(deposit.getDays()).isEqualTo(45);
    }

    @Test
    void originalEntryPointAcceptsAnEmptyList() {
        assertThatCode(() -> calculator.updateBalance(List.of())).doesNotThrowAnyException();
    }
}
