package com.example.attendance.repository;

import com.example.attendance.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;


@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByUserId(Long userId);
    long countByStatus(com.example.attendance.entity.LeaveStatus status);
    
    @Query("SELECT l FROM LeaveRequest l WHERE " +
           "(:name IS NULL OR LOWER(l.user.username) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:date IS NULL OR (l.startDate <= :date AND l.endDate >= :date))")
    List<LeaveRequest> searchLeaves(@Param("name") String name, @Param("date") LocalDate date);
}