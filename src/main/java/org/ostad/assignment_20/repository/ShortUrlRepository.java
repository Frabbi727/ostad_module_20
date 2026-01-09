package org.ostad.assignment_20.repository;

import org.ostad.assignment_20.entity.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByCode(String code);

    Optional<ShortUrl> findByOriginalUrl(String originalUrl);

    boolean existsByCode(String code);
}