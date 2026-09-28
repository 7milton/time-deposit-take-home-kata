package org.ikigaidigital.domain;

import org.ikigaidigital.TimeDeposit;

public final class StudentInterestPolicy implements InterestPolicy {
    @Override
    public boolean appliesTo(TimeDeposit deposit) {
        return deposit.getPlanType().equals("student");
    }

    @Override
    public double monthlyInterest(TimeDeposit deposit) {
        return deposit.getDays() < 366 ? deposit.getBalance() * 0.03 / 12 : 0;
    }
}
