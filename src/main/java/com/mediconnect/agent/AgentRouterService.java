package com.mediconnect.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AgentRouterService {

    private static final Logger log = LoggerFactory.getLogger(AgentRouterService.class);

    public enum AgentType {
        BOOKING_AGENT,
        POLICY_RAG_AGENT,
        GENERAL_GREETING_AGENT
    }

    public AgentType routeRequest(String prompt) {
        String lower = prompt.toLowerCase().trim();

        // Check Greetings
        if (lower.matches("^(hello|hi|hey|greetings|good morning|good afternoon|good evening|who are you|help)$")) {
            log.info("🔀 Router: Identified GENERAL_GREETING_AGENT");
            return AgentType.GENERAL_GREETING_AGENT;
        }

        // Check Booking intent
        if (lower.contains("book") || lower.contains("confirm") || lower.contains("appointment")
                || lower.contains("slot") || lower.contains("doctor") || lower.contains("schedule")
                || lower.contains("cancel") || lower.contains("history")) {
            log.info("🔀 Router: Identified BOOKING_AGENT");
            return AgentType.BOOKING_AGENT;
        }

        // Check Policy / Copay / PDF FAQ intent
        if (lower.contains("pay") || lower.contains("cost") || lower.contains("fee") || lower.contains("price")
                || lower.contains("copay") || lower.contains("coverage") || lower.contains("insurance")
                || lower.contains("policy") || lower.contains("pdf") || lower.contains("document") || lower.contains("how much")) {
            log.info("🔀 Router: Identified POLICY_RAG_AGENT");
            return AgentType.POLICY_RAG_AGENT;
        }

        // Default to Policy/RAG if clinical, else Booking
        if (lower.contains("headache") || lower.contains("heart") || lower.contains("skin") || lower.contains("pain")) {
            log.info("🔀 Router: Clinical Query -> Delegating to BOOKING_AGENT");
            return AgentType.BOOKING_AGENT;
        }

        log.info("🔀 Router: Defaulting to POLICY_RAG_AGENT");
        return AgentType.POLICY_RAG_AGENT;
    }
}
