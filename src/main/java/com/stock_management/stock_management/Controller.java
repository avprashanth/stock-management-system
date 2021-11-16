package com.stock_management.stock_management;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@RestController
public class Controller {
    StockManagementDAO stockDao = new StockManagementDAO();
    static Logger logger = Logger.getLogger(Main.class.getName());
    static final String DB_URL = "jdbc:mysql://localhost:3306/stock_management_db";
    static final String USER = "root";
    static final String PASS = "root";
    static int num = 4;
    static Scanner myObj = new Scanner(System.in);


    @PostMapping("/userLogin")
    String userLogin(@RequestBody String userId, @RequestBody String password) {
        String response = "";
        try {
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            response = stockDao.validateUser(connection, userId, password);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return response;
    }

    @GetMapping("/stockBrokerLogin")
    String stockBrokerLogin(@RequestBody String userId, @RequestBody String password) {
        String response = "";
        try {
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            response = stockDao.stockBrokerLogin(connection, userId, password);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return response;
    }

    @PostMapping("/registerUser")
    String registerUser(@RequestBody String userId, @RequestBody String password,
                        @RequestBody String address, @RequestBody String phoneNumber,
                        @RequestBody String firstName, @RequestBody String lastName) {
        String response = "";
        try {
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            response = stockDao.registerUser(connection, userId, password, address, phoneNumber, firstName, lastName);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return response;
    }

    @PostMapping("/upload")
    String uploadFile(@RequestParam("file") MultipartFile file) {
        String message = "";

        if (ExcelHelper.hasExcelFormat(file)) {
            try {
                Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                ExcelHelper.excelToCompanyStocks(file.getInputStream(), conn);

                message = "Uploaded the file successfully: " + file.getOriginalFilename();
                return  message;
                //return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
            } catch (Exception e) {
                message = "Could not upload the file: " + file.getOriginalFilename() + "!";
                return message;
                //return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
            }
        }

        message = "Please upload an excel file!";
        return message;
       // return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseMessage(message));
    }

    @GetMapping("/companies")
    List<CompanyStock> GetCompanies() {
        List<CompanyStock> res = null;
        try {
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);

            res = getCompanyList(conn);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    static List<CompanyStock> getCompanyList(Connection conn) {
        try {
            PreparedStatement statement = conn.prepareStatement("Select company_id,price,available_quantity,share_type from CompanyStock");
            ResultSet rs = statement.executeQuery();
            List<CompanyStock> companyStocks = new ArrayList<CompanyStock>();

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

    @PostMapping("/cancelRequest")
    String cancelRequest(@RequestBody String userId, @RequestBody String requestId) {
        boolean response = false;
        try {
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            PreparedStatement statement = connection.prepareStatement("DELETE from TradeRequest where user_id = ? and request_id = ?");
            statement.setString(1,userId);
            statement.setString(2, requestId);
            statement.executeQuery();
            return "Request cancelled";
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    @GetMapping("/tradeRequestsInProgress")
    List<TradeRequest> getTradeRequestsInProgress(@RequestBody String userId) {
        boolean response = false;
        try {
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            PreparedStatement statement = connection.prepareStatement("Select request_id,company_id,quantity from TradeRequest where user_id = ? and status = ? ");
            statement.setString(1,userId);
            statement.setString(2,"In progress");
            ResultSet rs = statement.executeQuery();
            List<TradeRequest> tradeRequests = new ArrayList<TradeRequest>();

            while (rs.next()) {
                String id = rs.getString("request_id");
                String companyId = rs.getString("company_id");
                int quantity = rs.getInt("quantity");

                TradeRequest request = new TradeRequest(id,companyId,quantity);
                tradeRequests.add(request);
            }
            return tradeRequests;
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    @PostMapping("/addBalance")
    boolean addBalance(@RequestBody String userId,@RequestBody int balance) {
        boolean response = false;
        try {
            if(balance < 0)
                return false;
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            PreparedStatement statement = connection.prepareStatement("SELECT acc_balance FROM Customer where customer_id =  ?");
            statement.setString(1,userId);
            ResultSet rs = statement.executeQuery();
            int accBalance = rs.getInt("acc_balance");
            PreparedStatement updateStatement = connection.prepareStatement("UPDATE Customer SET acc_balance = ?");
            statement.setInt(1,accBalance + balance);
            statement.executeQuery();
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    @PostMapping("/purchaseStocks")
    String purchaseStocks(@RequestBody String companyId, @RequestBody String price, @RequestBody int quantity, String customerId) {
        String response = "";
        try {
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            response = stockDao.performBuy(connection, customerId, companyId, price, quantity);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return response;
    }

    @PostMapping("/sellStocks")
    String sellStocks(@RequestBody String companyId, @RequestBody String price, @RequestBody int quantity, String userId) {
        String response = "";
        try {
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            response = stockDao.performSell(connection, companyId, price, quantity, userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return response;
    }

    @PostMapping("/transactionReport")
    List<String> getTransactionReport(@RequestBody String userId) {

        List<String> transactionReports = new ArrayList<>();
        try{
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            transactionReports.addAll(stockDao.getTransactionReports(connection, userId, "Success"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactionReports;
    }

}
