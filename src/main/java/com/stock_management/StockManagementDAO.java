package com.stock_management;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

@Repository
public class StockManagementDAO {
    static Logger logger = Logger.getLogger(StockManagementDAO.class.getName());
    static final String DB_URL = "jdbc:mysql://10.0.0.199:3306/stock_management_db";
    static final String USER = "root";
    static final String PASS = "root";
    static Scanner myObj = new Scanner(System.in);

    static SHA512Hasher hashObj = new SHA512Hasher();

    String registerUser(Connection connection, String username, String password,
                               String address, String phoneNumber, String firstName, String lastName, String role) {
        String response = "";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO users VALUES (?, ?, ?, ?, ?, ?, ?);");
            String hashedPassword = hashObj.hash(password);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, hashedPassword);
            preparedStatement.setString(3, address);
            preparedStatement.setString(4, phoneNumber);
            preparedStatement.setString(5, firstName);
            preparedStatement.setString(6, lastName);
            preparedStatement.setString(7, role);

            preparedStatement.executeQuery();

            if(role.equals("Admin")) {
                updateStockBrokerTable(connection, username);
            } else if (role.equals("Customer")) {
                updateCustomerTable(connection, username);
            } else {
                response = "Role should either be Admin/Customer";
                return response;
            }

            logger.info("Inserted user");
            response = " Registration successful";
        } catch (SQLException e) {
            response = " Registration Failed";
            e.printStackTrace();
        }
        return response;
    }

    void updateStockBrokerTable(Connection connection, String username) {
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO StockBroker VALUES (?);");
            preparedStatement.setString(1,username);
            preparedStatement.executeUpdate();
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    void updateCustomerTable(Connection connection, String username) {
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO Customer VALUES (?, ?);");
            preparedStatement.setString(1,username);
            preparedStatement.setInt(2,0);
            preparedStatement.executeUpdate();
        }catch (SQLException e){
            e.printStackTrace();
        }
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

    public String generateBatchId(Connection connection, int price, String companyId, String userId) throws SQLException {

        String sql = "select batch_id from traderequest where company_id = ? and price = ? and user_id = ? and status = 'success'";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, companyId);
        statement.setInt(2, price);
        statement.setString(3, userId);
        ResultSet set = statement.executeQuery();
        if(!set.next()) {
            return companyId + userId + price;
        }
        else {
            return set.getString("batch_id");
        }
    }

    public String performBuy(Connection connection, String userId, String companyId, int price, int quantity) throws SQLException {
        String response = "";
        int userBalance = getUserBalance(connection, userId);

        if(!canUserPurchase(connection, companyId, quantity, userBalance)) {
            response = "Balance insufficient";
            return response;
        }
        int availableStocks = checkAvailableStocks(connection, companyId, quantity);
        if(availableStocks >= quantity) {
            String requestId = UUID.randomUUID().toString();
            String query = "select price from companystock where company_id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, companyId);
            ResultSet set = statement.executeQuery();
            set.next();
            int stockPrice = set.getInt("price");
            String status = "";
            if(stockPrice == price) status = "success";
            else status = "failure";

            String batchId = generateBatchId(connection, price, companyId, userId);

            String sql = "INSERT INTO traderequest VALUES (?, ?, ?, 'buy', ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)";

            PreparedStatement statement1 = connection.prepareStatement(sql);
            statement1.setString(1, requestId);
            statement1.setString(2, companyId);
            statement1.setInt(3, quantity);
            statement1.setString(4, status);
            statement1.setInt(5, price);
            statement1.setString(6, batchId);
            statement1.setString(7, userId);
            statement1.executeUpdate();

            response = "Transaction successful";
        } else {
            String requestId = UUID.randomUUID().toString();
            String query = "select price from companystock where company_id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, companyId);
            ResultSet set = statement.executeQuery();
            set.next();
            int stockPrice = set.getInt("price");
            String status = "";
            if(stockPrice == price) status = "success";
            else status = "failure";

            String sql = "INSERT INTO traderequest VALUES (?, ?, ?, 'buy', ?, ?, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)";

            PreparedStatement statement1 = connection.prepareStatement(sql);
            statement1.setString(1, requestId);
            statement1.setString(2, companyId);
            statement1.setInt(3, quantity);
            statement1.setString(4, status);
            statement1.setInt(5, price);
            statement1.setString(6, userId);
            statement1.executeUpdate();
            response = "Transaction placed on hold as the requested quantity does not match the stock availability";
        }
        return response;
    }

//    public String performSell(Connection connection, String companyId, String price, int quantity, String userId, String batchId) {
//        String response = "";
//
//        int netCountOfStocks = checkIfCompanyIsListedAtAskingPrice(connection, companyId, price, quantity);
//
//        if(netCountOfStocks >= 0) {
//
//            int userBalance = getUserBalance(connection, userId);
//            if(netQuantity < 0) {
//                response = "Transaction cancelled - Users cannot attempt to sell more than what they have";
//                return response;
//            }
//
//            String updatedQuantity = String.valueOf(netQuantity);
//
//            updateTradeRequests(connection,newRequestId,companyId,updatedQuantity,"Sell",price, "Success",null, null);
//            updateUserBalance(connection, userBalance + (Integer.parseInt(price) * (quantity)), userId);
//            updateCompanyStocks(connection, String.valueOf(netCountOfStocks + (2*quantity)), companyId);
//
//            calculateAndUpdateGain(connection, companyId, userId, price, quantity);
//            response = "Transaction successful";
//        } else {
//
//            updateTradeRequests(connection,newRequestId,companyId,String.valueOf(quantity),"Sell",price,"InProgress",null, null);
//            response = "Transaction placed on hold as the demanded price does not match the stock listing price";
//
//        }
//        return response;
//    }

    public int checkAvailableStocks(Connection conn, String companyId, int quantity) {
        int availableStocks = 0;
        try{
            PreparedStatement preparedStatement = conn.prepareStatement("Select available_quantity from companystock where company_id = ?");
            preparedStatement.setString(1,companyId);

            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            availableStocks = resultSet.getInt("available_quantity"); // dummy column index

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return availableStocks;
    }

    public int getUserBalance(Connection connection, String userId) {
        int balance = 0;
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("Select acc_balance from customer where customer_id = ?");
            preparedStatement.setString(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            balance = resultSet.getInt("acc_balance");
        } catch(SQLException e) {
            e.printStackTrace();
        }
        System.out.println("&*&*&**" + balance);
        return balance;
    }

    public boolean canUserPurchase(Connection connection, String companyId, int quantity, int userBalance) {
        int listedPrice = 0;
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("Select price from companystock where company_id = ?");
            preparedStatement.setString(1,companyId);
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            listedPrice = resultSet.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return userBalance >= listedPrice * quantity;
    }

    public void updateTradeRequests(Connection connection, String requestId,
                                    String companyId, String quantity, String action, String price, String status, String updatedTime, String cancelTime) {

        try{
            Date javaDate = new Date(0);
            Timestamp date = new Timestamp (javaDate.getTime());
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


    public List<CompanyStock> getCompanyList(Connection conn) {
        List<CompanyStock> companyStocks = new ArrayList<CompanyStock>();
        try {
            PreparedStatement statement = conn.prepareStatement("Select company_id,price,available_quantity,share_type from CompanyStock order by created_time");
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                String id = rs.getString("company_id");
                int price = rs.getInt("price");
                int quantity = rs.getInt("available_quantity");
                String shareType = rs.getString("share_type");

                CompanyStock stock = new CompanyStock(id,price,quantity,shareType);

                companyStocks.add(stock);

            }
            return companyStocks;
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage());
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

