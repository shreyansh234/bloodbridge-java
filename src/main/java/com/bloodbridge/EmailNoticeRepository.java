package com.bloodbridge;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
public interface EmailNoticeRepository extends JpaRepository<EmailNotice,String>{
 List<EmailNotice> findTop20ByStatusInAndAttemptsLessThanAndNextAttemptLessThanEqualOrderByNextAttemptAsc(Collection<String> statuses,int attempts,long now);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select n from EmailNotice n where n.id=:id") Optional<EmailNotice> lockById(@Param("id") String id);
}
