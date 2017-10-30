```
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * 操作Hbase的常用方法
 * <p/>
 * Created by huqingmiao on 2015/4/14.
 */
public class HbaseUtil {

    private static final Logger log = LoggerFactory.getLogger(HbaseUtil.class);

    private static Configuration conf = null;

    private static HConnection conn = null;

    private static String HADOOP_HOME = "C:/hadoop";

    static {

//        try {
//            String hadoopHome = System.getProperties().getProperty("hadoop.home.dir"); //Windows下的HOME目录， 在unix下部署不需要设置
//            if (hadoopHome == null || "".equals(hadoopHome.trim())) {
//                hadoopHome = HADOOP_HOME;
//            }
//
//            File hadoopBinDir = new File(hadoopHome, "bin");      //HOME目录下的bin目录
//            if (!hadoopBinDir.exists()) {
//                hadoopBinDir.mkdirs();
//            }
//            File winExeFile = new File(hadoopBinDir.getCanonicalPath() + File.separator + "winutils.exe");
//            if (!winExeFile.exists()) {
//                winExeFile.createNewFile();
//            }
//
//            //设置环境变量
//            System.getProperties().put("hadoop.home.dir", hadoopHome);
//
//        } catch (IOException e) {
//            log.error("create ./bin/winutils.exe error.", e);
//        }

        //默认从hbase-site.xml读取配置信息
        conf = HBaseConfiguration.create();
//        conf.set("hbase.zookeeper.property.clientPort", "2181");
//        conf.set("hbase.zookeeper.quorum", "10.75.201.125");
//        conf.set("hbase.master", "10.75.201.125:60010");
        //conf.set("hbase.zookeeper.quorum", "hmaster");
        //与hbase/conf/hbase-site.xml中hbase.zookeeper.property.clientPort配置的值相同
      //  conf.set("hbase.zookeeper.property.clientPort", "2181");
    }


    public HbaseUtil() {
        try {
            //预先创建了一个连接，以后的访问都共享该连接
        	//conf.addResource("hbase-site.xml");
            conn = HConnectionManager.createConnection(conf);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void finalize() throws Throwable {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        super.finalize();
    }


    /**
     * 建表
     *
     * @param tableName     表名
     * @param columnFamilys 列簇名
     * @throws Exception
     */
    public void createTable(String tableName, String[] columnFamilys) throws Exception {
        HBaseAdmin hAdmin = null;
        try {
            hAdmin = new HBaseAdmin(conf);

            if (hAdmin.tableExists(tableName)) {
                log.info("已经存在要创建的表:" + tableName);

            } else {
                HTableDescriptor tableDesc = new HTableDescriptor(TableName.valueOf(tableName));

                //描述列族
                for (String columnFamily : columnFamilys) {
                    tableDesc.addFamily(new HColumnDescriptor(columnFamily));
                }

                //建表
                hAdmin.createTable(tableDesc);
                log.info("成功创建表:" + tableName);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw e;
        } finally {
            try {
                if (hAdmin != null) {
                    hAdmin.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }


    /**
     * 删除表
     *
     * @param tableName 表名
     * @throws Exception
     */
    public void deleteTable(String tableName) throws Exception {
        HBaseAdmin hAdmin = null;
        try {
            hAdmin = new HBaseAdmin(conf);

            if (hAdmin.tableExists(tableName)) {
                hAdmin.disableTable(tableName);//禁用表
                hAdmin.deleteTable(tableName);// 删除表
                log.info("成功删除表:" + tableName);

            } else {
                log.info("要删除的表不存在:" + tableName);
            }

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw e;
        } finally {
            try {
                if (hAdmin != null) {
                    hAdmin.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }


    /**
     * 向指定的行、列簇、列写入一项数据；如果该行不存在，则会插入一行。
     *
     * @param tableName 表名
     * @param rowkey    行键
     * @param colFamily 列簇名
     * @param column    列名
     * @param value     列值
     * @throws Exception
     */
    public void putData(String tableName, String rowkey,
                        String colFamily, String column, String value) throws Exception {

        HTableInterface table = null;
        try {
            //table = new HTable(conf, tableName);
            table = conn.getTable(tableName);
            Put put = new Put(Bytes.toBytes(rowkey));

            // 参数分别为：列族、列、值
            put.add(Bytes.toBytes(colFamily), Bytes.toBytes(column), Bytes.toBytes(value));

            table.put(put);
            log.info("成功写入1项数据到{}.", tableName);

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw e;
        } finally {
            try {
                if (table != null) {
                    table.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }


    /**
     * 向指定的行、列簇、列写入一项数据；如果该行不存在，则会插入一行。
     *
     * @param tableName 表名
     * @param hbCell    存放行键、列簇、列名、列值的数据单元
     * @throws Exception
     */
    public void putData(String tableName, HbaseCell hbCell) throws Exception {
        HTableInterface table = null;
        try {
            //table = new HTable(conf, tableName);
            table = conn.getTable(tableName);
            Put put = new Put(convertToBytes(hbCell.getRowkey()));

            // 参数分别为：列族、列、值
            put.add(Bytes.toBytes(hbCell.getColFamily()), Bytes.toBytes(hbCell.getColName()), convertToBytes(hbCell.getColValue()));

            table.put(put);
            log.info("成功写入1项数据到{}.", tableName);

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw e;
        } finally {
            try {
                if (table != null) {
                    table.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }


    /**
     * 写入多行、多列数据
     *
     * @param tableName  表名
     * @param hbCellList 存放行键、列簇、列名、列值的数据单元.
     * @throws Exception
     */
    public void putData(String tableName, List<HbaseCell> hbCellList) throws Exception {
        if (hbCellList.isEmpty()) {
            return;
        }
        HTableInterface table = null;
        try {
            //table = new HTable(conf, tableName);
            table = conn.getTable(tableName);

            List<Put> putList = new ArrayList<Put>();
            for (HbaseCell hbCell : hbCellList) {
                Put put = new Put(convertToBytes(hbCell.getRowkey()));
                put.add(Bytes.toBytes(hbCell.getColFamily()), Bytes.toBytes(hbCell.getColName()), convertToBytes(hbCell.getColValue()));

                putList.add(put);
            }

            table.put(putList);
            log.info("成功写入{}项数据到{}.", hbCellList.size(), tableName);

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw e;
        } finally {
            try {
                if (table != null) {
                    table.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 写入多行、多列数据
     *
     * @param tableName  表名
     * @param hbCellList 存放行键、列簇、列名、列值的数据单元.
     * @throws Exception
     */
    public void putDataForNotNull(String tableName, List<HbaseCell> hbCellList) throws Exception {
        if (hbCellList.isEmpty()) {
            return;
        }
        HTableInterface table = null;
        try {
            //table = new HTable(conf, tableName);
            table = conn.getTable(tableName);

            List<Put> putList = new ArrayList<Put>();
            for (HbaseCell hbCell : hbCellList) {
                if (!StringUtils.isEmpty(hbCell.getColValue() + "")) {
                    Put put = new Put(convertToBytes(hbCell.getRowkey()));
                    put.add(Bytes.toBytes(hbCell.getColFamily()), Bytes.toBytes(hbCell.getColName()), convertToBytes(hbCell.getColValue()));
                    putList.add(put);
                }
            }
            table.put(putList);
            log.info("成功写入{}项数据到{}.", hbCellList.size(), tableName);

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw e;
        } finally {
            try {
                if (table != null) {
                    table.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 删除一行
     *
     * @param tableName 表名
     * @param rowkey    行键
     * @throws Exception
     */

    public void delRow(String tableName, String rowkey) throws Exception {
        HTableInterface table = null;
        try {
            //table = new HTable(conf, tableName);
            table = conn.getTable(tableName);
            Delete del = new Delete(Bytes.toBytes(rowkey));

            table.delete(del);
            log.info("成功删除1行数据!");

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw e;
        } finally {
            try {
                if (table != null) {
                    table.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }


    /**
     * 删除多行
     *
     * @param tableName 表名
     * @param rowkeys   行键
     * @throws Exception
     */
    public void delMulitRows(String tableName, List<String> rowkeys) throws Exception {
        HTableInterface table = null;
        try {
            //table = new HTable(conf, tableName);
            table = conn.getTable(tableName);

            List<Delete> delList = new ArrayList<Delete>();
            for (String rowkey : rowkeys) {
                Delete del = new Delete(Bytes.toBytes(rowkey));
                delList.add(del);
            }
            table.delete(delList);
            delList.clear();
            log.info("成功删除{}行数据.", delList.size());

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw e;
        } finally {
            try {
                if (table != null) {
                    table.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }


    /**
     * 获取指定行的所有数据项
     *
     * @param tableName 表名
     * @param rowkey    行键
     * @return
     * @throws Exception
     */
    public Result getRow(String tableName, String rowkey) throws Exception {
        HTableInterface table = null;
        try {
            //table = new HTable(conf, tableName);
            table = conn.getTable(tableName);

            Get get = new Get(Bytes.toBytes(rowkey));
            Result rs = table.get(get);

//            for (Cell cell : result.rawCells()) {
//                System.out.print("Row Name: " + new String(CellUtil.cloneRow(cell)) + " ");
//                System.out.print("Timestamp: " + cell.getTimestamp() + " ");
//                System.out.print("column Family: " + new String(CellUtil.cloneFamily(cell)) + " ");
//                System.out.print("column Name:  " + new String(CellUtil.cloneQualifier(cell)) + " ");
//                System.out.println("Value: " + new String(CellUtil.cloneValue(cell)) + " ");
//            }

            return rs;

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw e;
        } finally {
            try {
                if (table != null) {
                    table.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }


    /**
     * 获取指定表的所有行的数据项
     *
     * @param tableName 表名
     * @return
     * @throws Exception
     */
    public List<Result> findAllRows(String tableName) throws Exception {
        HTableInterface table = null;
        try {
            //table = new HTable(conf, tableName);
            table = conn.getTable(tableName);

            Scan scan = new Scan();
            ResultScanner results = table.getScanner(scan);

            List<Result> rsList = new ArrayList<Result>();
            for (Result rs : results) {
                rsList.add(rs);
            }
            return rsList;

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw e;
        } finally {
            try {
                if (table != null) {
                    table.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }


    /**
     * 通用查询
     *
     * @param tableName 表名
     * @param filter    查询过滤器。单一条件查询可传Filter对象，组合条件查询可传FilterList, FilterList是Filter的子类。
     */
    public List<Result> findRow(String tableName, Filter filter) throws Exception {
        HTableInterface table = null;
        try {
            //table = new HTable(conf, tableName);
            table = conn.getTable(tableName);

            Scan scan = new Scan();
            scan.setFilter(filter);
            ResultScanner results = table.getScanner(scan);

            List<Result> rsList = new ArrayList<Result>();
            for (Result rs : results) {
                rsList.add(rs);
            }
            return rsList;

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw e;
        } finally {
            try {
                if (table != null) {
                    table.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }


    /**
     * 打印、展示查询结果
     *
     * @param result
     */
    public void showData(Result result) {

        for (Cell cell : result.rawCells()) {
            log.info("Row: " + new String(CellUtil.cloneRow(cell)) + " ");
            log.info("Timestamp: " + cell.getTimestamp() + " ");
            log.info("Column Family: " + new String(CellUtil.cloneFamily(cell)) + " ");
            log.info("Column Name:  " + new String(CellUtil.cloneQualifier(cell)) + " ");
            log.info("Column Value: " + new String(CellUtil.cloneValue(cell)) + " ");
        }

    }

    /**
     * 打印、展示查询的各项列值
     *
     * @param rsList
     */
    public void showData(List<Result> rsList) {
        log.info(">>>总的数据条数：" + rsList.size());

        if (rsList.isEmpty()) {
            return;
        }
        for (Result rs : rsList) {
            Cell[] cells = rs.rawCells();
            for (Cell cell : rs.rawCells()) {
                log.info("Row: " + new String(CellUtil.cloneRow(cell)) + " ");
                log.info("Timestamp: " + cell.getTimestamp() + " ");
                log.info("Column Family: " + new String(CellUtil.cloneFamily(cell)) + " ");
                log.info("Column Name:  " + new String(CellUtil.cloneQualifier(cell)) + " ");
                log.info("Column Value: " + new String(CellUtil.cloneValue(cell)) + " ");
            }
        }
    }


    /**
     * 打印、展示查询的各项列值
     *
     * @param rsList
     */
    public void showRowkey(List<Result> rsList) {
        log.info(">>>总的数据条数：" + rsList.size());

        if (rsList.isEmpty()) {
            return;
        }
        for (Result rs : rsList) {
            log.info(new String(rs.getRow()));
        }
    }

    private byte[] convertToBytes(Object obj) throws Exception {
        if (obj == null) {
            return new byte[0];
        }
        if (obj instanceof String) {
            return Bytes.toBytes((String) obj);
        }
        if (obj instanceof Double) {
            return Bytes.toBytes((Double) obj);
        }
        if (obj instanceof Float) {
            return Bytes.toBytes((Float) obj);
        }
        if (obj instanceof Long) {
            return Bytes.toBytes((Long) obj);
        }
        if (obj instanceof Integer) {
            return Bytes.toBytes((Integer) obj);
        }
        if (obj instanceof Date) {
            return Bytes.toBytes(((Date) obj).getTime());
        }
        if (obj instanceof Timestamp) {
            return Bytes.toBytes(((Timestamp) obj).getTime());
        }
        if (obj instanceof BigDecimal) {
            return Bytes.toBytes((BigDecimal) obj);
        }
        throw new Exception("未能识别的数据类型: " + obj.getClass().getName());
    }


    // main
    public static void main(String[] args) {
        try {
        	HbaseUtil client = new HbaseUtil();

            String tableName = "testtable";

            // 创建数据库表：“studyinfo”
            String[] colFamilys = {"studyinfo", "course"};
            client.createTable(tableName, colFamilys);

            // 添加第一行数据
            client.putData(tableName, "ligan", "studyinfo", "age", "2333");
            client.putData(tableName, "ligan", "studyinfo", "sex", "boy");
            client.putData(tableName, "ligan", "course", "china", "97");
            client.putData(tableName, "ligan", "course", "math", "128");
            client.putData(tableName, "ligan", "course", "english", "85");

            // 添加第二行数据
            client.putData(tableName, "xiaoxue", "studyinfo", "age", "20");
            client.putData(tableName, "xiaoxue", "studyinfo", "sex", "boy");
            client.putData(tableName, "xiaoxue", "course", "china", "90");
            client.putData(tableName, "xiaoxue", "course", "math", "100");
            client.putData(tableName, "xiaoxue", "course", "english", "90");

            // 添加第三行数据，也可以这样写:
            HbaseCell hbCell1 = new HbaseCell("walker", "studyinfo", "age", "18");
            HbaseCell hbCell2 = new HbaseCell("walker", "studyinfo", "sex", "girl");
            HbaseCell hbCell3 = new HbaseCell("walker", "course", "math", "100");
            HbaseCell hbCell4 = new HbaseCell("walker", "course", "english", "30");
            List<HbaseCell> cellList = new ArrayList<HbaseCell>();
            cellList.add(hbCell1);
            cellList.add(hbCell2);
            cellList.add(hbCell3);
            cellList.add(hbCell4);
            client.putData(tableName, cellList);


            // 获取一条数据
            log.info("获取一条数据");
            Result rs = client.getRow(tableName, "ligan");
            client.showData(rs);


            //组合查询
            log.info("组合查询");
            List<Filter> filters = new ArrayList<Filter>();
            Filter filter1 = new SingleColumnValueFilter(Bytes
                    .toBytes("studyinfo"), Bytes.toBytes("age"), CompareFilter.CompareOp.GREATER, Bytes
                    .toBytes("18"));
            filters.add(filter1);

            Filter filter2 = new SingleColumnValueFilter(Bytes
                    .toBytes("course"), Bytes.toBytes("math"), CompareFilter.CompareOp.EQUAL, Bytes
                    .toBytes("100"));
            filters.add(filter2);

            FilterList filterList = new FilterList(filters);

            List<Result> rsList = client.findRow(tableName, filterList);
            log.info(">>>" + rsList.size());


            // 获取所有数据
            log.info("获取所有数据");
            rsList = client.findAllRows(tableName);
            log.info(">>>" + rsList.size());

            //删除一条数据
            log.info("删除一条数据");
            client.delRow(tableName, "tht");
            log.info(">>>" + rsList.size());

            //删除多条数据
            log.info("删除多条数据");
            List<String> rows = new ArrayList<String>();
            rows.add("xiaoxue");
            rows.add("walker");
            client.delMulitRows(tableName, rows);
            client.findAllRows(tableName);
            log.info(">>>" + rsList.size());


            //删除数据库
            log.info("删除表");
            client.deleteTable(tableName);


        } catch (Exception err) {
            err.printStackTrace();
        }
    }

}
```
