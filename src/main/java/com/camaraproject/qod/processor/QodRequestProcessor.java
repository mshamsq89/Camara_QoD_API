package com.camaraproject.qod.processor;

import com.camaraproject.qod.model.QosStatus;
import com.camaraproject.qod.model.SessionInfo;
import com.camaraproject.qod.service.NefService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile; // <-- Add this import
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Profile("processor") // <-- THIS IS THE FIX: Only activate this bean for the 'processor' profile
public class QodRequestProcessor {

    private final NefService nefService;
    private final ReactiveRedisTemplate<String, SessionInfo> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String REDIS_KEY_PREFIX = "qod:session:";

    @KafkaListener(topics = "${qod.topics.session-request}", groupId = "qod-processors")
    public void processSessionCreation(ConsumerRecord<String, String> record) {
        log.info("PROCESSOR POD consuming session creation request. Key: {}", record.key());
        try {
            SessionInfo sessionInfo = objectMapper.readValue(record.value(), SessionInfo.class);
            log.info("PROCESSOR POD successfully deserialized session: {}", sessionInfo.getSessionId());
            nefService.createQosPolicy(sessionInfo);

            sessionInfo.setQosStatus(QosStatus.AVAILABLE);
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            sessionInfo.setStartedAt(now);
            sessionInfo.setExpiresAt(now.plusSeconds(sessionInfo.getDuration()));

            String key = REDIS_KEY_PREFIX + sessionInfo.getSessionId();
            redisTemplate.opsForValue().set(key, sessionInfo)
                    .subscribe(v -> log.info("PROCESSOR POD updated session {} to AVAILABLE.", sessionInfo.getSessionId()));

        } catch (Exception e) {
            log.error("PROCESSOR POD failed to process message. Value: {}. Error: {}", record.value(), e.getMessage());
        }
    }

    @KafkaListener(topics = "${qod.topics.session-delete}", groupId = "qod-processors")
    public void processSessionDeletion(ConsumerRecord<String, String> record) {
        // ... same logic as before, just add logging for clarity ...
        log.info("PROCESSOR POD consuming session deletion request. Key: {}", record.key());
        try {
            UUID sessionId = UUID.fromString(record.value());
            // ... rest of the method is the same
        } catch (Exception e) {
            log.error("PROCESSOR POD failed to process deletion message. Value: {}. Error: {}", record.value(), e.getMessage());
        }
    }
}
