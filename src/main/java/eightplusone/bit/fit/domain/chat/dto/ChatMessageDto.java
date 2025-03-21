package eightplusone.bit.fit.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eightplusone.bit.fit.domain.chat.enums.ChatCategory;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatMessageDto {
	private String messageId;
	private ChatCategory category;
	private String message;
	private String name;
	private String userId;
	private Long sessionId;
	private String timestamp;
	private int likes;

	@JsonCreator
	public ChatMessageDto(
		@JsonProperty("messageId") String messageId,
		@JsonProperty("category") ChatCategory category,
		@JsonProperty("message") String message,
		@JsonProperty("name") String name,
		@JsonProperty("userId") String userId,
		@JsonProperty("sessionId") Long sessionId,
		@JsonProperty("timestamp") String timestamp,
		@JsonProperty("likes") int likes) {
		this.messageId = messageId;
		this.category = category;
		this.message = message;
		this.name = name;
		this.userId = userId;
		this.sessionId = sessionId;
		this.timestamp = timestamp;
		this.likes = likes;
	}
}
