package com.bloodbridge;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
public interface DonationRecordRepository extends JpaRepository<DonationRecord,String> {
 long countByOwnerIdAndStatus(String ownerId,String status);
 List<DonationRecord> findTop200ByOwnerIdOrderByDonationDateDescCreatedAtDesc(String ownerId);
 List<DonationRecord> findTop100ByStatusOrderByCreatedAtAsc(String status);
 boolean existsByReferenceKey(String referenceKey);
 boolean existsByOwnerIdAndDonationDate(String ownerId,String date);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select d from DonationRecord d where d.id=:id") Optional<DonationRecord> findForReview(@Param("id") String id);
}
