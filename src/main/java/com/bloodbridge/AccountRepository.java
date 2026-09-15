package com.bloodbridge;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AccountRepository extends JpaRepository<Account,String> {
 Optional<Account> findByEmail(String email); Optional<Account> findByExternalId(String externalId);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select a from Account a where a.id=:id")
 Optional<Account> lockById(@org.springframework.data.repository.query.Param("id") String id);
}
