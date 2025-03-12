package eightplusone.bit.fit.domain.streaming;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.kurento.client.KurentoClient;
import org.springframework.beans.factory.annotation.Autowired;

import eightplusone.bit.fit.domain.session.service.SessionService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RoomManager {

	@Autowired
	private KurentoClient kurento;

	@Autowired
	private SessionService sessionService;

	private final ConcurrentMap<String, Room> rooms = new ConcurrentHashMap<>();

	public Room getRoom(String roomName) {
		log.info("Searching for room {}", roomName);
		Room room = rooms.get(roomName);

		if (room == null) {
			log.info("Room {} not found. Creating new room.", roomName);
			room = new Room(roomName, kurento.createMediaPipeline(), sessionService);
			rooms.put(roomName, room);
		}

		log.info("Room : {}", room.getName());

		return room;
	}

	public void removeRoom(String roomName) {
		Room room = rooms.remove(roomName);
		if (room != null) {
			room.close();
			log.info("Room {} removed and closed", roomName);
		}
	}

	public Collection<Room> getAllRooms() {
		return rooms.values();
	}

}

