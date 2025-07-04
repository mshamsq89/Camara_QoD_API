package com.camaraproject.qod.processor;

import com.camaraproject.qod.model.QosStatus;
import com.camaraproject.qod.model.SessionInfo;
import com.camaraproject.qod.service.NefService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@Slf4j
@RequiredArgsConstructor
public class QodRequestProcessor {

    private final NefService nefService;
    private final ReactiveRedisTemplate<String, SessionInfo> redisTemplate;
    private static final String REDIS_KEY_PREFIX = "qod:session:";

    @KafkaListener(topics = "${qod.topics.session-request}", groupId = "qod-processors")
    public void processSessionCreation(@Payload SessionInfo sessionInfo) {
        log.info("Consumed session creation request from Kafka: {}", sessionInfo.getSessionId());
        try {
            nefService.createQosPolicy(sessionInfo);

            sessionInfo.setQosStatus(QosStatus.AVAILABLE);
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            sessionInfo.setStartedAt(now);
            sessionInfo.setExpiresAt(now.plusSeconds(sessionInfo.getDuration()));

            String key = REDIS_KEY_PREFIX + sessionInfo.getSessionId();
            redisTemplate.opsForValue().set(key, sessionInfo)
                    .subscribe(v -> log.info("Updated session {} status to AVAILABLE in Redis.", sessionInfo.getSessionId()));
        } catch (Exception e) {
            log.error("Error processing session creation for {}: {}", sessionInfo.getSessionId(), e.getMessage());
            // In production, publish to a dead-letter queue (DLQ)
        }
    }

    @KafkaListener(topics = "${qod.topics.session-delete}", groupId = "qod-processors")
    public void processSessionDeletion(@Payload String sessionId) {
        String key = REDIS_KEY_PREFIX + sessionId;
        log.info("Consumed session deletion request from Kafka: {}", sessionId);
        redisTemplate.opsForValue().get(key)
                .doOnNext(nefService::deleteQosPolicy)
                .flatMap(sessionInfo -> redisTemplate.delete(key))
                .subscribe(v -> log.info("Session {} and its policies deleted.", sessionId));
    }
}
