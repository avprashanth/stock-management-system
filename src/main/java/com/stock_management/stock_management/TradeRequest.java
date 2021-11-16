package com.stock_management.stock_management;

public class TradeRequest {

    private String request_id;
    private String company_id;
    private int quantity;

    public TradeRequest(String request_id, String company_id, int quantity) {
        this.request_id = request_id;
        this.company_id = company_id;
        this.quantity = quantity;
    }

    public String getRequest_id() {
        return request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    public String getCompany_id() {
        return company_id;
    }

    public void setCompany_id(String company_id) {
        this.company_id = company_id;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}