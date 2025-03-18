package eightplusone.bit.fit.domain.chat.entity;

import eightplusone.bit.fit.domain.chat.enums.ChatCategory;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;

@RedisHash(value = "chat_message")
@Getter
@NoArgsConstructor
public class ChatMessage {
	@Id
	private String messageId; // 메시지 PK

	private String sessionId; // 강연 ID

	private String userId; // 사용자 ID

	@Enumerated(EnumType.STRING)
	private ChatCategory category; // ENUM 사용

	@Column(length = 300, nullable = false)
	private String message; // 채팅 내용

	private String timestamp; // 작성 시간

	// 기존 메시지를 새로 저장할 때 사용하는 생성자
	@Builder
	public ChatMessage(String sessionId, String userId, ChatCategory category, String message) {
		this.messageId = UUID.randomUUID().toString();
		this.sessionId = sessionId;
		this.userId = userId;
		this.category = category;
		this.message = message;
		this.timestamp = LocalDateTime.now().toString();
	}

	// Redis에서 가져온 기존 메시지를 복원할 때 사용하는 생성자
	public ChatMessage(String messageId, String sessionId, String userId, ChatCategory category, String message,
		String timestamp) {
		this.messageId = messageId;  // 기존 메시지 ID 유지
		this.sessionId = sessionId;
		this.userId = userId;
		this.category = category;
		this.message = message;
		this.timestamp = timestamp;
	}
}
