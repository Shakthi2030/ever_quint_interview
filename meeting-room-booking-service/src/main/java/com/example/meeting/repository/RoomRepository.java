package com.example.meeting.repository;

import com.example.meeting.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    
    boolean existsByNameIgnoreCase(String name);
    
    List<Room> findByCapacityGreaterThanEqual(Integer minCapacity);
    
    @Query("SELECT r FROM Room r WHERE :amenity MEMBER OF r.amenities")
    List<Room> findByAmenity(@Param("amenity") String amenity);
}
