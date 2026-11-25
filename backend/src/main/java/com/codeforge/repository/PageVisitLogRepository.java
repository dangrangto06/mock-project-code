package com.codeforge.repository;

import com.codeforge.entity.PageVisitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageVisitLogRepository extends JpaRepository<PageVisitLog, Long> {

    List<PageVisitLog> findAllByOrderByVisitedAtDesc();

    @Query("SELECT DISTINCT p.location FROM PageVisitLog p WHERE p.location IS NOT NULL AND p.location <> ''")
    List<String> findDistinctLocations();
}
