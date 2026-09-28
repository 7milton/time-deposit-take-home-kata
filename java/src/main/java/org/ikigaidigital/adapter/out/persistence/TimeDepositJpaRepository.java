package org.ikigaidigital.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;

public interface TimeDepositJpaRepository extends JpaRepository<TimeDepositEntity, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<TimeDepositEntity> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = "withdrawals")
    List<TimeDepositEntity> findAllWithWithdrawalsByOrderByIdAsc();
}
