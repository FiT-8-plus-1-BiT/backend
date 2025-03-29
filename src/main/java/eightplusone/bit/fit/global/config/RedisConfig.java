package eightplusone.bit.fit.global.config;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eightplusone.bit.fit.global.pubsub.ChatSubscriber;
import eightplusone.bit.fit.global.pubsub.LikeSubscriber;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
	private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

	@Value("${spring.data.redis.host}")
	private String redisHost;

	@Value("${spring.data.redis.port}")
	private int redisPort;

	@Value("${spring.data.redis.password:}") // 기본값 "" (비밀번호 없을 경우)
	private String redisPassword;

	@Bean
	public LettuceConnectionFactory redisConnectionFactory() {
		RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
		redisConfig.setHostName(redisHost);
		redisConfig.setPort(redisPort);
		if (!redisPassword.isEmpty()) {
			redisConfig.setPassword(redisPassword);
		}
		LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
			.commandTimeout(Duration.ofSeconds(5)) // 명령 실행 타임아웃 (기본값 60초 → 5초로 최적화)
			.shutdownTimeout(Duration.ZERO) // 종료 시 대기 시간 없음
			.clientOptions(ClientOptions.builder()
				.autoReconnect(true) // 자동 재연결 활성화
				.socketOptions(SocketOptions.builder()
					.connectTimeout(Duration.ofSeconds(5)) // 소켓 연결 타임아웃 (기본값 10초 → 5초)
					.build()
				)
				.timeoutOptions(TimeoutOptions.enabled()) // Redis 요청 타임아웃 활성화
				.build()
			)
			.build();

		return new LettuceConnectionFactory(redisConfig, clientConfig);
	}

	// @Bean
	// public RedisTemplate<String, Object> redisTemplate() {
	// 	RedisTemplate<String, Object> template = new RedisTemplate<>();
	// 	Jackson2JsonRedisSerializer<ChatMessageDto> serializer = new Jackson2JsonRedisSerializer<>(
	// 		ChatMessageDto.class);
	// 	template.setConnectionFactory(redisConnectionFactory());
	// 	template.setKeySerializer(new StringRedisSerializer());
	// 	template.setValueSerializer(serializer);
	// 	template.setHashKeySerializer(new StringRedisSerializer());
	// 	template.setHashValueSerializer(new StringRedisSerializer());
	// 	template.afterPropertiesSet();
	// 	return template;
	// }

	@Bean
	public ObjectMapper redisObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		// 💡 LocalDateTime 직렬화 시 나노초 제거 (기존 오류 해결용)
		objectMapper.configOverride(LocalDateTime.class)
			.setFormat(JsonFormat.Value.forPattern("yyyy-MM-dd'T'HH:mm:ss"));

		return objectMapper;
	}

	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
		ObjectMapper redisObjectMapper) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
		serializer.setObjectMapper(redisObjectMapper);

		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(serializer);
		template.setHashKeySerializer(new StringRedisSerializer());
		template.setHashValueSerializer(serializer);

		return template;
	}

	@Bean
	@Primary
	public RedisTemplate<String, String> redisTemplateForUserName() {
		RedisTemplate<String, String> template = new RedisTemplate<>();
		template.setConnectionFactory(redisConnectionFactory());
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new StringRedisSerializer()); // String 직렬화
		template.afterPropertiesSet();
		return template;
	}

	// Redis 메시지 리스너 등록
	@Bean
	public RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory,
		MessageListenerAdapter chatListenerAdapter,
		MessageListenerAdapter likeListenerAdapter) { // 좋아요 메시지 리스너 추가

		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);

		// 채팅 메시지 구독 (chat-{sessionId} 패턴)
		container.addMessageListener(chatListenerAdapter, new PatternTopic("chat-*"));

		// 좋아요 메시지 구독 (chat-likes)
		container.addMessageListener(likeListenerAdapter, new ChannelTopic("chat-likes"));

		logger.info("✅ Redis 구독 완료: chat-* (채팅), chat-likes (좋아요)");
		return container;
	}

	// ChatSubscriber를 Redis 리스너로 등록
	@Bean
	public MessageListenerAdapter chatListenerAdapter(ChatSubscriber chatSubscriber) {
		return new MessageListenerAdapter(chatSubscriber, "onMessage");
	}

	// likeSubscriber를 Redis 리스너로 등록
	@Bean
	public MessageListenerAdapter likeListenerAdapter(LikeSubscriber likeSubscriber) {
		return new MessageListenerAdapter(likeSubscriber, "onMessage");
	}

}
