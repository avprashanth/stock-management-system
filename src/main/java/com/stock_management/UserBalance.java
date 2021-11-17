package com.stock_management;


public class UserBalance {
    public UserBalance(int balance) {
        this.balance = balance;
    }

    public UserBalance() {

    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    private int balance;
}
