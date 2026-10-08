package org.ikigaidigital;

import org.ikigaidigital.domain.BasicInterestPolicy;
import org.ikigaidigital.domain.PremiumInterestPolicy;
import org.ikigaidigital.domain.StudentInterestPolicy;

import java.util.List;

/**
 * Retains the original kata's public entry point for existing callers.
 * Calculation is delegated to the same domain implementation used by Spring.
 */
public class TimeDepositCalculator {
    private final org.ikigaidigital.domain.TimeDepositCalculator delegate;

    /** Creates the original three-plan calculator without requiring a Spring context. */
    public TimeDepositCalculator() {
        delegate = new org.ikigaidigital.domain.TimeDepositCalculator(List.of(
            new StudentInterestPolicy(), new PremiumInterestPolicy(), new BasicInterestPolicy()));
    }

    /**
     * Credits one interest increment in place, preserving the original thresholds and rounding.
     * Days are unchanged; subsequent calls accrue again on the updated balances.
     *
     * @param deposits deposits whose balances are updated in place
     */
    public void updateBalance(List<TimeDeposit> deposits) {
        delegate.updateBalance(deposits);
    }
}
