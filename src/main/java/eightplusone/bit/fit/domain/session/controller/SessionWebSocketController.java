package eightplusone.bit.fit.domain.session.controller;

import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RestController;

import eightplusone.bit.fit.domain.session.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SessionWebSocketController {

	private final SimpMessagingTemplate messagingTemplate;
	private final SessionService sessionService;

	@Scheduled(fixedRate = 30000)
	public void broadcastSessionUpdate() {
		Map<Integer, Map<String, Object>> sessionData = sessionService.getUpdatedSessionData();
		log.info("send congestion data : {}", sessionData.toString());
		messagingTemplate.convertAndSend("/sub/ws-room", sessionData);
	}
}
