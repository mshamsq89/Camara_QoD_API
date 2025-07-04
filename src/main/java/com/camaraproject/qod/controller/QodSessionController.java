package com.camaraproject.qod.controller;

import com.camaraproject.qod.api.SessionsApi;
import com.camaraproject.qod.model.CreateSession;
import com.camaraproject.qod.model.SessionInfo;
import com.camaraproject.qod.service.QodSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class QodSessionController implements SessionsApi {

    private final QodSessionService sessionService;

    @Override
    public Mono<ResponseEntity<SessionInfo>> createSession(Mono<CreateSession> createSession, ServerWebExchange exchange) {
        return createSession
                .flatMap(sessionService::requestSessionCreation)
                .map(sessionInfo -> ResponseEntity.status(HttpStatus.ACCEPTED).body(sessionInfo));
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
                .then(Mono.fromCallable(() -> ResponseEntity.noContent().build()));
    }
}
