package dev.mel0n.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.mel0n.entity.MlnDownloaderEntity;

public interface MlnDownloaderReposiroty extends JpaRepository<MlnDownloaderEntity, Long> {

    Optional<MlnDownloaderEntity> findByFileName(String fileName);

}
