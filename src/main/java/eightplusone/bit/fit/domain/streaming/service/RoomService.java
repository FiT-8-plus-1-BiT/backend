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
}
