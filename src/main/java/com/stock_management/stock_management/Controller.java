package com.stock_management.stock_management;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@RestController
public class Controller {
    static Logger logger = Logger.getLogger(Main.class.getName());
    static final String DB_URL = "jdbc:mysql://localhost:3306/stock_management_db";
    static final String USER = "root";
    static final String PASS = "root";
    static int num = 4;
    static Scanner myObj = new Scanner(System.in);

    static int addUser(Connection conn) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users VALUES (?, 'test1', 'address1', '10010010', 'rama', 'thonupu');");
            pstmt.setString(1, String.valueOf(num++));
//          pstmt.setString(2, username);
            int rs = pstmt.executeUpdate();
            logger.info("inserted user1");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1;
    }

    static String getFirstUser(Connection conn) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("Select password from users where user_id = ?;");
            pstmt.setString(1, "3");
            ResultSet rs = pstmt.executeQuery();
            System.out.println(rs.toString());
            return "User 1";
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "failed";
    }

    @GetMapping("/hello")
    String all() {
        String res = "";
        try {
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);

            res = getFirstUser(conn);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    @PostMapping("/register")
    int insert(@RequestBody String username) {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);

            int res = addUser(conn);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1;
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

}
