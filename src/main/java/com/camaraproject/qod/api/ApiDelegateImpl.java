package com.camaraproject.qod.api;

import com.camaraproject.qod.api.generated.QodApiDelegate;
import com.camaraproject.qod.model.CreateSession;
import com.camaraproject.qod.model.SessionInfo;
import com.camaraproject.qod.service.QodSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * This is the Delegate Implementation.
 * It implements the generated QodApiDelegate interface and contains the real business logic.
 * The generated QodApiController will now find and use this bean instead of returning a hardcoded response.
 */
@Component
@RequiredArgsConstructor
public class ApiDelegateImpl implements QodApiDelegate {

    private final QodSessionService sessionService;

    @Override
    public Mono<ResponseEntity<SessionInfo>> createSession(Mono<CreateSession> createSession, ServerWebExchange exchange) {
        // This is your real logic that was never being called before.
        return createSession
                .flatMap(sessionService::requestSessionCreation)
                .map(sessionInfo -> ResponseEntity.status(HttpStatus.CREATED).body(sessionInfo));
    }

    @Override
    public Mono<ResponseEntity<SessionInfo>> getSession(UUID sessionId, ServerWebExchange exchange) {
        return sessionService.getSession(sessionId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteSession(UUID sessionId, ServerWebExchange exchange) {
        return sessionService.requestSessionDeletion(sessionId)
                .then(Mono.fromCallable(() -> ResponseEntity.noContent().<Void>build()));
    }
}
