
package com.example.meeting.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.List;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Room name is required")
    @Size(min = 2, max = 100, message = "Room name must be between 2 and 100 characters")
    @Column(nullable = false, unique = true)
    private String name;
    
    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 100, message = "Capacity cannot exceed 100")
    @Column(nullable = false)
    private int capacity;
    
    @Min(value = 0, message = "Floor cannot be negative")
    @Max(value = 50, message = "Floor cannot exceed 50")
    @Column(nullable = false)
    private int floor;
    
    @ElementCollection
    @CollectionTable(name = "room_amenities", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "amenity")
    private List<String> amenities;

    public Room(){}

    public Room(Long id, String name, int capacity, int floor, List<String> amenities) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.floor = floor;
        this.amenities = amenities;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public int getFloor() { return floor; }
    public void setFloor(int floor) { this.floor = floor; }

    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }
}
