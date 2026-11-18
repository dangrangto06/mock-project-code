package com.codeforge.repository;

import com.codeforge.entity.CommunityReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityReportRepository extends JpaRepository<CommunityReport, Long> {

    List<CommunityReport> findByStatusOrderByCreatedAtDesc(String status);

    long countByStatus(String status);

    void deleteByReportTypeAndTargetId(String reportType, Long targetId);

    List<CommunityReport> findByReportTypeAndTargetId(String reportType, Long targetId);
}
