package org.ikigaidigital.adapter.in.web;

import org.ikigaidigital.application.TimeDepositView;
import org.ikigaidigital.application.port.in.TimeDepositUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/time-deposits")
public class TimeDepositController {
    private final TimeDepositUseCase deposits;

    public TimeDepositController(TimeDepositUseCase deposits) {
        this.deposits = deposits;
    }

    @PostMapping("/update-balances")
    public ResponseEntity<Void> updateBalances() {
        deposits.accrueAllBalances();
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<TimeDepositView> getAll() {
        return deposits.listDeposits();
    }
}
