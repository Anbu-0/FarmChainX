package com.ecobazaar.ecobazaar.repository;

import com.ecobazaar.ecobazaar.model.AdminPromotionRequest;
import com.ecobazaar.ecobazaar.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdminPromotionRequestRepository extends JpaRepository<AdminPromotionRequest, Long> {

    List<AdminPromotionRequest> findByApprovedFalseAndRejectedFalse();

    boolean existsByUserAndApprovedFalseAndRejectedFalse(User user);

    AdminPromotionRequest findByUserAndApprovedFalseAndRejectedFalse(User user);
}
