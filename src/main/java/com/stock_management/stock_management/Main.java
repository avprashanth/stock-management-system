package com.stock_management.stock_management;

import org.apache.log4j.Logger;

import java.sql.*;
import java.util.Scanner;

public class Main {
    static Logger logger = Logger.getLogger(Main.class.getName());
    static final String DB_URL = "jdbc:mysql://10.0.0.199:3306/stock_management_db";
    static final String USER = "root";
    static final String PASS = "root";
    static int num = 3;
    static Scanner myObj = new Scanner(System.in);

    static int addUser(Connection conn) {
        String username;
        System.out.println("Enter username");
        username = myObj.nextLine();
        System.out.println(username);
        try {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users VALUES (?, 'test1', 'address1', '10010010', ?, 'thonupu');");
            pstmt.setString(1, String.valueOf(num++));
            pstmt.setString(2, username);
            int rs = pstmt.executeUpdate();
            logger.info("inserted user1");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1;
    }

    static void getFirstUser(Connection conn) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("Select passwords from users where user_id = ?;");
            pstmt.setString(1, "1");
            ResultSet rs = pstmt.executeQuery();
            System.out.println(rs.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
            Scanner obj = new Scanner(System.in);

            while(true) {
                System.out.println("Enter option");
                String c = obj.nextLine();
                switch (c) {
                    case "1":
                        addUser(conn);
                        break;
                    case "2":
                        getFirstUser(conn);
                        break;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}


//User Registration
//User Login/Pass-Fail
//Stock Broker login
//Trade/Buy-Sell
//Request recommendations
//Update stock prices by stock broker
//Trades on hold

