package com.example.attendance.repository;

import com.example.attendance.entity.Attendance;
import com.example.attendance.entity.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    @Query("SELECT a FROM Attendance a WHERE a.user.id = :userId AND a.checkInTime BETWEEN :start AND :end")
    Optional<Attendance> findByUserIdAndCheckInTimeBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
    // Lỗi fix: dùng tham số enum thay vì chuỗi string để so sánh trong JPQL
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.status = :status AND a.checkInTime BETWEEN :start AND :end")
    long countByStatusAndCheckInTimeBetween(
            @Param("status") AttendanceStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.checkInTime BETWEEN :start AND :end")
    long countTodayCheckIns(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT a FROM Attendance a WHERE a.checkInTime BETWEEN :start AND :end ORDER BY a.checkInTime DESC")
    List<Attendance> findAllByCheckInTimeBetween(@Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT a FROM Attendance a WHERE YEAR(a.checkInTime) = :year AND MONTH(a.checkInTime) = :month ORDER BY a.user.username, a.checkInTime")
    List<Attendance> findByMonthAndYear(@Param("year") int year, @Param("month") int month);

    @Query("SELECT a FROM Attendance a WHERE a.checkInTime BETWEEN :start AND :end AND a.checkOutTime IS NULL")
    List<Attendance> findCheckedInNotCheckout(LocalDateTime start, LocalDateTime end);
    // Thống kê số lần đi trễ, về sớm theo nhân viên trong khoảng thời gian
@Query("SELECT a.user.id, a.user.username, " +
       "SUM(CASE WHEN a.status = 'LATE' THEN 1 ELSE 0 END) as lateCount, " +
       "SUM(CASE WHEN a.checkoutStatus = 'EARLY' THEN 1 ELSE 0 END) as earlyCount, " +
       "COUNT(a) as totalWorkingDays, " +
       "COALESCE(SUM(a.totalHours), 0) as totalHours " +
       "FROM Attendance a " +
       "WHERE YEAR(a.checkInTime) = :year " +
       "AND (:quarter IS NULL OR QUARTER(a.checkInTime) = :quarter) " +
       "AND (:month IS NULL OR MONTH(a.checkInTime) = :month) " +
       "GROUP BY a.user.id, a.user.username")
List<Object[]> getEmployeePerformance(@Param("year") int year,
                                       @Param("quarter") Integer quarter,
                                       @Param("month") Integer month);
}
