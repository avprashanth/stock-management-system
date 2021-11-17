package com.stock_management;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.stock_management.*;
import java.sql.*;
import java.util.*;

@RestController
public class Controller {

    StockManagementDAO stockDao = new StockManagementDAO();
    static Logger logger = Logger.getLogger(Main.class.getName());
    static final String DB_URL = "jdbc:mysql://10.0.0.199/stock_management_db";
    static final String USER = "root";
    static final String PASS = "root";
    static int num = 4;
    static Scanner myObj = new Scanner(System.in);

    @PostMapping("/registerUser")
    String registerUser(@RequestParam String userId, @RequestParam String password,
                        @RequestParam String address, @RequestParam String phoneNumber,
                        @RequestParam String firstName, @RequestParam String lastName) {
        String response = "";
        try {
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            response = stockDao.registerUser(connection, userId, password, address, phoneNumber, firstName, lastName);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return response;
    }

    @PostMapping("/userLogin")
    String userLogin(@RequestParam String userId, @RequestParam String password) {
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
    String stockBrokerLogin(@RequestParam String userId, @RequestParam String password) {
        String response = "";
        try {
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            response = stockDao.stockBrokerLogin(connection, userId, password);

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
                stockDao.updateInProgressRequests(conn);
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

    @GetMapping("/ListCompanies")
    List<CompanyStock> GetCompanies() {
        List<CompanyStock> res = null;
        try {
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);

            res = stockDao.getCompanyList(conn);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    @PostMapping("/purchaseStocks")
    String purchaseStocks(@RequestParam String companyId, @RequestParam int price, @RequestParam int quantity, @RequestParam String customerId) {
        String response = "";
        try {
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            response = stockDao.performBuy(connection, customerId, companyId, price, quantity);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return response;
    }

    @PostMapping("/cancelRequest")
    String cancelRequest(@RequestParam String userId, @RequestParam String requestId) {
        boolean response = false;
        try {
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            PreparedStatement statement = connection.prepareStatement("DELETE from traderequest where user_id = ? and request_id = ?");
            statement.setString(1,userId);
            statement.setString(2, requestId);
            statement.executeQuery();
            return "Request cancelled";
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    @GetMapping("/tradeRequestsInProgress")
    List<TradeRequest> getTradeRequestsInProgress(@RequestParam String userId) {

        List<TradeRequest> tradeRequests = new ArrayList<TradeRequest>();
        try {
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            PreparedStatement statement = connection.prepareStatement("Select request_id,company_id,quantity from traderequest where user_id = ? and status = ? ");
            statement.setString(1,userId);
            statement.setString(2,"In progress");
            ResultSet rs = statement.executeQuery();

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
    boolean addBalance(@RequestParam String userId, @RequestParam int amount) {
        boolean response = false;
        try {
            if(amount < 0)
                return false;
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            PreparedStatement statement = connection.prepareStatement("SELECT acc_balance FROM customer where customer_id =  ?");
            statement.setString(1,userId);

            ResultSet rs = statement.executeQuery();
            int accBalance = 0;
            if(rs.next()) accBalance  = rs.getInt("accountBalance");
            else return false;
            PreparedStatement updateStatement = connection.prepareStatement("UPDATE customer SET acc_balance = ? where customer_id = ?");
            updateStatement.setInt(1,accBalance + amount);
            updateStatement.setString(2,userId);
            updateStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }


    @PostMapping("/sellStocks")
    String sellStocks(@RequestParam String companyId, @RequestParam int price, @RequestParam int quantity,@RequestParam String userId,@RequestParam String batchId) {
        String response = "";
        try {
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            response = stockDao.performSell(connection, companyId, price, quantity, userId,batchId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return response;
    }

    @PostMapping("/transactionReport")
    List<String> getTransactionReport(@RequestParam String userId) {

        List<String> transactionReports = new ArrayList<>();
        try{
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            transactionReports.addAll(stockDao.getTransactionReports(connection, userId, "Success"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactionReports;
    }

    @GetMapping("/recommendations")
    String getStock(@RequestParam String saferisk) {
        try {
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            List<CompanyStock> getCompanyList = stockDao.getCompanyList(connection);
            Map<String, List<Integer>> hm = new HashMap<>();
            List<Integer> values;
            for(CompanyStock stock : getCompanyList) {
                if(!hm.containsKey(stock.getCompany_id()))
                {
                    values = new ArrayList<>();
                    values.add(stock.getPrice());
                    hm.put(stock.getCompany_id(), values);
                }
                else {
                    hm.get(stock.getCompany_id()).add(stock.getPrice());
                }
            }

            if(saferisk.equals("risk")) {
                String company = "";
                int max = -1;
                for(Map.Entry<String, List<Integer>> set: hm.entrySet()) {
                    int res;
                    List<Integer> l = set.getValue();
                    res = l.get(l.size() - 1) - l.get(0);
                    if(max < res) {
                        company = set.getKey();
                        max = res;
                    }
                }
                return company;
            }
            else {
                String company = "";
                double min = Integer.MAX_VALUE;
                for(Map.Entry<String, List<Integer>> set: hm.entrySet()) {
                    double res;
                    List<Integer> l = set.getValue();
                    double sum = 0;
                    for(int i = 0; i < l.size() - 1; i++) {
                        sum = sum + Math.pow(l.get(i + 1) - l.get(i), 2);
                    }
                    res = sum/(l.size());
                    if(min > res) {
                        company = set.getKey();
                        min = res;
                    }
                }
                return company;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "error";
    }


    @PostMapping("/portfolio")
    List<Portfolio> getPortfolioDetails(@RequestParam String userId) {
        List<Portfolio> getPortfolioDetails = new ArrayList<>();
        try{
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            getPortfolioDetails.addAll(stockDao.getPortfolioDetails(connection, userId));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return getPortfolioDetails;
    }

}
