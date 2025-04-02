package eightplusone.bit.fit.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eightplusone.bit.fit.domain.chat.enums.ChatCategory;
import java.time.LocalDateTime;
import lombok.Builder;
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
	private LocalDateTime timestamp;
	private int likes;

	@Builder
	@JsonCreator
	public ChatMessageDto(
		@JsonProperty("messageId") String messageId,
		@JsonProperty("category") ChatCategory category,
		@JsonProperty("message") String message,
		@JsonProperty("name") String name,
		@JsonProperty("userId") String userId,
		@JsonProperty("sessionId") Long sessionId,
		@JsonProperty("timestamp") LocalDateTime timestamp,
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
