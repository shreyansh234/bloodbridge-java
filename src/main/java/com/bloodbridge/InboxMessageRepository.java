package com.bloodbridge;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxMessageRepository extends JpaRepository<InboxMessage,String> {
 Page<InboxMessage> findByThreadId(String threadId,Pageable pageable);
 boolean existsByThreadIdAndClientKey(String threadId,String key);
 long countByThreadIdAndSenderRole(String threadId,String role);
}
