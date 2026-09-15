package com.bloodbridge;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
public interface BloodMatchRepository extends JpaRepository<BloodMatch,String>{
 List<BloodMatch> findTop100ByDonorIdOrReceiverIdOrderByCreatedAtDesc(String donorId,String receiverId);
 long countByReceiverIdAndStatus(String receiverId,String status);
 boolean existsByDonorIdAndReceiverIdAndStatusIn(String donorId,String receiverId,Collection<String> statuses);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select m from BloodMatch m where m.id=:id") Optional<BloodMatch> lockById(@Param("id") String id);
}
