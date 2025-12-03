package com.ecobazaar.ecobazaar.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ecobazaar.ecobazaar.model.SupplyChainLog;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface SupplyChainLogRepository extends JpaRepository<SupplyChainLog, Long> {

    List<SupplyChainLog> findByProductIdOrderByTimestampAsc(Long productId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)

    Optional<SupplyChainLog> findTopByProductIdOrderByTimestampDesc(Long productId);

    @Query("""
         SELECT log FROM SupplyChainLog log
        WHERE log.toUserId = :retailerId
          AND log.confirmed = false
          AND log.rejected = false
          AND log.id = (
                SELECT MAX(l2.id)
                FROM SupplyChainLog l2
                WHERE l2.productId = log.productId
          )
        """)
    Page<SupplyChainLog> findPendingForRetailer(@Param("retailerId") Long retailerId, Pageable pageable);
}