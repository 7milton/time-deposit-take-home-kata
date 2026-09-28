package org.ikigaidigital.application.port.in;

import org.ikigaidigital.application.TimeDepositView;

import java.util.List;

/** Operations offered by the time-deposit application to incoming adapters. */
public interface TimeDepositUseCase {
    void accrueAllBalances();

    List<TimeDepositView> listDeposits();
}
