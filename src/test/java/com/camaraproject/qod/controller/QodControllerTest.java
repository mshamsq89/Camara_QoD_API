package com.camaraproject.qod.controller;

import com.camaraproject.qod.model.CreateSession;
import com.camaraproject.qod.model.QosProfileName;
import com.camaraproject.qod.model.SessionInfo;
import com.camaraproject.qod.service.QodSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest(QodController.class)
public class QodControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private QodSessionService sessionService;

    @Test
    void createSession_shouldReturnAccepted() {
        CreateSession request = new CreateSession().duration(3600).qosProfile(QosProfileName.QOS_L);
        SessionInfo response = new SessionInfo().sessionId(UUID.randomUUID());

        when(sessionService.requestSessionCreation(any(CreateSession.class))).thenReturn(Mono.just(response));

        webTestClient
                .mutateWith(csrf())
                .mutateWith(mockJwt().jwt(jwt -> jwt.claim("scope", "quality-on-demand:sessions:create")))
                .post()
                .uri("/qod/v1/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), CreateSession.class)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(SessionInfo.class);
    }
}
