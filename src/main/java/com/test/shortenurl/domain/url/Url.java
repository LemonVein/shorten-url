package com.test.shortenurl.domain.url;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Table(name = "url")
public class Url {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shorten_url", unique = true)
    private String shortUrl;

    @Column(name = "original_url", columnDefinition = "LONGTEXT", nullable = false)
    private String originalUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    private Boolean deleted;

    @Column(name = "created_by")
    private String createdBy;
}
