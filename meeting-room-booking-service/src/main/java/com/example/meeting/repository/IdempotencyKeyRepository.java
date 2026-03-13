package com.example.meeting.repository;

import com.example.meeting.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    
    Optional<IdempotencyKey> findByKeyAndOrganizerEmail(String key, String organizerEmail);
    
    boolean existsByKeyAndOrganizerEmail(String key, String organizerEmail);
}
