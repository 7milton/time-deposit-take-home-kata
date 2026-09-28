package org.ikigaidigital.application.port.out;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.TimeDepositView;

import java.util.List;

/** Persistence operations required by the time-deposit use cases. */
public interface TimeDepositRepository {
    /** Loads existing deposits with row locks held by the caller's accrual transaction. */
    List<TimeDeposit> lockAllForAccrual();

    void saveBalances(List<TimeDeposit> deposits);

    List<TimeDepositView> findAll();
}
