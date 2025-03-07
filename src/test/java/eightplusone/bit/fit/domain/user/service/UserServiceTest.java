package eightplusone.bit.fit.domain.user.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import eightplusone.bit.fit.domain.auth.dto.CustomUserDetails;
import eightplusone.bit.fit.domain.auth.service.OAuth2UnlinkService;
import eightplusone.bit.fit.domain.auth.service.RedisTokenService;
import eightplusone.bit.fit.domain.user.entity.User;
import eightplusone.bit.fit.domain.user.repository.UserRepository;
import eightplusone.bit.fit.support.fixture.UserFixture;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	UserRepository userRepository;

	@Mock
	RedisTokenService redisTokenService;

	@Mock
	OAuth2UnlinkService oAuth2UnlinkService;

	@InjectMocks
	UserService userService;

	@Test
	@DisplayName("회원 탈퇴를 한다")
	void userDelete() {
		//given
		User user = UserFixture.USER_FIXTURE_1.createUser();
		String provider = user.getProvider();

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
			new CustomUserDetails(user), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);

		Mockito.when(userRepository.findLoginUserByEmail(user.getEmail())).thenReturn(user);
		Mockito.doNothing().when(oAuth2UnlinkService).unlink(provider);
		//when
		userService.delete();

		//then
		Mockito.verify(userRepository, Mockito.times(1)).delete(user);
	}
}