package com.codeforge.repository;

import com.codeforge.entity.UserLoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserLoginLogRepository extends JpaRepository<UserLoginLog, Long> {

    List<UserLoginLog> findAllByOrderByLoggedInAtDesc();

    @Query("SELECT DISTINCT u.location FROM UserLoginLog u WHERE u.location IS NOT NULL AND u.location <> ''")
    List<String> findDistinctLocations();
}
