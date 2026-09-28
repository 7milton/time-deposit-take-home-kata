package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.port.in.TimeDepositUseCase;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.ikigaidigital.domain.TimeDepositCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TimeDepositFacade implements TimeDepositUseCase {
    private final TimeDepositRepository deposits;
    private final TimeDepositCalculator calculator;

    public TimeDepositFacade(TimeDepositRepository deposits, TimeDepositCalculator calculator) {
        this.deposits = deposits;
        this.calculator = calculator;
    }

    /**
     * Runs one monthly interest calculation for each stored deposit in a single transaction.
     * The kata defines no accrual date, so each invocation credits eligible deposits
     * again on their current balance without advancing {@code days}.
     * If a balance cannot be saved, the whole update is rolled back.
     */
    @Transactional
    @Override
    public void accrueAllBalances() {
        List<TimeDeposit> accounts = deposits.lockAllForAccrual();
        calculator.updateBalance(accounts);
        deposits.saveBalances(accounts);
    }

    /**
     * Returns deposits ordered by ID, each with its withdrawals ordered by ID.
     * The kata does not define when withdrawals are applied, so they are treated as
     * historical records already reflected in the current balance.
     *
     * @return the stored deposits with their withdrawal history
     */
    @Transactional(readOnly = true)
    @Override
    public List<TimeDepositView> listDeposits() {
        return deposits.findAll();
    }
}
