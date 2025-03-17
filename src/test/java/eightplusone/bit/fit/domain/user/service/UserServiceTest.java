package eightplusone.bit.fit.domain.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

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
import eightplusone.bit.fit.domain.user.dto.UserAccountResponseDto;
import eightplusone.bit.fit.domain.user.dto.UserProfileResponseDto;
import eightplusone.bit.fit.domain.user.dto.UserProfileUpdateRequestDto;
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

	@Test
	@DisplayName("회원 계정 정보를 조회한다")
	void userAccountGet() {
		//given
		User user = UserFixture.USER_FIXTURE_1.createUser();

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
			new CustomUserDetails(user), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);

		Mockito.when(userRepository.findLoginUserByEmail(user.getEmail())).thenReturn(user);

		//when
		UserAccountResponseDto accountInfo = userService.getAccountInfo();

		//then
		assertAll(
			() -> assertThat(accountInfo.getName()).isEqualTo(user.getName()),
			() -> assertThat(accountInfo.getEmail()).isEqualTo(user.getEmail())
		);
	}

	@Test
	@DisplayName("회원 개인 정보를 조회한다")
	void userProfileGet() {
		//given
		User user = UserFixture.USER_FIXTURE_1.createUser();

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
			new CustomUserDetails(user), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);

		Mockito.when(userRepository.findLoginUserByEmail(user.getEmail())).thenReturn(user);

		//when
		UserProfileResponseDto profileInfo = userService.getProfileInfo();

		//then
		assertAll(
			() -> assertThat(profileInfo.getJob()).isEqualTo(user.getJob()),
			() -> assertThat(profileInfo.getYears()).isEqualTo(user.getYears()),
			() -> assertThat(profileInfo.getInterests()).isEqualTo(user.getInterests())
		);
	}

	@Test
	@DisplayName("회원 개인 정보를 업데이트한다")
	void userProfileUpdate() {
		//given
		User user = UserFixture.USER_FIXTURE_1.createUser();

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
			new CustomUserDetails(user), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);

		Mockito.when(userRepository.findLoginUserByEmail(user.getEmail())).thenReturn(user);

		UserProfileUpdateRequestDto userProfileUpdateRequestDto = UserProfileUpdateRequestDto.of("디자이너", 10, "css");

		//when
		userService.updateProfileInfo(userProfileUpdateRequestDto);

		//then
		assertAll(
			() -> assertThat(user.getJob()).isEqualTo("디자이너"),
			() -> assertThat(user.getYears()).isEqualTo(10),
			() -> assertThat(user.getInterests()).isEqualTo("css")
		);
	}
}