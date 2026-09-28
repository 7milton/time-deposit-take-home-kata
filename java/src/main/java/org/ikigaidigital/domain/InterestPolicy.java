package org.ikigaidigital.domain;

import org.ikigaidigital.TimeDeposit;

/**
 * Plan-specific interest strategy. The calculator applies the first matching policy
 * after the common 30-day waiting period, even if that policy returns zero interest.
 */
public interface InterestPolicy {
    /**
     * Indicates whether this policy handles the deposit's plan type.
     * Day-based eligibility for that plan is evaluated in {@link #monthlyInterest(TimeDeposit)}.
     *
     * @param deposit deposit to check
     * @return whether this policy handles the deposit
     */
    boolean appliesTo(TimeDeposit deposit);

    /**
     * Calculates the unrounded interest increment without changing the deposit.
     * The calculator rounds the increment and updates the balance.
     *
     * @param deposit deposit on which interest is calculated
     * @return monthly interest increment, or zero when the plan is not yet eligible
     */
    double monthlyInterest(TimeDeposit deposit);
}
