package org.ikigaidigital.application;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WithdrawalView(int id, BigDecimal amount, LocalDate date) {
}
