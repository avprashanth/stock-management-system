package com.stock_management.stock_management;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Repository
public class StockManagementDAO {
    static Logger logger = Logger.getLogger(StockManagementDAO.class.getName());
    static final String DB_URL = "jdbc:mysql://10.0.0.199:3306/stock_management_db";
    static final String USER = "root";
    static final String PASS = "root";
    static int num = 3;
    static Scanner myObj = new Scanner(System.in);


    static SHA512Hasher hashObj = new SHA512Hasher();

    static String registerUser(Connection connection, String username, String password,
                               String address, String phoneNumber, String firstName, String lastName) {
        String response = "";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO users VALUES (?, ?, ?, ?, ?, ?);");
            String hashedPassword = hashObj.hash(password);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, hashedPassword);
            preparedStatement.setString(3, address);
            preparedStatement.setString(4, phoneNumber);
            preparedStatement.setString(5, firstName);
            preparedStatement.setString(6, lastName);

            preparedStatement.executeQuery();
            logger.info("inserted user1");
            response = " Registration successful";
        } catch (SQLException e) {
            response = " Registration Failed";
            e.printStackTrace();
        }
        return response;
    }

    static String validateUser(Connection conn, String userId, String password) {
        String response = "";
        try {

            PreparedStatement pstmt = conn.prepareStatement("Select password from users where user_id = ?;");
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            String stored_password = "";
            if(rs.next()){
                stored_password = rs.getString("password");
            }
            boolean success = hashObj.checkPassword(stored_password,password);
            if(success) {
                response = "You are authenticated";
            }
            else {
                response = "Authentication failed";
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return response;
    }

    public String stockBrokerLogin(Connection connection, String userId, String password) {
        String response = "";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("Select passwords from stockBrokers where broker_id = ?;");
            preparedStatement.setString(1, userId);
            ResultSet rs = preparedStatement.executeQuery();
            String stored_password = "";
            if(rs.next()){
                stored_password = rs.getString("password");
            }
            boolean success = hashObj.checkPassword(stored_password,password);
            if(success) {
                response = "You are authenticated";
            }
            else {
                response = "Authentication failed";
            }
        } catch (SQLException e) {
            response = " Login Failed ";
            e.printStackTrace();
        }
        return response;
    }

    public String performBuy(Connection connection, String userId, String companyId, String price, int quantity) {
        String response = "";
        String userBalance = getUserBalance(connection, userId);

        if(!canUserPurchase(connection, companyId, quantity, Integer.parseInt(userBalance))) {
            response = "Balance insufficient";
            return response;
        }

        int availableStocks = checkAvailableStocks(connection, companyId, quantity);

        String newRequestId = getMaxRequestIdFromTradeRequests(connection);

        if(availableStocks >= quantity) {
            String maxRequestIdOfUser = getMaxRequestIdOfUser(connection, userId);
            String updatedQuantity = String.valueOf(Integer.parseInt(getAvailableStocksOfUser(connection, maxRequestIdOfUser, userId, companyId)) + quantity);

            updateTradeRequests(connection,newRequestId,companyId,updatedQuantity,"Buy",price, "Success",null, null);
            updateUserBalance(connection, Integer.parseInt(userBalance) - (Integer.parseInt(price) * (quantity)), userId);
            updateCompanyStocks(connection, String.valueOf(availableStocks - quantity), companyId);
            response = "Transaction successful";
        } else {

            updateTradeRequests(connection,newRequestId,companyId,String.valueOf(quantity),"Sell",price,"InProgress",null, null);
            response = "Transaction placed on hold as the requested quantity does not match the stock availability";

        }
        return response;
    }

    public String performSell(Connection connection, String companyId, String price, int quantity, String userId) {
        String response = "";

        int netCountOfStocks = checkIfCompanyIsListedAtAskingPrice(connection, companyId, price, quantity);
        String newRequestId = getMaxRequestIdFromTradeRequests(connection);

        if(netCountOfStocks >= 0) {

            String userBalance = getUserBalance(connection, userId);
            String maxRequestIdOfUser = getMaxRequestIdOfUser(connection, userId);
            int netQuantity = Integer.parseInt(getAvailableStocksOfUser(connection, maxRequestIdOfUser, userId, companyId)) - quantity;
            if(netQuantity < 0) {
                response = "Transaction cancelled - Users cannot attempt to sell more than what they have";
                return response;
            }

            String updatedQuantity = String.valueOf(netQuantity);

            updateTradeRequests(connection,newRequestId,companyId,updatedQuantity,"Sell",price, "Success",null, null);
            updateUserBalance(connection, Integer.parseInt(userBalance) + (Integer.parseInt(price) * (quantity)), userId);
            updateCompanyStocks(connection, String.valueOf(netCountOfStocks + (2*quantity)), companyId);

            calculateAndUpdateGain(connection, companyId, userId, price, quantity);
            response = "Transaction successful";
        } else {

            updateTradeRequests(connection,newRequestId,companyId,String.valueOf(quantity),"Sell",price,"InProgress",null, null);
            response = "Transaction placed on hold as the demanded price does not match the stock listing price";

        }
        return response;
    }

    public int checkAvailableStocks(Connection conn, String companyId, int quantity) {
        int availableStocks = 0;
        try{
            PreparedStatement preparedStatement = conn.prepareStatement("Select quantity from companystock where company_id = ? quantity = ?");
            preparedStatement.setString(1,companyId);
            preparedStatement.setInt(2,quantity);

            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()) {
                availableStocks = Integer.parseInt(resultSet.getString(1)); // dummy column index
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return availableStocks;
    }

    public String getUserBalance(Connection connection, String userId) {
        String balance = "";
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("Select acc_balance from customer where customer_id = ?");
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                balance = resultSet.getString(1);
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return balance;
    }

    public boolean canUserPurchase(Connection connection, String companyId, int quantity, int userBalance) {
        int listedPrice = 0;
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("Select price from companystock where company_id = ?");
            preparedStatement.setString(1,companyId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                listedPrice += resultSet.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return userBalance >= listedPrice * quantity;
    }

    public void updateTradeRequests(Connection connection, String requestId,
                                    String companyId, String quantity, String action, String price, String status, String updatedTime, String cancelTime) {

        try{
            Date javaDate = new Date(0);
            java.sql.Timestamp date = new java.sql.Timestamp (javaDate.getTime());
            String currDate = date.toString();
            PreparedStatement preparedStatement = connection.prepareStatement("Insert into traderequest values(requestId, " +
                    "companyId, quantity, action, price, currDate, updatedTime, cancelTime )");
            preparedStatement.setString(1,requestId);
            preparedStatement.setString(2,companyId);
            preparedStatement.setString(3,quantity);
            preparedStatement.setString(4,action);
            preparedStatement.setString(5,price);
            preparedStatement.setString(6,status);
            preparedStatement.setString(7,currDate);
            preparedStatement.setString(8,updatedTime);
            preparedStatement.setString(9,cancelTime);
            preparedStatement.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateUserBalance(Connection connection, int balance, String userId) {
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("Update user set acc_balance = ? where customer_id = ?");
            preparedStatement.setString(1, String.valueOf(balance));
            preparedStatement.setString(2, userId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getMaxRequestIdFromTradeRequests(Connection connection) {
        String maxRequestId = "Select max request_id from trade_request";
        String newRequestId = "0";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(maxRequestId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                newRequestId = String.valueOf(Integer.parseInt(resultSet.getString(1)) + 1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return newRequestId;
    }

    public  String getMaxRequestIdOfUser(Connection connection, String userId) {
        String maxUserRequestId = "";
        String maxRequestIdOfUser = "Select max request_id from traderequest where customer_id = ?";
        try{
            PreparedStatement preparedStatement = connection.prepareStatement(maxRequestIdOfUser);
            preparedStatement.setString(1,userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()) {
                maxUserRequestId = String.valueOf(Integer.parseInt(resultSet.getString(1))+1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return  maxRequestIdOfUser;
    }

    public String getAvailableStocksOfUser(Connection connection, String requestId, String userId, String companyId) {
        String availableStocksOfUser = "Select quantity from traderequest where request_id = ? and customer_id = ?  and company_id = ?";
        String availableStocks = "";
        try{
            PreparedStatement preparedStatement = connection.prepareStatement(availableStocksOfUser);
            preparedStatement.setString(1,requestId);
            preparedStatement.setString(2,userId);
            preparedStatement.setString(3, companyId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                availableStocks = resultSet.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return availableStocks;
    }

    public void updateCompanyStocks(Connection connection, String availableStocks, String companyId) {
        String updateCompanyStocks = "Update Company set quantity = ? where company_id = ?";
        try{
            PreparedStatement preparedStatement = connection.prepareStatement(updateCompanyStocks);
            preparedStatement.setString(1,availableStocks);
            preparedStatement.setString(2,companyId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public int checkIfCompanyIsListedAtAskingPrice(Connection connection, String companyId, String askingPrice, int quantity) {
        int availableStocks = 0;
        String selectCompanyPrice = "Select quantity from companystock where company_id = ? and price >= ?";
        try{
            PreparedStatement preparedStatement = connection.prepareStatement(selectCompanyPrice);
            preparedStatement.setString(1, companyId);
            preparedStatement.setString(2, askingPrice);

            ResultSet resultSet = preparedStatement.executeQuery();

            while(resultSet.next()) {
                availableStocks = resultSet.getInt(0);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return availableStocks-quantity;
    }

    public void calculateAndUpdateGain(Connection connection, String companyId, String userId, String price, int quantity) {
        int tempPrice = 0, tempQuantity = 0;
        int totalPrice = tempPrice * tempQuantity;
        int totalQuantity = tempQuantity;
        try{
            Date date = new Date(0);
            PreparedStatement preparedStatement = connection.prepareStatement("Select price, quantity from traderequest where company_id = ? and customer_id = ? and date < ?");
            preparedStatement.setString(1,companyId);
            preparedStatement.setString(2,userId);
            preparedStatement.setDate(3,date);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                tempPrice += Integer.parseInt(resultSet.getString(1));
                tempQuantity += Integer.parseInt(resultSet.getString(2));
                totalPrice += tempPrice * tempQuantity;
                totalQuantity += tempQuantity;
            }

            double pricePerQuantity = totalPrice/totalQuantity;
            double gainOrloss = (Double.valueOf(price) - pricePerQuantity) * quantity;

            writeGainOrLossToTable(connection, companyId, userId, String.valueOf(gainOrloss));

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public void writeGainOrLossToTable(Connection connection, String companyId, String userId, String gainOrLoss ) {

        try{
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM stock_details WHERE company_id = ? and customer_id = ?");
            preparedStatement.setString(1,companyId);
            preparedStatement.setString(2,userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                preparedStatement = connection.prepareStatement("UPDATE stock_details SET gain = ? WHERE company_id = ? and customer_id = ?");
                preparedStatement.setString(1, gainOrLoss);
                int count = preparedStatement.executeUpdate();
                if(count > 0) {
                    logger.info("Stock Details updated successfully");
                } else {
                    logger.info("Failed to update stock details");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public List<String> getTransactionReports(Connection connection, String userId, String status) {
        List<String> transactionReports = new ArrayList<>();
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM traderequest where userId = ? and status = ?");
            preparedStatement.setString(1,userId);
            preparedStatement.setString(2,status);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String row = "";
                int i=0;
                while (resultSet.getString(i) != null) {
                    row += resultSet.getString(i) + ", ";
                    i++;
                }
                transactionReports.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactionReports;
    }


}


//User Registration
//User Login/Pass-Fail
//Stock Broker login
//Trade/Buy-Sell
//Request recommendations
//Update stock prices by stock broker
//Trades on hold

