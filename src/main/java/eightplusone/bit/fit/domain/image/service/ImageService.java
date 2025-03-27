package eightplusone.bit.fit.domain.image.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import eightplusone.bit.fit.domain.image.dto.S3ImageDto;
import eightplusone.bit.fit.global.exception.CustomException;
import eightplusone.bit.fit.global.exception.ErrorCode;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Operations;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ImageService {
	private final S3Operations s3Operations;
	private final String bucket;
	private final String s3baseUrl;

	public ImageService(
		S3Operations s3Operations,
		@Value("${cloud.aws.s3.bucket}") String bucket,
		@Value("${cloud.aws.region.static}") String region
	) {
		this.s3Operations = s3Operations;
		this.bucket = bucket;
		this.s3baseUrl = String.format("https://%s.s3.%s.amazonaws.com/", bucket, region);
	}

	public S3ImageDto uploadToS3(MultipartFile multipartFile) {
		try (InputStream inputStream = multipartFile.getInputStream()) {
			validateImage(multipartFile);

			String imageName = generateUniqueImageName(multipartFile.getOriginalFilename());
			ObjectMetadata objectMetadata = ObjectMetadata.builder()
				.contentType(multipartFile.getContentType())
				.build();
			s3Operations.upload(bucket, imageName, inputStream, objectMetadata);

			return S3ImageDto.of(s3baseUrl, imageName);
		} catch (IOException e) {
			throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	public String generateUniqueImageName(String originalFileName) {
		String cleanImageName = originalFileName.replace(" ", "_");
		String uuid = UUID.randomUUID().toString();
		return uuid + "_" + cleanImageName;
	}

	public void deleteFromS3(String url) {
		String imageName = url.substring(url.lastIndexOf("/") + 1);
		s3Operations.deleteObject(bucket, imageName);
	}

	private void validateImage(MultipartFile multipartFile) {
		String contentType = multipartFile.getContentType();
		String name = multipartFile.getOriginalFilename();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}
		if (name == null || !name.matches(".*\\.(jpg|jpeg|png|webp)$")) {
			throw new CustomException(ErrorCode.INVALID_REQUEST);
		}
	}
}
