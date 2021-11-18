package com.stock_management;

public class TradeRequest {

    public TradeRequest() {
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

    public String getBatch_id() {
        return batch_id;
    }

    public void setBatch_id(String batch_id) {
        this.batch_id = batch_id;
    }

    public TradeRequest(String request_id, String company_id, int quantity, String batch_id,int price) {
        this.request_id = request_id;
        this.company_id = company_id;
        this.quantity = quantity;
        this.batch_id = batch_id;
        this.price = price;
    }

    private String request_id;
    private String company_id;
    private int quantity;
    private String batch_id;

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    private int price;

}
