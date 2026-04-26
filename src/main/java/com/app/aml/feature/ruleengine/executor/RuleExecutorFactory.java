package com.app.aml.feature.ruleengine.executor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RuleExecutorFactory {

    private final Map<String, RuleExecutorStrategy> executorMap;

    @Autowired
    public RuleExecutorFactory(List<RuleExecutorStrategy> strategies) {
        this.executorMap = Collections.unmodifiableMap(
                strategies.stream()
                        .collect(Collectors.toMap(
                                strategy -> strategy.getRuleType().toUpperCase(),
                                strategy -> strategy,
                                (existing, replacement) -> {
                                    log.error("Duplicate RuleExecutorStrategy found for type: {}", existing.getRuleType());
                                    return existing;
                                }
                        ))
        );
        log.info("RuleExecutorFactory initialized with {} strategies: {}",
                executorMap.size(), executorMap.keySet());
    }

    public RuleExecutorStrategy getStrategy(String ruleType) {
        if (ruleType == null) {
            throw new IllegalArgumentException("Rule type cannot be null");
        }

        RuleExecutorStrategy strategy = executorMap.get(ruleType.trim().toUpperCase());

        if (strategy == null) {
            log.error("Execution failed: No strategy found for rule type '{}'", ruleType);
            throw new IllegalArgumentException(
                    String.format("Unsupported Rule Type: '%s'. Available types are: %s",
                            ruleType, executorMap.keySet())
            );
        }
        return strategy;
    }
}