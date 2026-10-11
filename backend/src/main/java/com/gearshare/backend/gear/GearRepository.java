
package com.gearshare.backend.gear;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GearRepository extends JpaRepository<Gear, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM Gear g WHERE g.id = :gearId")
    Optional<Gear> findByIdForUpdate(
            @Param("gearId") Long gearId
    );
}