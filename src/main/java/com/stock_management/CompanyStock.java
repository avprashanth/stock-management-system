package com.stock_management;


public class CompanyStock {

    public CompanyStock() {
    }

    public CompanyStock(String company_id, int price, int available_quantity) {
        this.company_id = company_id;
        this.price = price;
        this.available_quantity = available_quantity;
    }

    private String company_id;

    public String getCompany_id() {
        return company_id;
    }

    public void setCompany_id(String company_id) {
        this.company_id = company_id;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getAvailable_quantity() {
        return available_quantity;
    }

    public void setAvailable_quantity(int available_quantity) {
        this.available_quantity = available_quantity;
    }

    private int price;
    private int available_quantity;


    }
