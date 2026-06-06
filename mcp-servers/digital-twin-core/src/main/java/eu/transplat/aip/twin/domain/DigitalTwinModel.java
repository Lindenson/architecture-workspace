package eu.transplat.aip.twin.domain;

import eu.transplat.aip.mcp.common.Confidence;

import java.time.Instant;
import java.util.List;

/**
 * The merged DIGITAL_TWIN_MODEL: the orchestrator's synthesis of every
 * downstream state slice into one coherent picture of the project. Each
 * sub-state wraps the corresponding downstream slice together with its
 * provenance and freshness.
 *
 * <p>ARCHITECTURE_STATE is live (jQAssistant + Structurizr). KNOWLEDGE_STATE
 * (rag-mcp + wiki-mcp) is an OPTIONAL layer, OFF by default: when disabled it is
 * carried as a {@code DISABLED} sub-state that does not affect confidence.
 *
 * @param deliveryState     DELIVERY_STATE from jira-mcp
 * @param codeState         CODE_STATE from github-mcp
 * @param qualityState      QUALITY_STATE from sonar-mcp
 * @param debtState         DEBT_STATE from sonar-mcp (same slice, debt view)
 * @param architectureState ARCHITECTURE_STATE (live: jQAssistant graph + Structurizr C4 model)
 * @param knowledgeState    KNOWLEDGE_STATE (optional rag-mcp + wiki-mcp; DISABLED when the flag is off)
 * @param overallConfidence aggregated confidence per the resilience rule
 * @param staleSources      sources that could not be refreshed for this snapshot
 * @param recommendations   short rule-derived recommendations
 * @param generatedAt       snapshot timestamp
 */
public record DigitalTwinModel(
        SubState deliveryState,
        SubState codeState,
        SubState qualityState,
        SubState debtState,
        SubState architectureState,
        SubState knowledgeState,
        Confidence overallConfidence,
        List<String> staleSources,
        List<String> recommendations,
        Instant generatedAt) {
}
