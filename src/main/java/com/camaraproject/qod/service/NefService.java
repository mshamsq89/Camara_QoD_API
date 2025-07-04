package com.camaraproject.qod.service;

import com.camaraproject.qod.model.SessionInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NefService {
    public void createQosPolicy(SessionInfo session) {
        log.info("--> [NEF -> 5G Core] Creating QoS Policy for session: {}. Profile: {}", session.getSessionId(), session.getQosProfile());
        // Simulate network interaction latency
        try {
            Thread.sleep(50); // 50ms latency
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("<-- [NEF <- 5G Core] Policy for session {} created.", session.getSessionId());
    }

    public void deleteQosPolicy(SessionInfo session) {
        log.info("--> [NEF -> 5G Core] Deleting QoS Policy for session: {}.", session.getSessionId());
        log.info("<-- [NEF <- 5G Core] Policy for session {} deleted.", session.getSessionId());
    }
}
