//package com.app.aml.feature.ingestion.batch.step;
//
//
//import com.app.aml.domain.enums.RuleStatus;
//import com.app.aml.feature.notification.service.interfaces.NotificationService;
//import com.app.aml.feature.ruleengine.entity.TenantScenario;
//import com.app.aml.feature.ruleengine.repository.TenantScenarioRepository;
//import com.app.aml.feature.ruleengine.service.ScenarioOrchestrationService;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.batch.core.StepContribution;
//import org.springframework.batch.core.scope.context.ChunkContext;
//import org.springframework.batch.core.step.tasklet.Tasklet;
//import org.springframework.batch.repeat.RepeatStatus;
//import org.springframework.stereotype.Component;
//
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//import java.util.UUID;
//
//@Component
//@RequiredArgsConstructor
//public class RuleEngineStep implements Tasklet {
//
//    private final TenantScenarioRepository scenarioRepo;
//    private final ScenarioOrchestrationService orchestrationService;
//    private final NotificationService notifService;
//
//    @Override
//    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
//        List<TenantScenario> activeScenarios = scenarioRepo.findByStatus(RuleStatus.ACTIVE);
//        Set<UUID> allBreachingCustomers = new HashSet<>();
//
//        for (TenantScenario scenario : activeScenarios) {
//            Set<UUID> breaches = orchestrationService.executeFullScenario(scenario.getId());
//            allBreachingCustomers.addAll(breaches);
//        }
//
//        if (!allBreachingCustomers.isEmpty()) {
//            notifService.sendInternalAlert("Batch Execution: Found " + allBreachingCustomers.size() + " unique breaching customers.");
//        }
//
//        return RepeatStatus.FINISHED;
//    }
//}