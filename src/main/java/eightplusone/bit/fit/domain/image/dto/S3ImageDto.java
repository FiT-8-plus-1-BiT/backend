package eightplusone.bit.fit.domain.image.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class S3ImageDto {
	private final String url;
	private final String name;

	@Builder
	private S3ImageDto(String url, String name) {
		this.url = url;
		this.name = name;
	}

	public static S3ImageDto of(String s3baseUrl, String name) {
		return S3ImageDto.builder()
			.url(s3baseUrl + name)
			.name(name)
			.build();
	}
}
