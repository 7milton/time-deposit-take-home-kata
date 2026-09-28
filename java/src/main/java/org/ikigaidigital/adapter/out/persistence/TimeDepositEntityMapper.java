package org.ikigaidigital.adapter.out.persistence;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.TimeDepositView;
import org.ikigaidigital.application.WithdrawalView;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TimeDepositEntityMapper {
    TimeDeposit toDomain(TimeDepositEntity entity);

    TimeDepositView toView(TimeDepositEntity entity);

    WithdrawalView toWithdrawalView(WithdrawalEntity entity);
}
