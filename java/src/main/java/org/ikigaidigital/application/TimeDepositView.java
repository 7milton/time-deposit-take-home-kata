package org.ikigaidigital.application;

import java.math.BigDecimal;
import java.util.List;

public record TimeDepositView(int id, String planType, BigDecimal balance, int days,
                              List<WithdrawalView> withdrawals) {
    public TimeDepositView {
        withdrawals = List.copyOf(withdrawals);
    }
}
