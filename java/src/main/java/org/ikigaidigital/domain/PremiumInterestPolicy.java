package org.ikigaidigital.domain;

import org.ikigaidigital.TimeDeposit;

public final class PremiumInterestPolicy implements InterestPolicy {
    @Override
    public boolean appliesTo(TimeDeposit deposit) {
        return deposit.getPlanType().equals("premium");
    }

    @Override
    public double monthlyInterest(TimeDeposit deposit) {
        return deposit.getDays() > 45 ? deposit.getBalance() * 0.05 / 12 : 0;
    }
}
