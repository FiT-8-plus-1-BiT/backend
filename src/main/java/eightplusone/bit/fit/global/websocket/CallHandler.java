package eightplusone.bit.fit.global.websocket;

import static eightplusone.bit.fit.global.constants.TokenConstant.*;
import static org.springframework.http.HttpHeaders.*;

import java.io.IOException;

import org.kurento.client.IceCandidate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import eightplusone.bit.fit.domain.auth.jwt.TokenProvider;
import eightplusone.bit.fit.domain.streaming.Room;
import eightplusone.bit.fit.domain.streaming.RoomManager;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CallHandler extends TextWebSocketHandler {
	private static final Gson gson = new Gson();

	private final RoomManager roomManager;
	private final TokenProvider tokenProvider;

	public CallHandler(RoomManager roomManager, TokenProvider tokenProvider) {
		this.roomManager = roomManager;
		this.tokenProvider = tokenProvider;
	}

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);
		log.info("Incoming message from session '{}': {}", session.getId(), jsonMessage);

		String userId = (String)session.getAttributes().get("userId");

		if (jsonMessage.get("id").getAsString().equals(AUTHORIZATION)) {
			String accessToken = jsonMessage.get("accessToken").getAsString().substring(BEARER_PREFIX.length());

			if (tokenProvider.validateAccessToken(accessToken)) {
				Claims claims = tokenProvider.getClaimsByAccessToken(accessToken);
				session.getAttributes().put("userId", claims.getSubject());
				sendResponse(session, AUTHORIZATION, "success");
				log.info("User '{}' has been authenticated and stored for streaming access.", claims.getSubject());
			} else {
				sendError(session, "Invalid access accessToken");
				session.close();
			}
			return;
		}

		if (userId == null) {
			sendError(session, "Unauthorized access - userId missing");
			session.close();
			return;
		}

		String roomName = jsonMessage.get("room").getAsString();
		Room room = roomManager.getRoom(roomName);
		String messageId = jsonMessage.has("id") ? jsonMessage.get("id").getAsString() : "";

		switch (messageId) {
			case "presenter":
				log.info("User '{}' attempting to present in room '{}'", userId, room.getName());
				handlePresenter(session, room, jsonMessage);
				break;
			case "viewer":
				log.info("User '{}' attempting to join as viewer in room '{}'", userId, room.getName());
				handleViewer(session, room, jsonMessage);
				break;
			case "onIceCandidate":
				log.info("User '{}' sending ICE candidate in room '{}'", userId, room.getName());
				handleIceCandidate(session, room, jsonMessage);
				break;
			case "stop":
				log.info("User '{}' stopping session in room '{}'", userId, room.getName());
				handleStop(session, room);
				break;
			default:
				sendError(session, "Invalid message ID");
				break;
		}
	}

	private void handlePresenter(WebSocketSession session, Room room, JsonObject jsonMessage) throws
		IOException,
		InterruptedException {
		UserSession presenter = new UserSession(session);

		if (room.setPresenter(presenter)) {
			String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
			String sdpAnswer = room.processPresenterOffer(sdpOffer);
			sendResponse(session, "presenterResponse", "accepted", "sdpAnswer", sdpAnswer);
		} else {
			sendResponse(session, "presenterResponse", "rejected", "message",
				"A presenter already exists in this room.");
		}
	}

	private void handleViewer(WebSocketSession session, Room room, JsonObject jsonMessage) throws IOException {
		if (!room.hasPresenter()) {
			sendResponse(session, "viewerResponse", "rejected", "message",
				"No presenter available in this room.");
			return;
		}

		UserSession viewer = new UserSession(session);
		if (room.addViewer(viewer, room.getName())) {
			String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
			String sdpAnswer = room.processViewerOffer(viewer, sdpOffer);
			sendResponse(session, "viewerResponse", "accepted", "sdpAnswer", sdpAnswer);
		} else {
			sendResponse(session, "viewerResponse", "rejected");
		}
	}

	private void handleIceCandidate(WebSocketSession session, Room room, JsonObject jsonMessage) {
		IceCandidate candidate = new IceCandidate(
			jsonMessage.get("candidate").getAsJsonObject().get("candidate").getAsString(),
			jsonMessage.get("candidate").getAsJsonObject().get("sdpMid").getAsString(),
			jsonMessage.get("candidate").getAsJsonObject().get("sdpMLineIndex").getAsInt()
		);

		if (room.getPresenterUserSession() != null && room.getPresenterUserSession().getSession().equals(session)) {
			room.getPresenterUserSession().addCandidate(candidate);
			return;
		}

		room.getViewers().stream()
			.filter(v -> v.getSession().equals(session))
			.findFirst().ifPresent(user -> user.addCandidate(candidate));

	}

	private void handleStop(WebSocketSession session, Room room) throws IOException {
		if (room.getPresenterUserSession() != null && room.getPresenterUserSession().getSession().equals(session)) {
			room.removePresenter();
			roomManager.removeRoom(room.getName());
		} else {
			room.removeViewer(session.getId(), room.getName());
		}
		session.close();
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		// 모든 방을 검색하여 해당 세션을 가진 사용자를 삭제
		for (Room room : roomManager.getAllRooms()) {
			if (room.getPresenterUserSession() != null && room.getPresenterUserSession().getSession().equals(session)) {
				room.removePresenter();
				roomManager.removeRoom(room.getName()); // 발표자가 나가면 방 삭제
				return;
			}
			log.info("Checking if {} is a viewer in room {}", session.getId(), room.getName());
			room.removeViewer(session.getId(), room.getName());
		}
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
}