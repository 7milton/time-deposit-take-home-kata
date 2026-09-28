package org.ikigaidigital.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "time_deposits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimeDepositEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "plan_type", nullable = false, length = 64)
    private String planType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false)
    private int days;

    @OneToMany(mappedBy = "deposit")
    @OrderBy("id ASC")
    private List<WithdrawalEntity> withdrawals = new ArrayList<>();

    @Builder
    private TimeDepositEntity(String planType, BigDecimal balance, int days) {
        this.planType = planType;
        this.balance = balance;
        this.days = days;
    }

    void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
