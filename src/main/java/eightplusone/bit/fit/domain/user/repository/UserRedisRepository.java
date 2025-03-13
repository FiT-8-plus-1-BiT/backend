package eightplusone.bit.fit.domain.user.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRedisRepository {
	private final RedisTemplate<String, Object> redisTemplate;

	public UserRedisRepository(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	// userId - name 매핑 저장 (닉네임 변경 시에도 호출)
	public void saveUserName(String userId, String name) {
		redisTemplate.opsForValue().set("user:" + userId, name);
	}

	// userId로 name 조회
	public String getUserName(String userId) {
		String name = (String)redisTemplate.opsForValue().get("user:" + userId);
		return (name != null) ? name : "알 수 없음"; // 값이 없으면 기본값 반환
	}
}
