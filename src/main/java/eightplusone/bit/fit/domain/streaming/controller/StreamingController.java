package eightplusone.bit.fit.domain.streaming.controller;

import java.security.Principal;

import org.kurento.client.IceCandidate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import eightplusone.bit.fit.domain.streaming.dto.IceCandidateDto;
import eightplusone.bit.fit.domain.streaming.service.RoomService;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class StreamingController {
	private final RoomService roomService;

	public StreamingController(RoomService roomService) {
		this.roomService = roomService;
	}

	/**
	 *  * 발표자 SDP Offer 처리 -> SDP Answer 반환
	 */
	@MessageMapping("/room/{roomId}/presenter")
	@SendTo("/sub/room/{roomId}/presenterAnswer")
	public String handlePresenterOffer(@DestinationVariable String roomId, String sdpOffer) {
		log.info("[Presenter] Received SDP Offer: \n{}", sdpOffer);
		// return roomService.processPresenterOffer(roomId, sdpOffer);
		try {
			String sdpAnswer = roomService.processPresenterOffer(roomId, sdpOffer);
			log.info("sdpAnswer generated: {}", sdpAnswer);
			return sdpAnswer;
		} catch (Exception e) {
			log.error("Exception in handlePresenterOffer: ", e);
			throw e; // Rethrow or handle
		}
	}

	/**
	 * 발표자 ICE 후보 수신
	 */

	@MessageMapping("/room/{roomId}/presenterIce")
	public void handlePresenterIceCandidate(@DestinationVariable String roomId, IceCandidateDto candidateDto) {
		IceCandidate candidate = new IceCandidate(
			candidateDto.getCandidate(),
			candidateDto.getSdpMid(),
			candidateDto.getSdpMLineIndex()
		);
		roomService.addPresenterIceCandidate(roomId, candidate);
	}

	/**
	 * 청중 SDP Offer 처리 -> SDP Answer 반환
	 */
	@MessageMapping("/room/{roomId}/audience")
	@SendTo("/sub/room/{roomId}/audience/answer")
	public String handleAudienceOffer(
		@DestinationVariable String roomId,
		@Header("simpUser") Principal principal,
		String sdpOffer
	) {
		return roomService.processAudienceOffer(roomId, principal.getName(), sdpOffer);
	}

	/**
	 * 청중 ICE 후보 수신
	 */
	@MessageMapping("/room/{roomId}/audience/ice")
	public void handleAudienceIceCandidate(
		@DestinationVariable String roomId,
		@Header("simpUser") Principal principal,
		IceCandidateDto dto
	) {
		IceCandidate candidate = new IceCandidate(dto.getCandidate(), dto.getSdpMid(), dto.getSdpMLineIndex());
		roomService.addAudienceIceCandidate(roomId, principal.getName(), candidate);
	}

	/**
	 * 발표자 나가기 요청 처리
	 */
	@MessageMapping("/room/{roomId}/presenter/leave")
	public void handlePresenterLeave(@DestinationVariable String roomId) {
		log.info("[Presenter] Leave request received for room: {}", roomId);
		roomService.leavePresenter(roomId);
	}

	/**
	 * 청중 나가기 요청 처리
	 */
	@MessageMapping("/room/{roomId}/audience/leave")
	public void handleAudienceLeave(
		@DestinationVariable String roomId,
		@Header("simpUser") Principal principal
	) {
		log.info("[Audience] Leave request received for {} in room: {}", principal.getName(), roomId);
		roomService.leaveAudience(roomId, principal.getName());
	}

}
