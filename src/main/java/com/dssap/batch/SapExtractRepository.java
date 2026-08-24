package com.dssap.batch;

import com.dssap.batch.entity.SapExtractEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SapExtractRepository extends JpaRepository<SapExtractEntity, Long> {

    List<SapExtractEntity> findTop50ByOrderByExtractedAtDesc();

    long countByJobName(String jobName);
}
