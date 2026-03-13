package com.example.meeting.repository;

import com.example.meeting.model.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    List<Booking> findByRoomId(Long roomId);
    
    List<Booking> findByRoomIdAndStatus(Long roomId, String status);
    
    @Query("SELECT b FROM Booking b WHERE b.roomId = :roomId AND b.status = 'confirmed' AND " +
           "b.startTime < :endTime AND b.endTime > :startTime")
    List<Booking> findOverlappingBookings(@Param("roomId") Long roomId, 
                                         @Param("startTime") LocalDateTime startTime, 
                                         @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT b FROM Booking b WHERE " +
           "(:roomId IS NULL OR b.roomId = :roomId) AND " +
           "(:from IS NULL OR b.startTime >= :from) AND " +
           "(:to IS NULL OR b.endTime <= :to)")
    Page<Booking> findBookingsWithFilters(@Param("roomId") Long roomId,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to,
                                        Pageable pageable);
}
