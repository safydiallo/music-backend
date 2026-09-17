package com.distribution.music.repository;

import com.distribution.music.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrackRepository extends JpaRepository<Track, Long> {
    List<Track> findByReleaseIdOrderByTrackNumberAsc(Long releaseId);
    boolean existsByIsrc(String isrc);
    Optional<Track> findByIsrc(String isrc);
    Optional<Track> findByIdAndReleaseId(Long id, Long releaseId);
}
