package com.app.aml.feature.ruleengine.executor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RuleExecutorFactory {

    private final Map<String, RuleExecutorStrategy> executorMap;

    @Autowired
    public RuleExecutorFactory(List<RuleExecutorStrategy> strategies) {
        this.executorMap = strategies.stream()
                .collect(Collectors.toMap(
                        strategy -> strategy.getRuleType().toUpperCase(),
                        strategy -> strategy
                ));
    }

    public RuleExecutorStrategy getStrategy(String ruleType) {
        RuleExecutorStrategy strategy = executorMap.get(ruleType.toUpperCase());
        if (strategy == null) {
            log.error("No rule executor configured for type: {}", ruleType);
            throw new IllegalArgumentException("Unsupported Rule Type: " + ruleType);
        }
        return strategy;
    }
}