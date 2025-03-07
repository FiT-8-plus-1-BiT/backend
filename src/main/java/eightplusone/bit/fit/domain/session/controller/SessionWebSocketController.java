package eightplusone.bit.fit.domain.session.controller;

import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eightplusone.bit.fit.domain.session.service.SessionService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/congestion")
@RequiredArgsConstructor
public class SessionWebSocketController {

	private final SimpMessagingTemplate messagingTemplate;
	private final SessionService sessionService;

	@MessageMapping("/session")
	public void sendRoomUpdate(Map<String, Object> roomData) {
		messagingTemplate.convertAndSend("/sub/session", roomData);
	}

	@Scheduled(fixedRate = 10000)
	public void broadcastSessionUpdate() {
		Map<Long, Map<String, Object>> sessionData = sessionService.getUpdatedSessionData();
		messagingTemplate.convertAndSend("/sub/session", sessionData);
	}
}
