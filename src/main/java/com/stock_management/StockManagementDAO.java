package com.stock_management;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Repository;

import java.net.ConnectException;
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
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO users VALUES (?, ?, ?, ?, ?, ?);");
            String hashedPassword = hashObj.hash(password);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, hashedPassword);
            preparedStatement.setString(3, address);
            preparedStatement.setString(4, phoneNumber);
            preparedStatement.setString(5, firstName);
            preparedStatement.setString(6, lastName);
            connection.setAutoCommit(false);
            preparedStatement.executeUpdate();

            if(role.equals("Customer"))
                response = updateCustomerTable(connection, username);
            else if(role.equals("Admin")){
                response = updateStockBrokerTable(connection, username);
            }

            if(response.equals("success")) {
                connection.commit();
            } else {
                response = "Failed to register. Please try again later";
                return response;
            }

            logger.info("Inserted user");
            response = " Registration successful";
        } catch (SQLException e) {
            response = " Registration Failed. Please try again later";
            e.printStackTrace();
        }
        return response;
    }

    String updateStockBrokerTable(Connection connection, String username) {
        String response = "";
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO stockbroker VALUES (?);");
            preparedStatement.setString(1,username);
            preparedStatement.executeUpdate();
            response = "success";
        }catch (SQLException e){
            response = "failure";
            e.printStackTrace();
        }
        return response;
    }

    String updateCustomerTable(Connection connection, String username) {
        String response = "";
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO customer VALUES (?, ?);");
            preparedStatement.setString(1,username);
            preparedStatement.setInt(2,0);
            preparedStatement.executeUpdate();
            response = "success";
        }catch (SQLException e){
            response = "fail";
            e.printStackTrace();
        }
        return response;
    }

    static String validateUser(Connection conn, String userId, String password) {
        String response = "";
        try {

            PreparedStatement pstmt = conn.prepareStatement("Select password from users u join customer c on u.user_id = c.customer_id where u.user_id = ?;");
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
                response = "Incorrect password. Authentication failed";
            }
        } catch (SQLException e) {
            response = "Unexpected error. Please try again later";
            e.printStackTrace();
        }
        return response;
    }

    public String stockBrokerLogin(Connection connection, String userId, String password) {
        String response = "";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("Select u.password from users u join stockbroker s  on u.user_id = s.broker_id where u.user_id = ?;");
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
                response = "Incorrect password. Authentication failed";
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
        connection.setAutoCommit(false);
        int availableStocks = checkAvailableStocks(connection, companyId);
        if(availableStocks >= quantity) {
            String requestId = UUID.randomUUID().toString();
            int stockPrice = getCompanyPrice(connection, companyId);
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
            response = updateUserBalance(connection, userBalance - (price * quantity), userId);
            if(!response.equals("success")) {
                response = updateCompanyStocks(connection, availableStocks - quantity, companyId);
            }

            if(response.equals("success")) {
                connection.commit();
                response = "Transaction successful";
            }
        } else {
            String requestId = UUID.randomUUID().toString();
            String status = "failure";

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

    private int getCompanyPrice(Connection connection, String companyId) throws SQLException {
        String query = "select price from companystock where company_id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, companyId);
        ResultSet set = statement.executeQuery();
        set.next();
        int stockPrice = set.getInt("price");
        return  stockPrice;
    }

    public String performSell(Connection connection, String companyId, int price, int quantity, String userId, String batchId) throws SQLException {
        String response = "";
        int userBalance = getUserBalance(connection, userId);
        String status = "";
        String query = "select quantity from traderequest where user_id = ? and company_id = ? and batch_id = ? and action = 'buy' and status = 'success'" ;
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, userId);
        statement.setString(2, companyId);
        statement.setString(3, batchId);
        ResultSet set = statement.executeQuery();
        int totalStocksAvailable = 0;
        while (set.next())
        {
            totalStocksAvailable += set.getInt("quantity");
        }
        String query1 = "select quantity from traderequest where user_id = ? and company_id = ? and batch_id = ? and action = 'sell'";
        PreparedStatement statement1 = connection.prepareStatement(query1);
        statement1.setString(1, userId);
        statement1.setString(2, companyId);
        statement1.setString(3, batchId);
        ResultSet resultSet = statement1.executeQuery();
        int soldStocks = 0;
        while (resultSet.next())
        {
            soldStocks += resultSet.getInt("quantity");
        }
        int availableQuantity = totalStocksAvailable - soldStocks;
        int availableStocks = checkAvailableStocks(connection, companyId);
        //user level stocks
        if(availableQuantity >= quantity)
        {
            String requestId = UUID.randomUUID().toString();
            int stockPrice = getCompanyPrice(connection, companyId);
            if(stockPrice == price) status = "success";
            else status = "failure";

            String sql = "INSERT INTO traderequest VALUES (?, ?, ?, 'sell', ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)";
            PreparedStatement statement2 = connection.prepareStatement(sql);
            statement2.setString(1, requestId);
            statement2.setString(2, companyId);
            statement2.setInt(3, quantity);
            statement2.setString(4, status);
            statement2.setInt(5, price);
            statement2.setString(6, batchId);
            statement2.setString(7, userId);
            statement2.executeUpdate();
            if(status.equals("success"))
                updateUserBalance(connection, userBalance + (price * quantity), userId);
            updateCompanyStocks(connection, availableStocks + quantity, companyId);
            response = "Transaction successful";
        }
        else
            response = "Transaction fail";
        if(status.equals("success")) {
            String buyQuery =
                    "select price from traderequest" +
                            " where user_id = ? and company_id = ? and batch_id = ? and status = 'success' and action = 'buy'";
            PreparedStatement statement3 = connection.prepareStatement(buyQuery);
            statement3.setString(1, userId);
            statement3.setString(2, companyId);
            statement3.setString(3, batchId);
            ResultSet rs = statement3.executeQuery();
            rs.next();
            int buyprice = rs.getInt("price");
            int gain = (price - buyprice) * quantity;
            String insertDetails = "insert into stockdetails values (?, ?, ?)";
            PreparedStatement statement4 = connection.prepareStatement(insertDetails);
            statement4.setString(1, userId);
            statement4.setString(2, companyId);
            statement4.setInt(3, gain);
            statement4.executeUpdate();
        }
        return response;
    }

    public int checkAvailableStocks(Connection conn, String companyId) {
        int availableStocks = 0;
        try{
            PreparedStatement preparedStatement = conn.prepareStatement("Select available_quantity from companystock where company_id = ? order by created_time desc limit 1");
            preparedStatement.setString(1,companyId);

            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()) {
                availableStocks = resultSet.getInt("available_quantity"); // dummy column index
            }

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
            while(resultSet.next()) {
                balance = resultSet.getInt("acc_balance");
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
            while(resultSet.next()) {
                listedPrice = resultSet.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return userBalance >= listedPrice * quantity;
    }

    public void updateTradeRequests(Connection connection, String requestId,
                                    String companyId, String quantity, String action, int price, String status, String updatedTime, String cancelTime) {

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
            preparedStatement.setInt(5,price);
            preparedStatement.setString(6,status);
            preparedStatement.setString(7,currDate);
            preparedStatement.setString(8,updatedTime);
            preparedStatement.setString(9,cancelTime);
            preparedStatement.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String updateUserBalance(Connection connection, int balance, String userId) {
        String response = "";
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("Update customer set acc_balance = ? where customer_id = ?");
            preparedStatement.setString(1, String.valueOf(balance));
            preparedStatement.setString(2, userId);
            preparedStatement.executeUpdate();
            response = "success";
        } catch (SQLException e) {
            response = "fail";
            e.printStackTrace();
        }
        return response;
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

    public String updateCompanyStocks(Connection connection, int availableStocks, String companyId) {
        String response = "";
        String updateCompanyStocks = "Update Company set quantity = ? where company_id = ?";
        try{
            PreparedStatement preparedStatement = connection.prepareStatement(updateCompanyStocks);
            preparedStatement.setInt(1,availableStocks);
            preparedStatement.setString(2,companyId);
            response = "success";
        } catch (SQLException e) {
            response = "failure";
            e.printStackTrace();
        }
        return response;
    }


    public int checkIfCompanyIsListedAtAskingPrice(Connection connection, String companyId, int askingPrice, int quantity) {
        int availableStocks = 0;
        String selectCompanyPrice = "Select quantity from companystock where company_id = ? and price >= ?";
        try{
            PreparedStatement preparedStatement = connection.prepareStatement(selectCompanyPrice);
            preparedStatement.setString(1, companyId);
            preparedStatement.setInt(2, askingPrice);

            ResultSet resultSet = preparedStatement.executeQuery();

            while(resultSet.next()) {
                availableStocks = resultSet.getInt(0);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return availableStocks;
    }

    public void calculateAndUpdateGain(Connection connection, String companyId, String userId, int price, int quantity) {
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
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM stockdetails WHERE company_id = ? and customer_id = ?");
            preparedStatement.setString(1,companyId);
            preparedStatement.setString(2,userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()) {
                preparedStatement = connection.prepareStatement("UPDATE stockdetails SET gain = ? WHERE company_id = ? and customer_id = ?");
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
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM traderequest where user_id = ? and status = ?");
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
            PreparedStatement statement = conn.prepareStatement("Select company_id,price,available_quantity from companystock order by created_time");
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                String id = rs.getString("company_id");
                int price = rs.getInt("price");
                int quantity = rs.getInt("available_quantity");

                CompanyStock stock = new CompanyStock(id,price,quantity);

                companyStocks.add(stock);

            }
            return companyStocks;
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public void updateInProgressRequests(Connection connection) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT user_id, company_id, price, " +
                    "quantity, action, request_id, batch_id FROM traderequest WHERE status = ? order by requested_time");
            preparedStatement.setString(1, "failure");
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String userId = resultSet.getString(1);
                String companyId = resultSet.getString(2);
                int price = resultSet.getInt(3);
                int quantity = resultSet.getInt(4);
                String action = resultSet.getString(5);
                String requestId = resultSet.getString(6);
                if (action.equals("buy"))
                    UpdatePurchaseRequests(connection, userId, companyId, price, quantity, requestId);
                else if (action.equals("sell"))
                    updateSellRequests(connection, userId, companyId, price, quantity, requestId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void UpdatePurchaseRequests(Connection connection, String userId, String companyId, int price,
                                       int quantity, String requestId) {

        int userBalance = getUserBalance(connection, userId);
        if(!canUserPurchase(connection, companyId, quantity, userBalance)) {
            return;
        }
        int availableStocks = checkAvailableStocks(connection, companyId);
        if(availableStocks < quantity) {
            return;
        }
        int updatedBalance = userBalance + (price * quantity);

        int updatedStockQuantity = availableStocks - quantity;

        updateUserBalance(connection, updatedBalance, userId);
        updateCompanyStocks(connection, updatedStockQuantity, companyId);

        try{
            PreparedStatement preparedStatement = connection.prepareStatement("Update traderequest set status = ? where request_id = ?");
            preparedStatement.setString(1, "success");
            preparedStatement.setString(2,requestId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


    public void updateSellRequests(Connection connection, String userId, String companyId,
                                   int price, int quantity, String requestId) {

        int count = checkIfCompanyIsListedAtAskingPrice(connection, companyId, price, quantity);
        int userBalance = getUserBalance(connection, userId);
        int availableStocks = checkAvailableStocks(connection, companyId);
        if(count > 0) {
            try{
                PreparedStatement preparedStatement = connection.prepareStatement("Update traderequest set status = ? where request_id = ?");
                preparedStatement.setString(1, "success");
                preparedStatement.setString(2,requestId);
                preparedStatement.executeUpdate();

                int updatedBalance = userBalance + price * quantity;
                int updatedStockQuantity = availableStocks + quantity;
                updateUserBalance(connection, updatedBalance, userId);
                updateCompanyStocks(connection, updatedStockQuantity, companyId);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public List<Portfolio> getPortfolioDetails(Connection connection, String userId) {
        List<Portfolio> resultList = new ArrayList<>();
        try{
            PreparedStatement preparedStatement = connection.prepareStatement("Select * from stockdetails where customer_id = ?");
            preparedStatement.setString(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()) {
                Portfolio portfolio = new Portfolio();
                portfolio.setCustomer_id(userId);
                portfolio.setCompany_id(resultSet.getString(2));
                portfolio.setGain(resultSet.getInt(3));
                resultList.add(portfolio);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultList;
    }

}


//User Registration
//User Login/Pass-Fail
//Stock Broker login
//Trade/Buy-Sell
//Request recommendations
//Update stock prices by stock broker
//Trades on hold

