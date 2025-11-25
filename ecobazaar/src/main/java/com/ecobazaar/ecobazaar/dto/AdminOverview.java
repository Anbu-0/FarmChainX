package com.ecobazaar.ecobazaar.dto;

public class AdminOverview {
	
	private long totalUsers;
    private long totalProducts;
    private long totalLogs;
    private long totalFeedbacks;
    
    public AdminOverview() {}

    public AdminOverview(long totalUsers, long totalProducts, long totalLogs, long totalFeedbacks) {
        this.totalUsers = totalUsers;
        this.totalProducts = totalProducts;
        this.totalLogs = totalLogs;
        this.totalFeedbacks = totalFeedbacks;
    }

	public long getTotalUsers() {
		return totalUsers;
	}

	public void setTotalUsers(long totalUsers) {
		this.totalUsers = totalUsers;
	}

	public long getTotalProducts() {
		return totalProducts;
	}

	public void setTotalProducts(long totalProducts) {
		this.totalProducts = totalProducts;
	}

	public long getTotalLogs() { 
		return totalLogs;
	}

	public void setTotalLogs(long totalLogs) { 
		this.totalLogs = totalLogs;
	}

	public long getTotalFeedbacks() {
		return totalFeedbacks;
	}

	public void setTotalFeedbacks(long totalFeedbacks) {
		this.totalFeedbacks = totalFeedbacks;
	}

 

}