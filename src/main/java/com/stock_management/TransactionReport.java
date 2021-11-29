package com.stock_management;

public class TransactionReport {

    private String userId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public TransactionReport(String userId, String companyId, String action, int price, String status) {
        this.userId = userId;
        this.companyId = companyId;
        this.action = action;
        this.price = price;
        this.status = status;
    }

    private String companyId;
    private String action;
    private int price;
    private String status;

}