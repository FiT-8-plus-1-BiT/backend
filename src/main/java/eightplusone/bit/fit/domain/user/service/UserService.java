package eightplusone.bit.fit.domain.user.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eightplusone.bit.fit.domain.auth.service.OAuth2UnlinkService;
import eightplusone.bit.fit.domain.auth.service.RedisTokenService;
import eightplusone.bit.fit.domain.user.dto.UserAccountResponseDto;
import eightplusone.bit.fit.domain.user.dto.UserProfileResponseDto;
import eightplusone.bit.fit.domain.user.dto.UserProfileUpdateRequestDto;
import eightplusone.bit.fit.domain.user.entity.User;
import eightplusone.bit.fit.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;
	private final RedisTokenService redisTokenService;
	private final OAuth2UnlinkService oAuth2UnlinkService;

	@Transactional
	public void delete() {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		User user = userRepository.findLoginUserByEmail(email);
		userRepository.delete(user);
		deleteOauth2AccessToken(user.getProvider());
	}

	private void deleteOauth2AccessToken(String provider) {
		if (provider.startsWith("kakao") || provider.startsWith("google") || provider.startsWith("naver")) {
			oAuth2UnlinkService.unlink(provider);
		}
		if (redisTokenService.getOauth2AccessToken(provider) != null) {
			redisTokenService.deleteOauth2AccessToken(provider);
		}
	}

	public UserAccountResponseDto getAccountInfo() {
		User user = userRepository.findLoginUserByEmail(
			SecurityContextHolder.getContext().getAuthentication().getName());
		return UserAccountResponseDto.of(user.getName(), user.getEmail());
	}

	public UserProfileResponseDto getProfileInfo() {
		User user = userRepository.findLoginUserByEmail(
			SecurityContextHolder.getContext().getAuthentication().getName());
		return UserProfileResponseDto.of(user.getJob(), user.getYears(), user.getInterests());
	}

	@Transactional
	public void updateProfileInfo(UserProfileUpdateRequestDto userProfileUpdateRequestDto) {
		User user = userRepository.findLoginUserByEmail(
			SecurityContextHolder.getContext().getAuthentication().getName());
		user.updateProfileInfo(
			userProfileUpdateRequestDto.getJob(),
			userProfileUpdateRequestDto.getYears(),
			userProfileUpdateRequestDto.getInterests());
	}
}
