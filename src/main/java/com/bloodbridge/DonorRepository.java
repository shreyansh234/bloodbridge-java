package com.bloodbridge;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
public interface DonorRepository extends JpaRepository<Donor,String>,JpaSpecificationExecutor<Donor> {
 Optional<Donor> findByAccountId(String accountId);
 Optional<Donor> findByIdAndAvailableTrueAndConsentTrue(String id);
 long countByConsentTrue();
 long countByConsentTrueAndAvailableTrue();
 @Query("select count(distinct lower(trim(d.city))) from Donor d where d.consent = true") long countCities();
}
