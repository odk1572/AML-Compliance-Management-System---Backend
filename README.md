
# Enterprise AML & Fraud Detection System 🛡️

## 1. Project Overview
This project is a Multi-Tenant Anti-Money Laundering (AML) and Transaction Monitoring platform. Designed for financial institutions, it provides an end-to-end automated pipeline for ingesting customer and transaction data, passing it through a highly dynamic SQL-driven Rule Engine, and managing the resulting alerts through a structured Case Management and Investigation workflow, ultimately culminating in Suspicious Transaction Report (STR) filing.

## 2. Architecture & Core Patterns
The system is built as a **Modular Monolith** using Domain-Driven Design (DDD) principles, with a strong emphasis on data isolation and asynchronous processing.

* **Multi-Tenancy Architecture:** Implements a schema-per-tenant model. The `TenantContextFilter` and `TenantAwareDataSource` route traffic and database queries to the specific schema associated with the authenticated user, ensuring strict data isolation.
* **Dynamic Rule Engine (Strategy Pattern):** The core transaction evaluation logic utilizes the Strategy Pattern (`RuleExecutorStrategy`). Different ML-like and heuristic models (e.g., *Structuring*, *Velocity*, *Scatter*, *Funnel*) are dynamically loaded and executed via a `RuleExecutorFactory`.
* **Asynchronous Batch Processing:** High-volume data ingestion is handled by Spring Batch (`AsyncBatchLauncher`, `TransactionIngestionBatchConfig`), utilizing parallel steps and chunk processing for efficient CSV parsing and validation.
* **Event-Driven Workflows:** Leverages Spring Application Events to decouple core transactions from side effects. For example, a `BatchCompletedEvent`, `AlertGeneratedEvent`, or `CaseEscalatedEvent` triggers asynchronous email and in-platform notifications (`NotificationEventListenerImpl`).
* **Centralized Auditing:** Custom annotations (`@AuditAction`) and Spring AOP (`AuditLogAspect`) capture state changes across the application, writing immutable logs to `PlatformAuditLog` and `TenantAuditLog`.

## 3. The End-to-End Execution Flow

The platform's execution lifecycle is divided into five distinct operational phases:

### Phase 1: Data Ingestion & Validation
1. **Upload:** A tenant user uploads a batch of `CustomerProfile` or `Transaction` records (typically via CSV).
2. **Batch Processing:** The `UniversalBatchIngestionService` triggers an asynchronous Spring Batch job.
3. **Validation:** Processors (`TransactionValidationProcessor`, `CustomerProfileValidationProcessor`) validate the raw data against business rules. Invalid records are skipped and logged via `TransactionValidationSkipListener`.
4. **Persistence:** Validated records are bulk-inserted into the tenant's specific database schema. 

### Phase 2: The Rule Engine Evaluation (Transaction Monitoring)
Once data is ingested, the `RuleEngineStep` evaluates the new data against defined global and tenant-specific risk scenarios.
1. **Scenario Orchestration:** The `ScenarioOrchestrationService` fetches active rules applicable to the tenant.
2. **Strategy Execution:** The `RuleExecutorFactory` delegates the evaluation to specific algorithms:
    * *StructuringRuleExecutor:* Detects multiple transactions kept deliberately just below reporting thresholds.
    * *VelocityRuleExecutor:* Flags unusual spikes in transaction frequency over a short interval.
    * *DormantReactivationExecutor:* Identifies sudden large transfers in previously inactive accounts.
    * *GeographicRiskEvaluator:* Scores transactions based on high-risk origin/destination jurisdictions.
    * *(Other strategies include Funnel, Scatter, LowIncomeHighTransfer, etc.)*
3. **Breach Registration:** If a threshold is breached, a `RuleBreachResult` is generated and returned to the execution context.

### Phase 3: Alert Generation & Triage
1. **Alert Creation:** Breaches from Phase 2 automatically generate an `Alert` entity containing `AlertEvidence` and mapped `AlertTransaction` records.
2. **Severity Scoring:** Alerts are categorized by `AlertSeverity` (e.g., LOW, MEDIUM, HIGH) based on the rule configuration.
3. **Dashboarding:** Compliance officers review the queue via the `AlertDashboardService`.
4. **Triage:** The officer can mark the alert as a false positive (closing it) or escalate it by converting it into a formal investigation `Case`.

### Phase 4: Case Management & Investigation
When an alert is deemed severe enough, it transitions to the Case Management bounded context.
1. **Case Assignment:** The `CaseAssignmentService` routes the case to a specific compliance investigator.
2. **Investigation Workbench:** The `TransactionInvestigationService` and `CustomerInvestigationService` provide the investigator with a 360-degree view of the customer's historical activity.
3. **Note Keeping & Audit:** Investigators append immutable `CaseNote` records to document their findings.
4. **Escalation:** If necessary, a L1 investigator can push the case to a L2 senior officer via the `CaseEscalationService`.

### Phase 5: Closure & STR Filing
The lifecycle ends with a formal decision on the case.
1. **Closure Disposition:** The `CaseClosureService` requires a formal disposition (e.g., `FalsePositiveClosureRequest` or `StrClosureRequest`).
2. **STR Document Generation:** If the activity is confirmed as suspicious, the `StrFilingService` is invoked. The `StrDocumentGenerator` compiles all transactional evidence, KYC data, and case notes into a regulatory-compliant format.
3. **Filing Event:** An `StrFiledEvent` is fired, notifying relevant stakeholders and freezing further action on the transaction subset.

## 4. Project Structure (Bounded Contexts)

The repository is modularized into distinct functional domains:

* `aml.multitenency`: Core infrastructure for tenant schema provisioning, resolution, and security context.
* `aml.feature.ruleengine`: The heart of the system. Contains global/tenant scenarios, strategies, and the dynamic SQL interval parsers.
* `aml.feature.ingestion`: Spring Batch configurations, CSV parsing, and async launchers.
* `aml.feature.casemanagement`: Workflows for assignments, escalations, audit trails, and case linkages.
* `aml.feature.alert`: Alert dashboarding, evidence tracking, and status transitions.
* `aml.feature.strfiling`: Regulatory report generation and submission logic.
* `aml.feature.investigation`: Deep-dive data aggregation for compliance officers.
* `aml.security` & `aml.feature.auth`: Unified User Details, JWT provision, JTI blacklisting, and Role-Based Access Control (RBAC).
* `aml.faker`: Utility generators (`TransactionCsvGenerator`, etc.) for seeding databases and testing the rule engine.

## 5. Technology Stack
* **Core Framework:** Java, Spring Boot
* **Data Processing:** Spring Batch
* **Database & Migration:** Relational DB (SQL-driven), Flyway (Tenant-aware schema initialization)
* **Security:** Spring Security, JWT (with Token Blacklisting)
* **Media/Storage:** Cloudinary (Document/Evidence storage)
* **Mapping & Code Reduction:** MapStruct, Lombok
