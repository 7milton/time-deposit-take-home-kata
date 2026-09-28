package org.ikigaidigital.domain;

import org.ikigaidigital.TimeDeposit;

public final class BasicInterestPolicy implements InterestPolicy {
    @Override
    public boolean appliesTo(TimeDeposit deposit) {
        return deposit.getPlanType().equals("basic");
    }

    @Override
    public double monthlyInterest(TimeDeposit deposit) {
        return deposit.getBalance() * 0.01 / 12;
    }
}
