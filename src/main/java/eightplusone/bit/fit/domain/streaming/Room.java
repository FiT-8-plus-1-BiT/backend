package eightplusone.bit.fit.domain.streaming;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PreDestroy;

import org.kurento.client.Continuation;
import org.kurento.client.DtlsConnectionState;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;

import eightplusone.bit.fit.domain.session.service.SessionService;
import eightplusone.bit.fit.global.websocket.UserSession;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Room implements Closeable {
	private final ConcurrentMap<String, UserSession> viewers = new ConcurrentHashMap<>();
	private final MediaPipeline pipeline;

	@Getter
	private final String name;
	@Getter
	private UserSession presenterUserSession; // 발표자 (1명)

	private final SessionService sessionService;

	public Room(String roomName, MediaPipeline pipeline, SessionService sessionService) {
		this.name = roomName;
		this.pipeline = pipeline;
		this.sessionService = sessionService;
		log.info("ROOM {} has been created", roomName);
	}

	@PreDestroy
	private void shutdown() {
		this.close();
	}

	public synchronized boolean setPresenter(UserSession presenter) {
		if (this.presenterUserSession != null) {
			// 이미 발표자가 존재하는 경우
			return false;
		}
		this.presenterUserSession = presenter;

		if (pipeline == null) {
			log.error("Pipeline is NULL before creating WebRtcEndpoint.");
			return false;
		}

		// WebRtcEndpoint 생성
		WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
		presenterUserSession.setWebRtcEndpoint(webRtcEndpoint);

		// MediaSessionStartedListener: 실제 세션이 시작되었을 때 호출
		webRtcEndpoint.addMediaSessionStartedListener(event -> {
			log.info("[{}] Presenter media session started.", name);
			// 필요시 여기에 추가 로직
			// ex) 특정 상태 값 업데이트, 다른 사용자에게 알림 등
		});

		// MediaFlowInStateChangedListener: 미디어가 들어오기 시작(FLOWING)하면 호출
		webRtcEndpoint.addMediaFlowInStateChangedListener(event -> {
			switch (event.getState()) {
				case FLOWING:
					log.info("[{}] Presenter media IN flow is now FLOWING.", name);
					break;
				case NOT_FLOWING:
					log.warn("[{}] Presenter media IN flow is NOT_FLOWING.", name);
					break;
				default:
					break;
			}
		});

		// MediaFlowOutStateChangedListener: 미디어가 나가기 시작(FLOWING)하면 호출
		webRtcEndpoint.addMediaFlowOutStateChangedListener(event -> {
			switch (event.getState()) {
				case FLOWING:
					log.info("[{}] Presenter media OUT flow is now FLOWING.", name);
					break;
				case NOT_FLOWING:
					log.warn("[{}] Presenter media OUT flow is NOT_FLOWING.", name);
					break;
				default:
					break;
			}
		});

		webRtcEndpoint.addIceCandidateFoundListener(event -> {
			String candidateStr = event.getCandidate().getCandidate();
			log.info("[Presenter] ICE candidate found: {}", candidateStr);
		});

		log.info("Presenter WebRtcEndpoint successfully created in room: {}", name);
		return true;
	}

	public synchronized boolean addViewer(UserSession viewer, String roomName) {
		if (this.presenterUserSession == null) {
			// 발표자가 없어 시청자 참가 불가
			return false;
		}
		// WebRtcEndpoint 생성
		WebRtcEndpoint viewerEndpoint = new WebRtcEndpoint.Builder(pipeline).build();

		// 미디어 대역폭 설정 (오디오만 수신 허용 등)
		viewerEndpoint.setMaxVideoRecvBandwidth(0);
		viewerEndpoint.setMaxAudioRecvBandwidth(500);

		// UserSession에 WebRtcEndpoint 연결
		viewer.setWebRtcEndpoint(viewerEndpoint);

		if (viewer.getWebRtcEndpoint() == null) {
			log.error("Viewer WebRtcEndpoint is NULL. Cannot process offer.");
			return false;
		}

		// 이벤트 리스너 등록
		viewerEndpoint.addMediaSessionStartedListener(event -> {
			log.info("[{}] Viewer media session started. sessionId={}",
				this.name, viewer.getSession().getId());
		});

		viewerEndpoint.addMediaFlowInStateChangedListener(event -> {
			switch (event.getState()) {
				case FLOWING:
					log.info("[{}] Viewer IN flow is now FLOWING. sessionId={}",
						this.name, viewer.getSession().getId());
					break;
				case NOT_FLOWING:
					log.warn("[{}] Viewer IN flow is NOT_FLOWING. sessionId={}",
						this.name, viewer.getSession().getId());
					break;
				default:
					break;
			}
		});

		viewerEndpoint.addMediaFlowOutStateChangedListener(event -> {
			switch (event.getState()) {
				case FLOWING:
					log.info("[{}] Viewer OUT flow is now FLOWING. sessionId={}",
						this.name, viewer.getSession().getId());
					break;
				case NOT_FLOWING:
					log.warn("[{}] Viewer OUT flow is NOT_FLOWING. sessionId={}",
						this.name, viewer.getSession().getId());
					break;
				default:
					break;
			}
		});

		viewerEndpoint.addDtlsConnectionStateChangeListener(event -> {
			DtlsConnectionState currentState = event.getState();
			log.info("[Viewer] DTLS state changed to: {}", currentState);
			switch (currentState) {
				case CONNECTED:
					log.info("[Viewer] DTLS CONNECTED for sessionId={}", viewer.getSession().getId());
					break;
				case FAILED:
					log.warn("[Viewer] DTLS FAILED for sessionId={}", viewer.getSession().getId());
					break;
				default:
					break;
			}
		});

		// Presenter -> Viewer 연결
		presenterUserSession.getWebRtcEndpoint().connect(viewerEndpoint);
		log.info("Viewer {} connected to presenter in room {}",
			viewer.getSession().getId(), this.name);

		// viewers 목록에 등록
		viewers.put(viewer.getSession().getId(), viewer);
		log.info("현재 viewer 수: {}", viewers.size());

		// 예시: 세션 정보 업데이트 후 브로드캐스트
		sessionService.updateAndBroadcastIfChanged(Integer.valueOf(roomName));

		return true;
	}

	public synchronized void removeViewer(String sessionId, String roomName) {
		if (viewers.containsKey(sessionId)) {
			viewers.remove(sessionId);
			log.info("Viewer {} removed from room {}", sessionId, name);
			log.info("현재 viewer 수: {}", viewers.size());
			sessionService.updateAndBroadcastIfChanged(Integer.valueOf(roomName));
		} else {
			log.warn("Viewer {} not found in room {}", sessionId, name);
		}
	}

	public synchronized void removePresenter() throws IOException {
		if (presenterUserSession != null) {
			presenterUserSession.close();
			presenterUserSession = null;
		}
		// 발표자가 나가면 모든 시청자도 강제 퇴장
		for (UserSession viewer : viewers.values()) {
			viewer.close();
		}
		viewers.clear();
	}

	public synchronized boolean hasPresenter() {
		return presenterUserSession != null;
	}

	public Collection<UserSession> getViewers() {
		return viewers.values();
	}

	public String processPresenterOffer(String sdpOffer) {
		if (presenterUserSession == null || presenterUserSession.getWebRtcEndpoint() == null) {
			throw new IllegalStateException("Presenter is not set up properly.");
		}
		// SDP Offer 처리
		String sdpAnswer = presenterUserSession.getWebRtcEndpoint().processOffer(sdpOffer);
		//ICE gathering 시작
		presenterUserSession.getWebRtcEndpoint().gatherCandidates();
		// 이제 WebRtcEndpoint가 ICE Candidate를 수용할 준비가 되었다고 표시
		presenterUserSession.markEndpointReady();
		return sdpAnswer;
	}

	public String processViewerOffer(UserSession viewer, String sdpOffer) {
		if (!viewers.containsKey(viewer.getSession().getId())) {
			throw new IllegalStateException("Viewer is not registered in the room.");
		}
		WebRtcEndpoint viewerEndpoint = viewer.getWebRtcEndpoint();
		// SDP Offer 처리
		String sdpAnswer = viewerEndpoint.processOffer(sdpOffer);
		// ICE gathering 시작
		viewerEndpoint.gatherCandidates();
		// viewer endpoint도 준비 완료
		viewer.markEndpointReady();

		return sdpAnswer;
	}

	@Override
	public void close() {
		try {
			removePresenter();
		} catch (IOException e) {
			log.error("Error closing room {}", name, e);
		}
		pipeline.release(new Continuation<Void>() {
			@Override
			public void onSuccess(Void result) {
				log.info("ROOM {}: Released Pipeline", name);
			}

			@Override
			public void onError(Throwable cause) {
				log.warn("ROOM {}: Could not release Pipeline", name);
			}
		});
		log.info("Room {} closed", name);
	}
}
