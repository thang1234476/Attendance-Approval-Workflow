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




    // Thêm vào cuối interface, trước dấu }

// Thống kê số lần nghỉ theo nhân viên và lý do trong khoảng thời gian
@Query("SELECT l.user.username as username, l.reason as reason, COUNT(l) as total " +
       "FROM LeaveRequest l WHERE l.status = 'APPROVED' " +
       "AND YEAR(l.startDate) = :year " +
       "AND (:quarter IS NULL OR QUARTER(l.startDate) = :quarter) " +
       "AND (:month IS NULL OR MONTH(l.startDate) = :month) " +
       "GROUP BY l.user.username, l.reason")
List<Object[]> countLeaveByUserAndReason(@Param("year") int year,
                                          @Param("quarter") Integer quarter,
                                          @Param("month") Integer month);

// Thống kê tổng số ngày nghỉ theo nhân viên
@Query("SELECT l.user.username as username, SUM(DATEDIFF(l.endDate, l.startDate) + 1) as totalDays " +
       "FROM LeaveRequest l WHERE l.status = 'APPROVED' " +
       "AND YEAR(l.startDate) = :year " +
       "AND (:quarter IS NULL OR QUARTER(l.startDate) = :quarter) " +
       "AND (:month IS NULL OR MONTH(l.startDate) = :month) " +
       "GROUP BY l.user.username")
List<Object[]> countTotalLeaveDaysByUser(@Param("year") int year,
                                          @Param("quarter") Integer quarter,
                                          @Param("month") Integer month);

// Thống kê xu hướng nghỉ theo tháng
@Query("SELECT MONTH(l.startDate) as month, COUNT(l) as total " +
       "FROM LeaveRequest l WHERE l.status = 'APPROVED' AND YEAR(l.startDate) = :year " +
       "GROUP BY MONTH(l.startDate) ORDER BY month")
List<Object[]> countLeaveByMonth(@Param("year") int year);




// Thống kê số lần nghỉ và lý do theo nhân viên
@Query("SELECT l.user.id, l.user.username, " +
       "COUNT(l) as totalLeave, " +
       "SUM(CASE WHEN l.status = 'REJECTED' THEN 1 ELSE 0 END) as rejectedCount, " +
       "SUM(CASE WHEN l.reason LIKE '%ốm%' OR l.reason LIKE '%đột xuất%' THEN 1 ELSE 0 END) as suddenLeave " +
       "FROM LeaveRequest l " +
       "WHERE YEAR(l.startDate) = :year " +
       "AND (:quarter IS NULL OR QUARTER(l.startDate) = :quarter) " +
       "AND (:month IS NULL OR MONTH(l.startDate) = :month) " +
       "GROUP BY l.user.id, l.user.username")
List<Object[]> getLeaveStatsByEmployee(@Param("year") int year,
                                        @Param("quarter") Integer quarter,
                                        @Param("month") Integer month);
}