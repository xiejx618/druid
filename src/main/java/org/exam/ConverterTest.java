package org.exam;


import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.util.JdbcConstants;
import org.exam.util.FileUtils;
import org.exam.visitor.MysqlToH2Visitor;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConverterTest {

    final static private Logger LOGGER = LoggerFactory.getLogger(ConverterTest.class);
    private Connection connection;

    @BeforeClass
    public static void initDriver() {
        org.h2.Driver.load();
    }

    @AfterClass
    public static void cleanupDriver() {
        org.h2.Driver.unload();
    }

    @Before
    public void initConnection() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:test;MODE=MySQL");
    }

    @After
    public void closeConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            LOGGER.warn("Exception while closing connection", e);
        }
    }

    private void executeUpdate(String sql) throws SQLException {
        try {
            Statement sqlStat = connection.createStatement();
            sqlStat.executeUpdate(sql);
            sqlStat.close();
        } catch (SQLException e) {
            LOGGER.warn("Error sql=" + sql, e);
            throw e;
        }
    }

    private List<Map<String, Object>> executeSelect(String sql) throws SQLException {
        List<Map<String, Object>> result = null;

        Statement sqlStat = connection.createStatement();
        if (sqlStat.execute(sql)) {
            ResultSet rs = sqlStat.getResultSet();
            ResultSetMetaData metaData = rs.getMetaData();

            result = new ArrayList<Map<String, Object>>();
            while (rs.next()) {
                Map<String, Object> record = new HashMap<String, Object>();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    record.put(metaData.getColumnName(i), rs.getObject(i));
                }
                result.add(record);
            }
        }
        sqlStat.close();

        return result;
    }

    @Test
    public void test1() throws SQLException {
        fromText("CREATE DATABASE /*!32312 IF NOT EXISTS */`xwiki` /*!40100 DEFAULT CHARACTER SET utf8 */;");
    }
    @Test
    public void test2() throws SQLException {
        fromFile("E:\\ij\\demo\\src\\test\\resources\\create-table-with-constraint-on-delete.sql");
    }
    @Test
    public void test3() throws SQLException {
        fromFile("E:\\ij\\demo\\src\\test\\resources\\drupal.sql");
    }
    @Test
    public void test4() throws SQLException {
        fromFile("E:\\ij\\demo\\src\\test\\resources\\utf8mb4.sql");
    }
    @Test
    public void test5() throws SQLException {
        fromFile("E:\\ij\\demo\\src\\test\\resources\\wordpress.sql");
    }
    @Test
    public void test6() throws SQLException {
        fromFile("E:\\ij\\demo\\src\\test\\resources\\xwiki.sql");
    }
    @Test
    public void test7() throws SQLException {
        fromFile("E:\\ij\\demo\\src\\test\\resources\\xwiki-no-foreign-key-checks.sql");
    }
    @Test
    public void test8() throws SQLException {
        fromFile("E:\\ij\\demo\\src\\test\\resources\\xwiki-sqlyog.sql");
    }

    private void fromFile(String filepath) throws SQLException {
        String sql = FileUtils.copyToString(new File(filepath),  Charset.forName("UTF-8"));
        fromText(sql);
    }

    private void fromText(String sql) throws SQLException {
        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        StringBuilder sb = new StringBuilder();
        MysqlToH2Visitor visitor = new MysqlToH2Visitor(sb);
        for (SQLStatement statement : sqlStatements) {
            statement.accept(visitor);
            sb.append("\n");
        }
        sql = sb.toString();
        System.out.println(sql);
        executeUpdate(sql);
    }


}
