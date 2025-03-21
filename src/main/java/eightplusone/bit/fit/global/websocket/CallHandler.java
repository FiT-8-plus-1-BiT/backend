package eightplusone.bit.fit.global.websocket;

import static eightplusone.bit.fit.global.constants.TokenConstant.*;
import static org.springframework.http.HttpHeaders.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.kurento.client.IceCandidate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import eightplusone.bit.fit.domain.auth.jwt.TokenProvider;
import eightplusone.bit.fit.domain.streaming.Room;
import eightplusone.bit.fit.domain.streaming.RoomManager;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CallHandler extends TextWebSocketHandler {
	private static final Gson gson = new Gson();

	private final RoomManager roomManager;
	private final TokenProvider tokenProvider;

	// 세션 및 Pong 응답 시간 기록을 위한 맵
	private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, Long> lastPongTimestamps = new ConcurrentHashMap<>();

	// Ping 및 Timeout 체크를 위한 스케줄러
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private final long PING_INTERVAL = 30; // Ping 전송 주기 (초)
	private final long TIMEOUT_THRESHOLD = 60000; // Pong 응답 타임아웃 기준 (밀리초)

	public CallHandler(RoomManager roomManager, TokenProvider tokenProvider) {
		this.roomManager = roomManager;
		this.tokenProvider = tokenProvider;
	}

	/**
	 * 클라이언트로부터 텍스트 메시지 수신 시 처리 로직.
	 */
	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);
		log.info("수신 메시지: sessionId={}, payload={}", session.getId(), jsonMessage);

		String userId = (String)session.getAttributes().get("userId");

		// 인증 메시지 처리
		if (jsonMessage.get("id").getAsString().equals(AUTHORIZATION)) {
			String accessToken = jsonMessage.get("accessToken").getAsString()
				.substring(BEARER_PREFIX.length());

			if (tokenProvider.validateAccessToken(accessToken)) {
				Claims claims = tokenProvider.getClaimsByAccessToken(accessToken);
				session.getAttributes().put("userId", claims.getSubject());
				sendResponse(session, AUTHORIZATION, "success");
				log.info("사용자 인증 성공: userId={}", claims.getSubject());
			} else {
				sendError(session, "Invalid accessToken");
				log.warn("인증 실패: 잘못된 accessToken, sessionId={}", session.getId());
				session.close();
			}
			return;
		}

		// 인증되지 않은 요청 처리
		if (userId == null) {
			sendError(session, "Unauthorized access - userId missing");
			log.warn("인증되지 않은 접근: sessionId={}", session.getId());
			session.close();
			return;
		}

		// 메시지에 따른 방 및 역할 처리
		String roomName = jsonMessage.get("room").getAsString();
		Room room = roomManager.getRoom(roomName);
		String messageId = jsonMessage.has("id") ? jsonMessage.get("id").getAsString() : "";

		switch (messageId) {
			case "presenter":
				log.info("Presenter 요청: userId={}, roomName={}", userId, room.getName());
				handlePresenter(session, room, jsonMessage);
				break;
			case "viewer":
				log.info("Viewer 요청: userId={}, roomName={}", userId, room.getName());
				handleViewer(session, room, jsonMessage);
				break;
			case "onIceCandidate":
				log.info("ICE Candidate 요청: userId={}, roomName={}", userId, room.getName());
				handleIceCandidate(session, room, jsonMessage);
				break;
			case "stop":
				log.info("Stop 요청: userId={}, roomName={}", userId, room.getName());
				handleStop(session, room);
				break;
			default:
				sendError(session, "Invalid message ID");
				log.warn("잘못된 메시지 ID: sessionId={}, messageId={}", session.getId(), messageId);
				break;
		}
	}

	/**
	 * Presenter 역할 요청 처리.
	 */
	private void handlePresenter(WebSocketSession session, Room room, JsonObject jsonMessage)
		throws IOException, InterruptedException {
		UserSession presenter = new UserSession(session);
		if (room.setPresenter(presenter)) {
			String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
			String sdpAnswer = room.processPresenterOffer(sdpOffer);
			sendResponse(session, "presenterResponse", "accepted", "sdpAnswer", sdpAnswer);
			log.info("Presenter 요청 승인: sessionId={}, roomName={}", session.getId(), room.getName());
		} else {
			sendResponse(session, "presenterResponse", "rejected", "message",
				"A presenter already exists in this room.");
			log.info("Presenter 요청 거부(이미 존재): sessionId={}, roomName={}", session.getId(), room.getName());
		}
	}

	/**
	 * Viewer 역할 요청 처리.
	 */
	private void handleViewer(WebSocketSession session, Room room, JsonObject jsonMessage) throws IOException {
		if (!room.hasPresenter()) {
			sendResponse(session, "viewerResponse", "rejected", "message",
				"No presenter available in this room.");
			log.warn("Viewer 요청 거부: presenter 없음, sessionId={}, roomName={}", session.getId(), room.getName());
			return;
		}

		UserSession viewer = new UserSession(session);
		if (room.addViewer(viewer, room.getName())) {
			String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
			String sdpAnswer = room.processViewerOffer(viewer, sdpOffer);
			sendResponse(session, "viewerResponse", "accepted", "sdpAnswer", sdpAnswer);
			log.info("Viewer 요청 승인: sessionId={}, roomName={}", session.getId(), room.getName());
		} else {
			sendResponse(session, "viewerResponse", "rejected");
			log.error("Viewer 추가 실패: sessionId={}, roomName={}", session.getId(), room.getName());
		}
	}

	/**
	 * ICE Candidate 메시지 처리.
	 */
	private void handleIceCandidate(WebSocketSession session, Room room, JsonObject jsonMessage) {
		IceCandidate candidate = new IceCandidate(
			jsonMessage.get("candidate").getAsJsonObject().get("candidate").getAsString(),
			jsonMessage.get("candidate").getAsJsonObject().get("sdpMid").getAsString(),
			jsonMessage.get("candidate").getAsJsonObject().get("sdpMLineIndex").getAsInt()
		);
		log.info("ICE Candidate 수신: sessionId={}, roomName={}, candidate={}",
			session.getAttributes().get("userId"), room.getName(), candidate.getCandidate());

		// 발표자와 시청자 구분하여 ICE candidate 추가 처리
		if (room.getPresenterUserSession() != null && room.getPresenterUserSession().getSession().equals(session)) {
			room.getPresenterUserSession().addCandidate(candidate);
			log.debug("발표자 ICE Candidate 등록: roomName={}", room.getName());
		} else {
			room.getViewers().stream()
				.filter(v -> v.getSession().equals(session))
				.findFirst()
				.ifPresent(user -> {
					user.addCandidate(candidate);
					log.debug("시청자 ICE Candidate 등록: roomName={}", room.getName());
				});
		}
	}

	/**
	 * 세션 종료 요청 처리 및 방/세션 정리.
	 */
	private void handleStop(WebSocketSession session, Room room) throws IOException {
		if (room.getPresenterUserSession() != null && room.getPresenterUserSession().getSession().equals(session)) {
			room.removePresenter();
			roomManager.removeRoom(room.getName());
			log.info("Presenter 종료로 인한 방 삭제: roomName={}", room.getName());
		} else {
			room.removeViewer(session.getId(), room.getName());
			log.info("Viewer 종료 처리: sessionId={}, roomName={}", session.getId(), room.getName());
		}
		session.close();
	}

	private void sendResponse(WebSocketSession session, String id, String response) {
		sendResponse(session, id, response, null, null);
	}

	private void sendResponse(WebSocketSession session, String id, String response, String key, String value) {
		try {
			JsonObject json = new JsonObject();
			json.addProperty("id", id);
			json.addProperty("response", response);
			if (key != null && value != null) {
				json.addProperty(key, value);
			}
			session.sendMessage(new TextMessage(gson.toJson(json)));
		} catch (IOException e) {
			log.error("Failed to send response to session {}: {}", session.getId(), e.getMessage(), e);
		}
	}

	private void sendError(WebSocketSession session, String errorMessage) {
		sendResponse(session, "error", "rejected", "message", errorMessage);
	}

	/**
	 * @PostConstruct 어노테이션을 통해 Bean 초기화 후 Keep-Alive 관련 작업을 시작합니다.
	 */
	@PostConstruct
	public void initKeepAliveTasks() {
		startPingTask();
		startTimeoutChecker();
		log.info("Keep-Alive 작업 시작: PingInterval={}초, TimeoutThreshold={}ms", PING_INTERVAL, TIMEOUT_THRESHOLD);
	}

	/**
	 * WebSocket 연결이 성립될 때 호출되어 세션을 등록합니다.
	 */
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		sessions.put(session.getId(), session);
		lastPongTimestamps.put(session.getId(), System.currentTimeMillis());
		log.info("WebSocket 연결 성립: sessionId={}", session.getId());
	}

	/**
	 * WebSocket 연결 종료 시 세션 제거 및 해당 세션이 포함된 방의 정리를 수행합니다.
	 */
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		sessions.remove(session.getId());
		lastPongTimestamps.remove(session.getId());
		log.info("WebSocket 연결 종료: sessionId={}, status={}", session.getId(), status);

		// 해당 세션이 참여한 모든 방에 대해 정리 처리
		for (Room room : roomManager.getAllRooms()) {
			if (room.getPresenterUserSession() != null &&
				room.getPresenterUserSession().getSession().equals(session)) {
				log.info("Presenter session 종료로 인해 방 삭제: roomName={}", room.getName());
				room.removePresenter();
				roomManager.removeRoom(room.getName());
				return;
			}
			room.removeViewer(session.getId(), room.getName());
		}
	}

	/**
	 * 모든 활성 세션에 대해 주기적으로 Ping 메시지를 전송합니다.
	 */
	private void startPingTask() {
		scheduler.scheduleAtFixedRate(() -> {
			sessions.forEach((id, session) -> {
				try {
					if (session.isOpen()) {
						session.sendMessage(new PingMessage(ByteBuffer.wrap("ping".getBytes())));
						log.debug("Ping 전송: sessionId={}", id);
					}
				} catch (IOException e) {
					log.error("Ping 전송 실패: sessionId={}, error={}", id, e.getMessage());
					closeSessionQuietly(session);
				}
			});
		}, PING_INTERVAL, PING_INTERVAL, TimeUnit.SECONDS);
	}

	/**
	 * 각 세션별로 마지막 Pong 응답 시간을 확인하여 타임아웃된 세션을 종료합니다.
	 */
	private void startTimeoutChecker() {
		scheduler.scheduleAtFixedRate(() -> {
			long now = System.currentTimeMillis();
			sessions.forEach((id, session) -> {
				Long lastPong = lastPongTimestamps.get(id);
				if (lastPong == null || (now - lastPong) > TIMEOUT_THRESHOLD) {
					log.warn("Pong 미응답으로 타임아웃: sessionId={}", id);
					closeSessionQuietly(session);
					sessions.remove(id);
					lastPongTimestamps.remove(id);
				}
			});
		}, TIMEOUT_THRESHOLD, TIMEOUT_THRESHOLD, TimeUnit.MILLISECONDS);
	}

	/**
	 * Pong 메시지 수신 시 마지막 응답 시간을 갱신합니다.
	 */
	@Override
	protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
		lastPongTimestamps.put(session.getId(), System.currentTimeMillis());
		log.info("Received pong from session: {}", session.getId());
	}

	/**
	 * 예외 발생 시 세션을 안전하게 종료하는 헬퍼 메서드.
	 */
	private void closeSessionQuietly(WebSocketSession session) {
		try {
			if (session.isOpen()) {
				session.close();
				log.info("세션 종료: sessionId={}", session.getId());
			}
		} catch (IOException e) {
			log.error("세션 종료 실패: sessionId={}, error={}", session.getId(), e.getMessage());
		}
	}
}