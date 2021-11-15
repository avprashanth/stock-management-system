package com.stock_management.stock_management;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;


public class ExcelHelper {
    public static String TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    static String[] HEADERs = { "Id", "price", "available_quantity", "share_type" };
    static String SHEET = "Stocks";

    public static boolean hasExcelFormat(MultipartFile file) {

        if (!TYPE.equals(file.getContentType())) {
            return false;
        }

        return true;
    }

    public static List<CompanyStock> excelToCompanyStocks(InputStream is, Connection connection)   {
        try {
            Workbook workbook = new XSSFWorkbook(is);

            Sheet sheet = workbook.getSheet(SHEET);
            Iterator<Row> rows = sheet.iterator();

            long millis = System.currentTimeMillis();
            java.sql.Date date = new java.sql.Date(millis);
            String sql = "INSERT INTO companystock (company_id, created_time, price, available_quantity, share_type, updated_time) VALUES (?, '2021-11-14', ?, ?, ?, '2021-11-14')";
            PreparedStatement statement = connection.prepareStatement(sql);

            List<CompanyStock> companyStocks = new ArrayList<CompanyStock>();

            int rowNumber = 0;
            while (rows.hasNext()) {
                Row currentRow = rows.next();

                // skip header
                if (rowNumber == 0) {
                    rowNumber++;
                    continue;
                }

                Iterator<Cell> cellsInRow = currentRow.iterator();

                CompanyStock stock = new CompanyStock();

                int cellIdx = 0;
                while (cellsInRow.hasNext()) {
                    Cell currentCell = cellsInRow.next();

                    switch (cellIdx) {
                        case 0:
                            String company_id = currentCell.getStringCellValue();
                            stock.setCompany_id(company_id);
                            statement.setString(1, company_id);
                            break;

                        case 1:
                            int price = (int)currentCell.getNumericCellValue();
                            stock.setPrice(price);
                            statement.setInt(2, price);
                            break;

                        case 2:
                            int quantity = (int)currentCell.getNumericCellValue();
                            stock.setAvailable_quantity(quantity);
                            statement.setInt(3, quantity);
                            break;

                        case 3:
                            String share_type = currentCell.getStringCellValue();
                            stock.setShare_type(share_type);
                            statement.setString(4, share_type);
                            break;

                        default:
                            break;
                    }

                    cellIdx++;
                }
                statement.addBatch();
                companyStocks.add(stock);

                statement.executeBatch();
            }

            workbook.close();
            return companyStocks;
        } catch (IOException e) {
            throw new RuntimeException("fail to parse Excel file: " + e.getMessage());
        }
        catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }
}