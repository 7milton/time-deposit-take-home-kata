package org.ikigaidigital.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.TimeDepositView;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaTimeDepositAdapter implements TimeDepositRepository {
    private final TimeDepositJpaRepository deposits;
    private final TimeDepositEntityMapper mapper;

    /**
     * Locks deposits in ID order for the caller's accrual transaction.
     * The caller must keep the transaction open through {@link #saveBalances(List)}.
     *
     * @return deposits to be updated while their database rows remain locked
     */
    @Override
    public List<TimeDeposit> lockAllForAccrual() {
        return deposits.findAllByOrderByIdAsc().stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public void saveBalances(List<TimeDeposit> updated) {
        deposits.saveAll(updated.stream().map(this::withUpdatedBalance).toList());
    }

    @Override
    public List<TimeDepositView> findAll() {
        return deposits.findAllWithWithdrawalsByOrderByIdAsc().stream()
            .map(mapper::toView)
            .toList();
    }

    private TimeDepositEntity withUpdatedBalance(TimeDeposit deposit) {
        TimeDepositEntity entity = deposits.getReferenceById(deposit.getId());
        // Persist cents while retaining the shared Double-based calculator unchanged.
        entity.setBalance(BigDecimal.valueOf(deposit.getBalance())
            .setScale(2, RoundingMode.HALF_UP));
        return entity;
    }
}
