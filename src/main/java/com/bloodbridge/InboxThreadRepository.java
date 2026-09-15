package com.bloodbridge;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface InboxThreadRepository extends JpaRepository<InboxThread,String>,JpaSpecificationExecutor<InboxThread> {
 Optional<InboxThread> findBySourceKey(String key);
 long countByOwnerIdAndMemberUnreadTrue(String ownerId);
 long countByAdminUnreadTrue();
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select t from InboxThread t where t.id=:id")
 Optional<InboxThread> lockById(@Param("id") String id);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select t from InboxThread t where t.sourceKey=:key")
 Optional<InboxThread> lockBySourceKey(@Param("key") String key);
}
