package com.stock_management;

public class UserBatch {
    public UserBatch() {
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    private String batchId;
    private int price;
    private int quantity;
    private String companyId;

    public UserBatch(String batchId, int price, int quantity, String companyId) {
        this.batchId = batchId;
        this.price = price;
        this.quantity = quantity;
        this.companyId = companyId;
    }
}
