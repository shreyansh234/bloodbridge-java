package com.bloodbridge;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
public interface FamilyRequestRepository extends JpaRepository<FamilyRequest,String> {
 long countByOwnerIdAndStatusNot(String ownerId,String status);
 List<FamilyRequest> findTop50ByOwnerIdOrderByCreatedAtDesc(String ownerId);
 List<FamilyRequest> findTop100ByStatusInOrderByCreatedAtAsc(Collection<String> status);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select f from FamilyRequest f where f.id=:id") Optional<FamilyRequest> findForReview(@Param("id") String id);
}
