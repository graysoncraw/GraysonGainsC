package com.graysoncraw.ggainsbackend.repository;

import com.graysoncraw.ggainsbackend.model.PersonalRecord;
import com.graysoncraw.ggainsbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonalRecordRepository extends JpaRepository<PersonalRecord, Long> {

    Optional<PersonalRecord> findByUser(User user);
    Optional<PersonalRecord> findByUser_FirebaseUid(String firebaseUid);
}
