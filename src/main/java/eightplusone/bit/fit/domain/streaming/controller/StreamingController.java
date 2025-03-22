package eightplusone.bit.fit.domain.streaming.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

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

}
