package org.ikigaidigital.adapter.out.persistence;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@Profile("demo")
public class DemoDataInitializer implements ApplicationRunner {
    private final TimeDepositJpaRepository deposits;
    private final WithdrawalJpaRepository withdrawals;

    public DemoDataInitializer(TimeDepositJpaRepository deposits, WithdrawalJpaRepository withdrawals) {
        this.deposits = deposits;
        this.withdrawals = withdrawals;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (deposits.count() > 0) {
            return;
        }

        deposits.save(TimeDepositEntity.builder()
            .planType("student").balance(new BigDecimal("500.00")).days(100).build());
        deposits.save(TimeDepositEntity.builder()
            .planType("premium").balance(new BigDecimal("10000.00")).days(60).build());

        TimeDepositEntity basic = deposits.save(TimeDepositEntity.builder()
            .planType("basic").balance(new BigDecimal("1000.00")).days(45).build());
        withdrawals.save(WithdrawalEntity.builder()
            .deposit(basic).amount(new BigDecimal("25.00")).date(LocalDate.of(2026, 1, 15)).build());
    }
}
