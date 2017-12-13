package com.tuniu.vnd.guide.common.util;

import com.tuniu.vnd.guide.nm.domain.vo.ExcelExportVo;
import com.tuniu.vnd.guide.nm.util.Constants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Excel文件 读取工具类
 * Created by qiyu on 2017/11/8.
 */
public class ExcelUtil {

    private static Logger logger = LoggerFactory.getLogger(ExcelUtil.class);

    /**
     * 读取Excel的内容，第一维数组存储的是一行中格列的值，二维数组存储的是多少个行
     * 文件后缀如果是xls，使用HSSFWorkbook；如果是xlsx，使用XSSFWorkbook
     *
     * @param inputStream 读取数据的源Excel
     * @param ignoreRows  读取数据忽略的行数，比如行头不需要读入 忽略的行数为1
     * @return 读出的Excel中数据的内容
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static List<String[]> getData(InputStream inputStream, String suffixName, int ignoreRows) throws IOException {
        Workbook wb;
        if (Constants.XLS.equals(suffixName)) {
            wb = new HSSFWorkbook(inputStream);
        } else if (Constants.XLSX.equals(suffixName)) {
            wb = new XSSFWorkbook(inputStream);
        } else {
            return new ArrayList<String[]>();
        }
        FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();

        List<String[]> result = new ArrayList<String[]>();

        for (int sheetIndex = 0; sheetIndex < wb.getNumberOfSheets(); sheetIndex++) {
            Sheet st = wb.getSheetAt(sheetIndex);
            // 从忽略行数开始取数据
            for (int rowIndex = ignoreRows; rowIndex <= st.getLastRowNum(); rowIndex++) {
                Row row = st.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                int rowSize = row.getLastCellNum();
                String[] values = new String[rowSize];
                int hasValueFlag = 0;
                for (int columnIndex = 0; columnIndex < row.getLastCellNum(); columnIndex++) {
                    String value = "";
                    Cell cell = row.getCell(columnIndex);
                    CellValue cellValue = evaluator.evaluate(cell);
                    if (cellValue != null) {
                        switch (cellValue.getCellType()) {
                            case Cell.CELL_TYPE_BOOLEAN:
                                value = cellValue.getBooleanValue() ? "true" : "false";
                                break;
                            case Cell.CELL_TYPE_NUMERIC:
                                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                                    Date date = cell.getDateCellValue();
                                    if (date != null) {
                                        value = new SimpleDateFormat("yyyy-MM-dd").format(date);
                                    } else {
                                        value = "";
                                    }
                                } else {
                                    value = new DecimalFormat().format(cellValue.getNumberValue());
                                }
                                break;
                            case Cell.CELL_TYPE_STRING:
                                value = cellValue.getStringValue();
                                break;
                            case Cell.CELL_TYPE_BLANK:
                                break;
                            case Cell.CELL_TYPE_ERROR:
                                break;
                            // CELL_TYPE_FORMULA will never happen
                            case Cell.CELL_TYPE_FORMULA:
                                break;
                            default:
                                value = "";
                        }
                    }
                    values[columnIndex] = rightTrim(value);
                    if (StringUtils.isNotEmpty(values[columnIndex])) {
                        hasValueFlag++;
                    }
                }
                if (hasValueFlag > 0) {
                    result.add(values);
                }
            }
        }
        return result;
    }

    /**
     * 去掉字符串右边的空格
     *
     * @param str 要处理的字符串
     * @return 处理后的字符串
     */

    public static String rightTrim(String str) {
        if (str == null) {
            return "";
        }
        int length = str.length();
        for (int i = length - 1; i >= 0; i--) {
            if (str.charAt(i) != 0x20) {
                break;
            }
            length--;
        }
        return str.substring(0, length);
    }


    /**
     * @param os
     * @throws IOException
     */
    public static void export(OutputStream os, List<ExcelExportVo> exportList, String suffix) throws IOException {
        if (CollectionUtils.isNotEmpty(exportList)) {
            Workbook wb;
            if (Constants.XLS.equals(suffix)) {
                wb = new HSSFWorkbook();
            } else if (Constants.XLSX.equals(suffix)) {
                wb = new XSSFWorkbook();
            } else {
                return;
            }
            //处理每个sheet
            for (ExcelExportVo exportVo : exportList) {
                String sheetName = exportVo.getSheetName();//表名
                String[] excelHeader = exportVo.getHeader().split(",");//表头
                List<String> contentList = exportVo.getContentList();//表内容
                Sheet sheet = null;
                if(StringUtils.isNotEmpty(sheetName)){
                	sheet = wb.createSheet(sheetName);
                }else{
                	sheet = wb.createSheet();
                }
                Row row = sheet.createRow(0);
                //第一行插入表头内容
                for (int i = 0; i < excelHeader.length; i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(excelHeader[i]);
                    sheet.autoSizeColumn(i);
                }
                //从第二行开始插入数据
                for (int i = 0; i < contentList.size(); i++) {
                    row = sheet.createRow(i+1);
                    String[] rowInfo = contentList.get(i).split(",");
                    for (int j = 0; j < rowInfo.length; j++) {
                        row.createCell(j).setCellValue(rowInfo[j]);
                    }
                }
            }

            //os写数据流
            try {
                wb.write(os);//输出流，最终调用write(byte[] b, int off, int len)
                os.flush();
            } catch (IOException e) {
                logger.error("error in ExcelUtil.export", e);
            } finally {
                os.close();
            }
        }
    }

//    /**
//     * 二维数组回写excel流,这个是要用jxl的jar包，比较简单但是只能处理xls
//     */
//    public static void outputExcel(OutputStream os, List<String[]> strList) throws IOException,WriteException {
//        XSSFWorkbook wwww = new XSSFWorkbook();
//
//        org.apache.poi.ss.usermodel.Workbook wb = WorkbookFactory.create();
//
//
//        // 使用jxl包生成Excel并写入到输出流中
//        WritableWorkbook workbook = Workbook.createWorkbook(os);
//        WritableSheet sheet = workbook.createSheet("ErrorSheet", 0);
//        // 初始化第一行
//        Label headLabel = new Label(0, 0, "以下数据未成功录入请检查后重新使用模板录入");
//        sheet.addCell(headLabel);
//        //从第二行写入数据
//        int rowNum = 1;
//        for (String[] strs : strList) {
//            int columnNum = 0;
//            for (String str : strs) {
//                //创建要显示的内容,创建一个单元格，第一个参数为列坐标column，第二个参数为行坐标row，第三个参数为内容
//                Label tempLabel = new Label(columnNum, rowNum, str);
//                sheet.addCell(tempLabel);
//                columnNum++;
//            }
//            rowNum++;
//        }
//        workbook.write();
//        workbook.close();
//        os.close();
//    }

}


