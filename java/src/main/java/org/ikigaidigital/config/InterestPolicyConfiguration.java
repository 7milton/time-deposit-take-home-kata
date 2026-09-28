package org.ikigaidigital.config;

import org.ikigaidigital.domain.BasicInterestPolicy;
import org.ikigaidigital.domain.InterestPolicy;
import org.ikigaidigital.domain.PremiumInterestPolicy;
import org.ikigaidigital.domain.StudentInterestPolicy;
import org.ikigaidigital.domain.TimeDepositCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class InterestPolicyConfiguration {
    @Bean
    InterestPolicy basicInterestPolicy() {
        return new BasicInterestPolicy();
    }

    @Bean
    InterestPolicy studentInterestPolicy() {
        return new StudentInterestPolicy();
    }

    @Bean
    InterestPolicy premiumInterestPolicy() {
        return new PremiumInterestPolicy();
    }

    @Bean
    TimeDepositCalculator timeDepositCalculator(List<InterestPolicy> policies) {
        return new TimeDepositCalculator(policies);
    }
}
