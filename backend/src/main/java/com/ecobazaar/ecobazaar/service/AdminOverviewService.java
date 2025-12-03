package com.ecobazaar.ecobazaar.service;

import org.springframework.stereotype.Service;

import com.ecobazaar.ecobazaar.dto.AdminOverview;
import com.ecobazaar.ecobazaar.repository.FeedbackRepository;
import com.ecobazaar.ecobazaar.repository.ProductRepository;
import com.ecobazaar.ecobazaar.repository.SupplyChainLogRepository;
import com.ecobazaar.ecobazaar.repository.UserRepository;

@Service
public class AdminOverviewService {

	private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final SupplyChainLogRepository supplyChainLogRepo;
    private final FeedbackRepository feedbackRepo;

    public AdminOverviewService(UserRepository userRepo,
                                ProductRepository productRepo,
                                SupplyChainLogRepository supplyChainLogRepo,
                                FeedbackRepository feedbackRepo) {
        this.userRepo = userRepo;
        this.productRepo = productRepo;
        this.supplyChainLogRepo = supplyChainLogRepo;
        this.feedbackRepo = feedbackRepo;
    }

    public AdminOverview getOverview() {
    	return new AdminOverview(
    			userRepo.count(),
    			productRepo.count(),
    			supplyChainLogRepo.count(),
    			feedbackRepo.count());
    }

}