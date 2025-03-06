package eightplusone.bit.fit.domain.streaming;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PreDestroy;

import org.kurento.client.Continuation;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;

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

	public Room(String roomName, MediaPipeline pipeline) {
		this.name = roomName;
		this.pipeline = pipeline;
		log.info("ROOM {} has been created", roomName);
	}

	@PreDestroy
	private void shutdown() {
		this.close();
	}

	public synchronized boolean setPresenter(UserSession presenter) throws InterruptedException {
		if (this.presenterUserSession == null) {
			this.presenterUserSession = presenter;
			if (pipeline == null) {
				log.error("Pipeline is NULL before creating WebRtcEndpoint.");
				return false;
			}
			WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
			presenterUserSession.setWebRtcEndpoint(webRtcEndpoint);
			Thread.sleep(100);
			if (presenterUserSession.getWebRtcEndpoint() == null) {
				log.error("WebRtcEndpoint is NULL after creation. Something went wrong.");
				return false;
			}

			log.info("Presenter WebRtcEndpoint successfully created in room: {}", name);
			return true;
		}
		return false;
	}

	public synchronized boolean addViewer(UserSession viewer) {
		if (this.presenterUserSession == null) {
			return false; // 발표자가 있어야만 시청자가 참가 가능
		}
		// WebRtcEndpoint 생성 추가
		WebRtcEndpoint viewerEndpoint = new WebRtcEndpoint.Builder(pipeline).build();

		viewerEndpoint.setMaxVideoRecvBandwidth(0); // 비디오 수신 완전 차단
		viewerEndpoint.setMaxAudioRecvBandwidth(500); // 오디오 수신 허용(고음질 음성 통화 수준)

		viewer.setWebRtcEndpoint(viewerEndpoint);

		if (viewer.getWebRtcEndpoint() == null) {
			log.error("Viewer WebRtcEndpoint is NULL. Cannot process offer.");
			return false;
		}

		presenterUserSession.getWebRtcEndpoint().connect(viewerEndpoint); // Presenter와 연결
		log.info("Viewer {} connected to presenter in room {}", viewer.getSession().getId(), name);
		viewers.put(viewer.getSession().getId(), viewer);
		log.info("현재 viewer 수: {}", viewers.size());
		return true;
	}

	public synchronized void removeViewer(String sessionId) {
		if (viewers.containsKey(sessionId)) {
			viewers.remove(sessionId);
			log.info("Viewer {} removed from room {}", sessionId, name);
			log.info("현재 viewer 수: {}", viewers.size());
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
		String sdpAnswer = presenterUserSession.getWebRtcEndpoint().processOffer(sdpOffer);
		presenterUserSession.getWebRtcEndpoint().gatherCandidates();
		return sdpAnswer;
	}

	public String processViewerOffer(UserSession viewer, String sdpOffer) {
		if (!viewers.containsKey(viewer.getSession().getId())) {
			throw new IllegalStateException("Viewer is not registered in the room.");
		}
		WebRtcEndpoint viewerEndpoint = viewer.getWebRtcEndpoint();
		String sdpAnswer = viewerEndpoint.processOffer(sdpOffer);
		viewerEndpoint.gatherCandidates();
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
