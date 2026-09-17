package com.distribution.music.repository;

import com.distribution.music.entity.Release;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReleaseRepository extends JpaRepository<Release, Long> {
    List<Release> findByArtistIdOrderByUpdatedAtDesc(Long artistId);
    Optional<Release> findByUpc(String upc);
}
