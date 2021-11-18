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
                        @RequestParam String firstName, @RequestParam String lastName, @RequestParam String role) {
        String response = "";
        try {
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
            response = stockDao.registerUser(connection, userId, password, address, phoneNumber, firstName, lastName, role);
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

    @PostMapping("/stockBrokerLogin")
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
            return tradeRequests;
        } catch (SQLException e) {
            e.printStackTrace();
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
                }
                else {
                    hm.get(stock.getCompany_id()).add(stock.getPrice());
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return getPortfolioDetails;
    }


    @GetMapping("/companiesByUser")
    List<UserBatch> GetCompaniesByUserId(String userId) {
        List<UserBatch> res = null;
        try {
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);

            res = getUserCompanyList(conn,userId);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    static List<UserBatch> getUserCompanyList(Connection conn,String userId) {
        try {
            PreparedStatement statement = conn.prepareStatement("select distinct t1.company_id, t1.batch_id, t1.quantity, t1.price from traderequest t1 where status = 'success' and action = 'buy' and user_Id = ?" +
                    " and t1.company_id IN ( select t2.company_id from  traderequest t2 where action = 'sell' " +
                    " and t2.batch_id = t1.batch_id " +
                    " and t2.quantity < t1.quantity and user_Id = ? and status = 'success')" +
                    " UNION" +
                    " select distinct t1.company_id, t1.batch_id, t1.quantity, t1.price from traderequest t1 where action = 'buy' " +
                    " and user_Id = ? and status = 'success' " +
                    " and t1.company_id NOT IN ( select t2.company_id from  traderequest t2 where action = 'sell' and user_Id = ? and status = 'success' )");
            statement.setString(1,userId);
            statement.setString(2,userId);
            statement.setString(3,userId);
            statement.setString(4,userId);
            ResultSet rs = statement.executeQuery();
            List<UserBatch> userStocks = new ArrayList<UserBatch>();

            while (rs.next()) {
                String companyId = rs.getString("company_id");
                String batchId = rs.getString("batch_id");
                int quantity = rs.getInt("quantity");
                int price = rs.getInt("price");
                UserBatch stock = new UserBatch(batchId,price,quantity,companyId);

                userStocks.add(stock);
            }
            return userStocks;
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return balance;
    }

}
