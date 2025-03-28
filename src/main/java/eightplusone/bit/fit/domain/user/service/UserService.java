package eightplusone.bit.fit.domain.user.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import eightplusone.bit.fit.domain.auth.service.OAuth2UnlinkService;
import eightplusone.bit.fit.domain.auth.service.RedisTokenService;
import eightplusone.bit.fit.domain.image.dto.S3ImageDto;
import eightplusone.bit.fit.domain.image.service.ImageService;
import eightplusone.bit.fit.domain.interest.entity.Interest;
import eightplusone.bit.fit.domain.interest.entity.MyInterest;
import eightplusone.bit.fit.domain.interest.repository.InterestRepository;
import eightplusone.bit.fit.domain.interest.repository.MyInterestRepository;
import eightplusone.bit.fit.domain.user.dto.UserAccountResponseDto;
import eightplusone.bit.fit.domain.user.dto.UserProfileResponseDto;
import eightplusone.bit.fit.domain.user.dto.UserProfileUpdateRequestDto;
import eightplusone.bit.fit.domain.user.entity.User;
import eightplusone.bit.fit.domain.user.repository.UserRepository;
import eightplusone.bit.fit.global.exception.CustomException;
import eightplusone.bit.fit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;
	private final MyInterestRepository myInterestRepository;
	private final InterestRepository interestRepository;
	private final RedisTokenService redisTokenService;
	private final OAuth2UnlinkService oAuth2UnlinkService;
	private final ImageService imageService;

	@Transactional
	public void delete(String email) {
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

	public UserAccountResponseDto getAccountInfo(String email) {
		User user = userRepository.findLoginUserByEmail(email);
		return UserAccountResponseDto.of(user.getName(), user.getEmail(), user.getImageUrl());
	}

	public UserProfileResponseDto getProfileInfo(String email) {
		User user = userRepository.findLoginUserByEmail(email);
		List<MyInterest> myInterest = myInterestRepository.findByUserIdWithInterests(user.getId());
		return UserProfileResponseDto.of(user.getJob(), user.getYears(), myInterest);
	}

	@Transactional
	public void updateProfileInfo(String email, UserProfileUpdateRequestDto requestDto) {
		User user = userRepository.findLoginUserByEmail(email);
		user.updateProfileInfo(requestDto.getName(), requestDto.getJob(), requestDto.getYears());

		Interest interest1 = interestRepository.findByName(requestDto.getInterests().get(0));
		Interest interest2 = interestRepository.findByName(requestDto.getInterests().get(1));
		Interest interest3 = interestRepository.findByName(requestDto.getInterests().get(2));

		List<MyInterest> myInterests = myInterestRepository.findByUserIdWithInterests(user.getId());
		if (!myInterests.isEmpty()) {
			myInterests.get(0).updateInterest(interest1, user);
			myInterests.get(1).updateInterest(interest2, user);
			myInterests.get(2).updateInterest(interest3, user);
			return;
		}
		myInterestRepository.saveAll(List.of(
			MyInterest.of(interest1, user),
			MyInterest.of(interest2, user),
			MyInterest.of(interest3, user)
		));
	}

	@Transactional
	public void updateProfileImage(String email, MultipartFile requestImage) {
		User user = userRepository.findLoginUserByEmail(email);
		String userImageUrl = user.getImageUrl();
		if (userImageUrl != null) {
			imageService.deleteFromS3(userImageUrl);
		}
		S3ImageDto s3ImageDto = imageService.uploadToS3(requestImage);
		user.updateProfileImage(
			s3ImageDto.getName(), s3ImageDto.getUrl()
		);
	}

	@Transactional
	public void deleteProfileImage(String email) {
		User user = userRepository.findLoginUserByEmail(email);
		String userImageUrl = user.getImageUrl();
		if (userImageUrl == null) {
			throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
		}
		imageService.deleteFromS3(userImageUrl);
		user.deleteProfileImage();
	}

	public String findProfileImage(String email) {
		User user = userRepository.findLoginUserByEmail(email);
		return user.getImageUrl();
	}
}
