package org.ikigaidigital.domain;

import org.ikigaidigital.TimeDeposit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Calculates one monthly interest increment for each deposit using the registered policies. */
public final class TimeDepositCalculator {
    private final List<InterestPolicy> policies;

    public TimeDepositCalculator(List<InterestPolicy> policies) {
        this.policies = List.copyOf(policies);
    }

    /**
     * Updates each deposit in place with the first applicable policy's monthly interest.
     * No interest is applied during the first 30 days. The interest increment is rounded
     * to cents using {@link RoundingMode#HALF_UP}; {@code days} is not changed, and each
     * subsequent call accrues again on the updated balance.
     *
     * @param deposits deposits whose balances are updated in place
     */
    public void updateBalance(List<TimeDeposit> deposits) {
        deposits.stream().forEachOrdered(this::updateDepositBalance);
    }

    private void updateDepositBalance(TimeDeposit deposit) {
        double interest = calculateMonthlyInterest(deposit);
        // Preserve the original double-to-decimal rounding at half-cent boundaries.
        double roundedInterest = new BigDecimal(interest)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();

        deposit.setBalance(deposit.getBalance() + roundedInterest);
    }

    private double calculateMonthlyInterest(TimeDeposit deposit) {
        if (deposit.getDays() <= 30) {
            return 0;
        }

        return policies.stream()
            .filter(policy -> policy.appliesTo(deposit))
            .findFirst()
            .map(policy -> policy.monthlyInterest(deposit))
            .orElse(0.0);
    }
}
