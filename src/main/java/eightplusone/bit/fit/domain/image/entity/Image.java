package eightplusone.bit.fit.domain.image.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Image {

	@Column(name = "image_url")
	private String url;

	@Column(name = "image_name")
	private String name;

	@Builder
	private Image(String url, String name) {
		this.url = url;
		this.name = name;
	}

	public static Image of(String url, String name) {
		return Image.builder()
			.url(url)
			.name(name)
			.build();
	}
}
