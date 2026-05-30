package com.notesapp.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.FileInputStream;

// reads data from excel test data files
public class ExcelUtils {

    // returns the value of a specific cell
    // rowNum and colNum are 0-based (row 0 = header, row 1 = first data row)
    public static String getCellData(String filePath, int rowNum, int colNum) {
        try {
            FileInputStream fis = new FileInputStream(filePath);
            Workbook wb = WorkbookFactory.create(fis);
            Sheet sheet = wb.getSheetAt(0);
            Row row = sheet.getRow(rowNum);
            Cell cell = row.getCell(colNum);
            String value = cell.toString().trim();
            wb.close();
            fis.close();
            return value;
        } catch (Exception e) {
            System.out.println("ExcelUtils error: " + e.getMessage());
            return "";
        }
    }

    // reads all data rows and returns them as a 2D array for TestNG DataProvider
    // column order: 0=email, 1=password, 2=expectedResult, 3=testCaseId
    public static Object[][] getLoginTestData(String filePath) {
        try {
            FileInputStream fis = new FileInputStream(filePath);
            Workbook wb = WorkbookFactory.create(fis);
            Sheet sheet = wb.getSheetAt(0);

            int totalRows = sheet.getLastRowNum(); // row 0 is header, so data is rows 1 to lastRowNum
            Object[][] data = new Object[totalRows][4];

            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                data[i - 1][0] = row.getCell(0).toString().trim(); // email
                data[i - 1][1] = row.getCell(1).toString().trim(); // password
                data[i - 1][2] = row.getCell(2).toString().trim(); // expectedResult
                data[i - 1][3] = row.getCell(3).toString().trim(); // testCaseId
            }

            wb.close();
            fis.close();
            return data;

        } catch (Exception e) {
            System.out.println("Error reading login data: " + e.getMessage());
            return new Object[0][0];
        }
    }

    // reads notes test data - columns: 0=title, 1=description, 2=category
    public static Object[][] getNotesTestData(String filePath) {
        try {
            FileInputStream fis = new FileInputStream(filePath);
            Workbook wb = WorkbookFactory.create(fis);
            Sheet sheet = wb.getSheetAt(0);

            int totalRows = sheet.getLastRowNum();
            Object[][] data = new Object[totalRows][3];

            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                data[i - 1][0] = row.getCell(0).toString().trim(); // title
                data[i - 1][1] = row.getCell(1).toString().trim(); // description
                data[i - 1][2] = row.getCell(2).toString().trim(); // category
            }

            wb.close();
            fis.close();
            return data;

        } catch (Exception e) {
            System.out.println("Error reading notes data: " + e.getMessage());
            return new Object[0][0];
        }
    }
}
