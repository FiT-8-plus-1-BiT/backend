package eightplusone.bit.fit.global.base;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@Getter
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public class BaseTimeEntity {

	@CreatedDate
	@Column(name = "created_date", updatable = false)
	private LocalDateTime createdDate;

	@LastModifiedDate
	@Column(name = "modified_date")
	private LocalDateTime modifiedDate;
}
