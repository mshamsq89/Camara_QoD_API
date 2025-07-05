package com.camaraproject.qod.service;

import com.camaraproject.qod.model.CreateSession;
import com.camaraproject.qod.model.QosStatus;
import com.camaraproject.qod.model.SessionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QodSessionService {

    private final ReactiveRedisTemplate<String, SessionInfo> redisTemplate;
    // The KafkaTemplate now sends <String, Object> where Object can be a SessionInfo or a String
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${qod.topics.session-request}")
    private String sessionRequestTopic;

    @Value("${qod.topics.session-delete}")
    private String sessionDeleteTopic;

    private static final String REDIS_KEY_PREFIX = "qod:session:";

    public Mono<SessionInfo> requestSessionCreation(CreateSession createSession) {
        UUID sessionId = UUID.randomUUID();
        SessionInfo sessionInfo = new SessionInfo()
                .sessionId(sessionId)
                .duration(createSession.getDuration())
                .device(createSession.getDevice())
                .applicationServer(createSession.getApplicationServer())
                .qosProfile(createSession.getQosProfile())
                .qosStatus(QosStatus.REQUESTED);

        String key = REDIS_KEY_PREFIX + sessionId;

        return redisTemplate.opsForValue()
                .set(key, sessionInfo, Duration.ofSeconds(createSession.getDuration() + 600))
                .doOnSuccess(v -> log.info("Session {} stored in Redis with status REQUESTED", sessionId))
                .then(Mono.fromRunnable(() ->
                        // The producer sends the SessionInfo object, which gets JSON serialized
                        kafkaTemplate.send(sessionRequestTopic, sessionId.toString(), sessionInfo)
                ))
                .doOnSuccess(v -> log.info("Published session creation request {} to Kafka", sessionId))
                .thenReturn(sessionInfo);
    }

    public Mono<Void> requestSessionDeletion(UUID sessionId) {
        log.info("Requesting deletion for session {}", sessionId);
        // =======================================================
        // == THE FIX: EXPLICITLY SEND THE SESSION ID AS A STRING ==
        // =======================================================
        return Mono.fromRunnable(() ->
                kafkaTemplate.send(sessionDeleteTopic, sessionId.toString(), sessionId.toString())
        );
    }

    public Mono<SessionInfo> getSession(UUID sessionId) {
        return redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + sessionId);
    }
}
