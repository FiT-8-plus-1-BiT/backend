package eightplusone.bit.fit.domain.streaming.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.client.IceCandidate;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import eightplusone.bit.fit.domain.session.service.SessionService;
import eightplusone.bit.fit.domain.streaming.dto.IceCandidateDto;
import eightplusone.bit.fit.domain.streaming.entity.AudioRoom;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RoomService {

	private final KurentoService kurentoService;

	private final Map<String, AudioRoom> rooms = new ConcurrentHashMap<>();
	private final SimpMessagingTemplate messagingTemplate;

	private final SessionService sessionService;

	public RoomService(KurentoService kurentoService, SimpMessagingTemplate messagingTemplate,
		SessionService sessionService) {
		this.kurentoService = kurentoService;
		this.messagingTemplate = messagingTemplate;
		this.sessionService = sessionService;
	}

	public synchronized AudioRoom getOrCreateRoom(String roomId) {
		AudioRoom room = rooms.get(roomId);
		if (room == null) {
			// 새 MediaPipeline 생성
			MediaPipeline pipeline = kurentoService.createPipeline();
			room = new AudioRoom(roomId, pipeline);
			rooms.put(roomId, room);
		}
		return room;
	}

	public String processPresenterOffer(String roomId, String sdpOffer) {
		AudioRoom room = getOrCreateRoom(roomId);
		// 이미 발표자가 있다면 재설정하거나, 없으면 새로 생성
		WebRtcEndpoint presenterEndpoint = room.getPresenterEndpoint();
		// 기존 Endpoint가 있으면 release
		if (presenterEndpoint != null) {
			room.getPresenterEndpoint().release();
		}

		presenterEndpoint = kurentoService.createWebRtcEndpoint(room.getPipeline());
		room.setPresenterEndpoint(presenterEndpoint);

		// 서버 -> 클라이언트 ICE 후보 전송 (발견 시 이벤트)
		presenterEndpoint.addIceCandidateFoundListener(event -> {
			IceCandidate candidate = event.getCandidate();
			// ICE 후보를 클라이언트로 보내야 하기에
			// STOMP를 통해 /sub/room/{roomId}/iceCandidate 로 전송
			IceCandidateDto dto = IceCandidateDto.from(candidate);
			log.info("[Presenter][addIceCandidateFoundListener]Received candidate: {}", dto);
			messagingTemplate.convertAndSend(
				"/sub/room/" + roomId + "/presenterIceCandidate",
				dto
			);

		});
		// SDP Offer 처리 -> SDP Answer 생성
		String sdpAnswer = presenterEndpoint.processOffer(sdpOffer);
		// ICE candidate 수신 대기
		presenterEndpoint.gatherCandidates();

		return sdpAnswer;
	}

	public void addPresenterIceCandidate(String roomId, IceCandidate candidate) {
		AudioRoom room = rooms.get(roomId);
		if (room != null && room.getPresenterEndpoint() != null) {
			// 발표자가 보낸 ICE 후보를 Kurento에 추가
			room.getPresenterEndpoint().addIceCandidate(candidate);
		}
	}

	public String processAudienceOffer(String roomId, String audienceEmail, String sdpOffer) {
		AudioRoom room = getOrCreateRoom(roomId);

		WebRtcEndpoint presenterEndpoint = room.getPresenterEndpoint();

		Map<String, WebRtcEndpoint> audienceMap = room.getAudienceEndpoints();
		if (audienceMap.containsKey(audienceEmail)) {
			WebRtcEndpoint oldEndpoint = audienceMap.get(audienceEmail);
			oldEndpoint.release();
			audienceMap.remove(audienceEmail);
		}

		// 새 청중 WebRtcEndpoint 생성
		WebRtcEndpoint audienceEndpoint = kurentoService.createWebRtcEndpoint(room.getPipeline());
		room.getAudienceEndpoints().put(audienceEmail, audienceEndpoint);

		// 발표자 -> 청중 연결
		if (presenterEndpoint != null) {
			presenterEndpoint.connect(audienceEndpoint);
		}

		// 청중 ICE 후보 -> 클라이언트
		audienceEndpoint.addIceCandidateFoundListener(event -> {
			IceCandidate candidate = event.getCandidate();
			IceCandidateDto dto = IceCandidateDto.from(candidate);

			// /sub/room/{roomId}/audience/iceCandidate
			messagingTemplate.convertAndSend(
				"/sub/room/" + roomId + "/audience/iceCandidate",
				dto
			);
		});

		String sdpAnswer = audienceEndpoint.processOffer(sdpOffer);
		audienceEndpoint.gatherCandidates();
		return sdpAnswer;
	}

	// 청중 ICE 추가
	public void addAudienceIceCandidate(String roomId, String audienceEmail, IceCandidate candidate) {
		AudioRoom room = rooms.get(roomId);
		if (room == null) {
			return;
		}

		WebRtcEndpoint endpoint = room.getAudienceEndpoints().get(audienceEmail);
		if (endpoint != null) {
			log.info("[Audience] addIceCandidate: {}", candidate);
			endpoint.addIceCandidate(candidate);
		}
	}

	// 발표자 퇴장 -> 청중 모두 연결 끊어짐.
	public void leavePresenter(String roomId) {
		AudioRoom room = rooms.get(roomId);
		if (room != null) {
			// 발표자 엔드포인트 해제
			if (room.getPresenterEndpoint() != null) {
				room.getPresenterEndpoint().release();
				room.setPresenterEndpoint(null);
				log.info("[Presenter] Released presenter endpoint for room: {}", roomId);
			}
			// 모든 청중 엔드포인트 해제
			for (Map.Entry<String, WebRtcEndpoint> entry : room.getAudienceEndpoints().entrySet()) {
				entry.getValue().release();
				log.info("[Audience] Released audience endpoint for {} in room: {}", entry.getKey(), roomId);
			}
			room.getAudienceEndpoints().clear();

			// MediaPipeline 해제 후 방 제거
			room.getPipeline().release();
			rooms.remove(roomId);
			log.info("[Room] Removed room {} because presenter left and all audience endpoints were released.", roomId);
		}
	}

	// 청중 퇴장
	public void leaveAudience(String roomId, String audienceEmail) {
		AudioRoom room = rooms.get(roomId);
		if (room != null) {
			WebRtcEndpoint endpoint = room.getAudienceEndpoints().get(audienceEmail);
			if (endpoint != null) {
				endpoint.release();
				room.getAudienceEndpoints().remove(audienceEmail);
				log.info("[Audience] Released audience endpoint for {} in room: {}", audienceEmail, roomId);
			}
			// 발표자도 없고 청중도 없으면 MediaPipeline 해제 후 방 제거
			if (room.getPresenterEndpoint() == null && room.getAudienceEndpoints().isEmpty()) {
				room.getPipeline().release();
				rooms.remove(roomId);
				log.info("[Room] Removed room {} as no participants remain.", roomId);
			}
		}
	}

}
