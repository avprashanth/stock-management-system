package com.stock_management;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
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
                        @RequestParam String firstName, @RequestParam String lastName, @RequestParam String role) {
        String response = "";
        try {
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            response = stockDao.registerUser(connection, userId, password, address, phoneNumber, firstName, lastName, role);
            logger.info("inserted user" + userId);
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            logger.info("insert new user in failed");
        }
        return response;
    }

    @PostMapping("/userLogin")
    String userLogin(@RequestParam String userId, @RequestParam String password) {
        String response = "";
        try {
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            response = stockDao.validateUser(connection, userId, password);
            logger.info("user logged in" + userId);
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }

        return response;
    }

    @PostMapping("/stockBrokerLogin")
    String stockBrokerLogin(@RequestParam String userId, @RequestParam String password) {
        String response = "";
        try {
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            response = stockDao.stockBrokerLogin(connection, userId, password);
            logger.info("stockBrokerLogin:"+userId);
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
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
                logger.info("upload by admin:"+message);
                return  message;
                //return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
            } catch (Exception e) {
                message = "Could not upload the file: " + file.getOriginalFilename() + "!";
                logger.info(message);
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
            logger.info("listcompanies successful");
        } catch (SQLException e) {
            logger.error(e.getMessage());
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
            logger.info(response);
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
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
            statement.executeUpdate();
            logger.info("Request cancelled");
            return "Request cancelled";
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    @GetMapping("/tradeRequestsInProgress")
    List<TradeRequest> getTradeRequestsInProgress(@RequestParam String userId) {
        boolean response = false;
        List<TradeRequest> tradeRequests = new ArrayList<TradeRequest>();
        try {
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            PreparedStatement statement = connection.prepareStatement("Select request_id,company_id,quantity,batch_id,price from traderequest where user_id = ? and status = ? ");
            statement.setString(1, userId);
            statement.setString(2, "failure");
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                String id = rs.getString("request_id");
                String companyId = rs.getString("company_id");
                int quantity = rs.getInt("quantity");
                String batch_id = rs.getString("batch_id");
                int price = rs.getInt("price");
                TradeRequest request = new TradeRequest(id, companyId, quantity, batch_id, price);
                tradeRequests.add(request);
            }
            logger.info("trade request list ");
            return tradeRequests;
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
        return tradeRequests;
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
            if(rs.next()) accBalance  = rs.getInt("acc_balance");
            else return false;
            PreparedStatement updateStatement = connection.prepareStatement("UPDATE customer SET acc_balance = ? where customer_id = ?");
            updateStatement.setInt(1,accBalance + amount);
            updateStatement.setString(2,userId);
            updateStatement.executeUpdate();
            logger.info("added"+amount);
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
        logger.info(response);
        return response;
    }

    @PostMapping("/transactionReport")
    List<String> getTransactionReport(@RequestParam String userId) {

        List<String> transactionReports = new ArrayList<>();
        try{
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            transactionReports.addAll(stockDao.getTransactionReports(connection, userId, "Success"));
            logger.info("success");
        } catch (SQLException e) {

            e.printStackTrace();
            logger.error(e.getMessage());
        }
        return transactionReports;
    }

    @PostMapping("/recommendations")
    ArrayList<String> getStock(@RequestParam String saferisk) throws SQLException {
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
                    logger.info("one");
                }
                else {
                    hm.get(stock.getCompany_id()).add(stock.getPrice());
                    logger.info("two");
                }
            }

            Map<Integer, String> treemap =
                    new TreeMap<Integer, String>(Collections.reverseOrder());
            ArrayList<String> res
                    = new ArrayList<String>();

            int noofrecommendations = 5;
            if(saferisk.equals("risk")) {
                for(Map.Entry<String, List<Integer>> companyData: hm.entrySet()) {
                    int pricedifference;
                    List<Integer> l = companyData.getValue();
                    pricedifference = l.get(l.size() - 1) - l.get(0);
                    treemap.put(pricedifference, companyData.getKey());
                }

                Set set = treemap.entrySet();
                Iterator i = set.iterator();
                // Traverse map and print elements
                while (i.hasNext() && noofrecommendations > 0) {
                    Map.Entry me = (Map.Entry)i.next();
                    System.out.print(me.getKey() + ": ");
                    System.out.println(me.getValue());
                    res.add((String) me.getValue());
                    noofrecommendations--;
                }
                logger.info("three");
                return res;
            }
            else {
                for(Map.Entry<String, List<Integer>> companyData: hm.entrySet()) {
                    int pricedeviation;
                    List<Integer> l = companyData.getValue();
                    double sum = 0;
                    for (int j = 0; j < l.size() - 1; j++) {
                        sum = sum + Math.pow(l.get(j + 1) - l.get(j), 2);
                    }
                    pricedeviation = (int) (sum / (l.size()));
                    treemap.put(pricedeviation, companyData.getKey());
                }
                Set set = treemap.entrySet();
                Iterator i = set.iterator();
                // Traverse map and print elements
                while (i.hasNext() && noofrecommendations > 0) {
                    Map.Entry me = (Map.Entry)i.next();
                    System.out.print(me.getKey() + ": ");
                    System.out.println(me.getValue());
                    res.add((String) me.getValue());
                    noofrecommendations--;
                }
                logger.info("four");
                return res;
            }
        } catch (SQLException e) {
            throw e;
        }
    }


    @GetMapping("/portfolio")
    List<Portfolio> getPortfolioDetails(@RequestParam String userId) {
        List<Portfolio> getPortfolioDetails = new ArrayList<>();
        try{
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            getPortfolioDetails.addAll(stockDao.getPortfolioDetails(connection, userId));
            logger.info("success");
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
        return getPortfolioDetails;
    }


    @GetMapping("/companiesByUser")
    List<UserBatch> GetCompaniesByUserId(@RequestParam String userId) {
        List<UserBatch> res = null;
        try {
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);

            res = getUserCompanyList(conn,userId);
            logger.info("success");
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
        logger.info("inserted user");
        return res;
    }

    static List<UserBatch> getUserCompanyList(Connection conn,String userId) {
        try {
            PreparedStatement statement = conn.prepareStatement("select distinct batch_id from traderequest where user_id = ?");
            statement.setString(1, userId);
            ResultSet rs = statement.executeQuery();
            List<String> batchIds = new ArrayList<>();
            List<UserBatch> userBatchList = new ArrayList<>();
            while(rs.next()) {
                batchIds.add(rs.getString("batch_id"));
            }
            for(int i = 0; i < batchIds.size(); i++) {
                int buyQuantity = 0;
                int sellQuantity = 0;
                int resQuantity = -1;
                String batchId = batchIds.get(i);
                PreparedStatement statement1 = conn.prepareStatement(
                        "select quantity from traderequest where batch_id = ? and action = 'buy' and status = 'success'"
                );
                statement1.setString(1, batchId);
                ResultSet res = statement1.executeQuery();
                while(res.next()) {
                    buyQuantity = buyQuantity + res.getInt("quantity");
                }
                PreparedStatement statement2 = conn.prepareStatement(
                        "select quantity from traderequest where batch_id = ? and action = 'sell'"
                );
                statement2.setString(1, batchId);
                ResultSet sellresult = statement2.executeQuery();
                while(sellresult.next()) {
                    sellQuantity = sellQuantity + sellresult.getInt("quantity");
                }
                resQuantity = buyQuantity - sellQuantity;
                if(resQuantity > 0) {
                    PreparedStatement statement3 = conn.prepareStatement(
                            "select * from traderequest where batch_id = ? LIMIT 1"
                    );
                    statement3.setString(1, batchId);
                    ResultSet result = statement3.executeQuery();
                    result.next();
                    int price = result.getInt("price");
                    String companyId = result.getString("company_id");
                    userBatchList.add(new UserBatch(batchId, price, resQuantity, companyId));
                }
            }
            logger.info("success");
            return userBatchList;
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }


    @GetMapping("/getBalance")
    UserBalance GetBalance(@RequestParam String userId) {
        UserBalance balance = null;
        try {
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
            balance = new UserBalance();
            int res = stockDao.getUserBalance(conn,userId);
            balance.setBalance(res);
            logger.info("success");
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
        return balance;
    }

}