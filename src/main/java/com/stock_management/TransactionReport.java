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

    public TransactionReport(String userId, String companyId, String action, int quantity, int price, String status) {
        this.userId = userId;
        this.companyId = companyId;
        this.action = action;
        this.quantity = quantity;
        this.price = price;
        this.status = status;
    }

    private String companyId;
    private String action;

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    private int quantity;
    private int price;
    private String status;

}