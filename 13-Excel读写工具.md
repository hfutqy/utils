```
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Excel文件 读取工具类
 * Created by qiyu on 2017/11/8.
 */
public class ExcelUtil {

    ////测试使用main，给个excel的url就能读
//    public static void main(String[] args) throws Exception {
////        File file = new File("D:\\这是excel的测试.xlsx");
//        URL url = new URL("http://public-api.nj.pla.tuniu.org/fb2/t2/G3/M00/77/04/Cii-a1mtBvKIX1YUAAAoCZWY800AAGS-wOiWbwAACgh41.xlsx");
//        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
//        //设置3s超时限定
//        conn.setConnectTimeout(3*1000);
//        //得到输入流
//        InputStream inputStream = conn.getInputStream();
//        String[][] result = getDataX(inputStream, 1);
//        int row = 1;
//        for (String[] e : result) {
//            for (String h : e) {
//                System.out.println(h);
//            }
//            System.out.println("以上是第" + row++ + "行");
//        }
//    }

    /**
     * 读取Excel的内容，第一维数组存储的是一行中格列的值，二维数组存储的是多少个行
     * 文件后缀如果是xls，使用HSSFWorkbook；如果是xlsx，使用XSSFWorkbook
     *
     * @param inputStream 读取数据的源Excel
     * @param ignoreRows  读取数据忽略的行数，比喻行头不需要读入 忽略的行数为1
     * @return 读出的Excel中数据的内容
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static String[][] getData(InputStream inputStream, int ignoreRows) throws IOException {
        //读取文件流
        BufferedInputStream in = new BufferedInputStream(inputStream);
        // 打开HSSFWorkbook
        POIFSFileSystem fs = new POIFSFileSystem(in);
        HSSFWorkbook wb = new HSSFWorkbook(fs);

        List<String[]> result = new ArrayList<String[]>();
        int rowSize = 0;
        HSSFCell cell = null;
        for (int sheetIndex = 0; sheetIndex < wb.getNumberOfSheets(); sheetIndex++) {
            HSSFSheet st = wb.getSheetAt(sheetIndex);
            // 第一行为标题，不取
            for (int rowIndex = ignoreRows; rowIndex <= st.getLastRowNum(); rowIndex++) {
                HSSFRow row = st.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                int tempRowSize = row.getLastCellNum() + 1;
                if (tempRowSize > rowSize) {
                    rowSize = tempRowSize;
                }
                String[] values = new String[rowSize];
                Arrays.fill(values, "");
                boolean hasValue = false;
                for (int columnIndex = 0; columnIndex <= row.getLastCellNum(); columnIndex++) {
                    String value = "";
                    cell = row.getCell(columnIndex);
                    if (cell != null) {
                        switch (cell.getCellType()) {
                            case HSSFCell.CELL_TYPE_STRING:
                                value = cell.getStringCellValue();
                                break;
                            case HSSFCell.CELL_TYPE_NUMERIC:
                                if (HSSFDateUtil.isCellDateFormatted(cell)) {
                                    Date date = cell.getDateCellValue();
                                    if (date != null) {
                                        value = new SimpleDateFormat("yyyy-MM-dd").format(date);
                                    } else {
                                        value = "";
                                    }
                                } else {
                                    value = new DecimalFormat("0").format(cell

                                            .getNumericCellValue());
                                }
                                break;
                            case HSSFCell.CELL_TYPE_FORMULA:
                                // 导入时如果为公式生成的数据则无值
                                if (!cell.getStringCellValue().equals("")) {
                                    value = cell.getStringCellValue();
                                } else {
                                    value = String.valueOf(cell.getNumericCellValue());
                                }
                                break;
                            case HSSFCell.CELL_TYPE_BLANK:
                                break;
                            case HSSFCell.CELL_TYPE_ERROR:
                                value = "";
                                break;
                            case HSSFCell.CELL_TYPE_BOOLEAN:
                                value = (cell.getBooleanCellValue() ? "Y" : "N");
                                break;
                            default:
                                value = "";
                        }
                    }
                    if (columnIndex == 0 && value.trim().equals("")) {
                        break;
                    }
                    values[columnIndex] = rightTrim(value);
                    hasValue = true;
                }
                if (hasValue) {
                    result.add(values);
                }
            }
        }
        in.close();
        String[][] returnArray = new String[result.size()][rowSize];
        for (int i = 0; i < returnArray.length; i++) {
            returnArray[i] = result.get(i);
        }
        return returnArray;
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
     * 这个是针对Excel2007 &+ 的处理，文件后缀带个x
     * 即xlsx后缀
     *
     * @param ignoreRows
     * @return
     * @throws Exception
     */
    public static String[][] getDataX(InputStream inputStream, int ignoreRows) throws Exception {
        BufferedInputStream inputBuffer = new BufferedInputStream(inputStream);
        XSSFWorkbook wb = new XSSFWorkbook(inputBuffer);

        List<String[]> result = new ArrayList<String[]>();
        int rowSize = 0;// 行长度为列数
        // 开始处理Excel数据
        int sheetNumbers = wb.getNumberOfSheets();// 获取表的总数
        for (int sheetIndex = 0; sheetIndex < sheetNumbers; sheetIndex++) {
            XSSFSheet st = wb.getSheetAt(sheetIndex);
            // 按照ignoreRows去掉不读的行数（如第一行标题）
            for (int rowIndex = ignoreRows; rowIndex <= st.getLastRowNum(); rowIndex++) {
                XSSFRow row = st.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                // 获取rowSize,行长取最长的一行size
                int tempRowSize = row.getLastCellNum();
                if (tempRowSize > rowSize) {
                    rowSize = tempRowSize;
                }
                // 获取一行的数据values[],初始化""
                String[] values = new String[rowSize];
                Arrays.fill(values, "");
                for (int columnIndex = 0; columnIndex < tempRowSize; columnIndex++) {
                    String value = "";
                    XSSFCell cell = row.getCell(columnIndex);
                    if (cell != null) {
                        switch (cell.getCellType()) {
                            case XSSFCell.CELL_TYPE_STRING:
                                value = cell.getStringCellValue();
                                break;
                            case XSSFCell.CELL_TYPE_NUMERIC:
                                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                                    Date date = cell.getDateCellValue();
                                    if (date != null) {
                                        value = new SimpleDateFormat("yyyy-MM-dd").format(date);
                                    } else {
                                        value = "";
                                    }
                                } else {
                                    value = new DecimalFormat("0").format(cell

                                            .getNumericCellValue());
                                }
                                break;
                            case XSSFCell.CELL_TYPE_FORMULA:
                                // 导入时如果为公式生成的数据则无值
                                if (!cell.getStringCellValue().equals("")) {
                                    value = cell.getStringCellValue();
                                } else {
                                    value = Double.toString(cell.getNumericCellValue());
                                }
                                break;
                            case XSSFCell.CELL_TYPE_BLANK:
                                break;
                            case XSSFCell.CELL_TYPE_ERROR:
                                value = "";
                                break;
                            case XSSFCell.CELL_TYPE_BOOLEAN:
                                value = (cell.getBooleanCellValue() ? "Y" : "N");
                                break;
                            default:
                                value = "";
                        }
                    }
                    values[columnIndex] = value;
                }
                result.add(values);
            }
        }
        inputBuffer.close();

        String[][] retResult = new String[result.size()][rowSize];
        for (int i = 0; i < retResult.length; i++) {
            retResult[i] = result.get(i);
        }
        return retResult;
    }

    /**
     * 将excel数据转化成流文件输出
     * @param os 输出流
     * @param list excel数据
     * @throws Exception
     */
    public static void export(OutputStream os, List<String[]> list) throws Exception {
        String[] excelHeader = {"导领ID", "导游荣誉", "荣誉时间"};
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("error_info");
        XSSFRow row = sheet.createRow(0);
        //第一行插入固定内容
        for (int i = 0; i < excelHeader.length; i++) {
            XSSFCell cell = row.createCell(i);
            cell.setCellValue(excelHeader[i]);
            sheet.autoSizeColumn(i);
        }
        //从第二行开始插入数据
        for (int i = 0; i < list.size(); i++) {
            row = sheet.createRow(i + 1);
            String[] rowInfo = list.get(i);
            for (int j = 0; j < rowInfo.length; j++) {
                row.createCell(j).setCellValue(rowInfo[j]);
            }
        }
        wb.write(os);//输出流，最终调用write(byte[] b, int off, int len)
        os.flush();
        os.close();
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



```
