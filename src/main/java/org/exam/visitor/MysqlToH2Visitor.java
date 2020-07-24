package org.exam.visitor;

import com.alibaba.druid.sql.ast.SQLDataType;
import com.alibaba.druid.sql.ast.expr.SQLBinaryExpr;
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.ast.MySqlKey;
import com.alibaba.druid.sql.dialect.mysql.ast.MySqlPrimaryKey;
import com.alibaba.druid.sql.dialect.mysql.ast.MySqlUnique;
import com.alibaba.druid.sql.dialect.mysql.ast.MysqlForeignKey;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlCreateTableStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlLockTableStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlTableIndex;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUnlockTablesStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlOutputVisitor;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

//sed -e "/^--/d" \
/*不用处理*/
//-e 's/`//g' \
/*不用处理*/
//-e '/^DROP TABLE/d' \
/*不用处理*/
//-e 's/ COMMENT [^\n]*/,/g' \
/*不用处理*/
//-e 's/bigint/numeric/g' \
/*不用处理*/
//-e 's/double\(([^(]*)\)/double/' \
/*printDataType(SQLDataType x)*/
//-e 's/[^ ]*int([0-9]*)/integer/g' \
/*不用处理*/
//-e '/^\/\*.*\*\//d' \
/*不用处理*/
//-e '/LOCK TABLES/d' \
/*visit(MySqlLockTableStatement x) visit(MySqlUnlockTablesStatement x)*/
//-e '/^\/\*/,/\*\//c\;' \
/*不清楚要干嘛*/
//-e '/^CREATE TABLE/,/);/{/^  KEY/d; }' \
/*不清楚要干嘛*/
//-e 's/enum(.*)/varchar(255)/g' \
/*不用处理*/
//-e 's/CONSTRAINT "[^"]*"/ADD/g' \
/*printTableElements(List<SQLTableElement> tableElementList),另外不处理跑起来也没问题,因为和前面的索引名不一致,这里就采用删除*/
//-e 's/UNIQUE KEY "[^"]*"/UNIQUE/g' \
/*不用处理:UNIQUE KEY和UNIQUE INDEX都不需要处理*/
//-e 's/ DEFAULT [^\n]*/,/g' \
/*不用处理*/
//-e 's/CHARACTER SET [^ ]*//g' \
/* printDataType(SQLDataType x)*/
//-e 's/^CREATE TABLE/CREATE TABLE IF NOT EXISTS/g' \
/*不用处理*/
//-e 's/ text / varchar(65535) /g' \
/*不用处理*/
//-e "s/\\\'/''/g" \
/*不清楚要干嘛*/
//| sed -r 's/"(.*)"/"\U\1"/g' \
/*不清楚要干嘛*/
//| sed -r '/UNIQUE/  s/([^,]*)\([0-9]*\)/\1/g'
/*不清楚要干嘛*/
public class MysqlToH2Visitor extends MySqlOutputVisitor {
    private static final AtomicInteger atomicInteger = new AtomicInteger();

    public MysqlToH2Visitor(Appendable appender) {
        super(appender);
    }

    private static String unquote(String name) {
        if (name.startsWith("`") && name.endsWith("`")) {
            name = name.substring(1, name.length() - 1);
        }
        return name;
    }

    @Override
    public boolean visit(MySqlCreateTableStatement x) {
        //去掉表的AUTO_INCREMENT和CHARACTER SET
        x.getTableOptions().keySet().removeIf(key -> "AUTO_INCREMENT".equals(key) || "CHARSET".equals(key)
                || "CHARACTER SET".equals(key)
        );
        x.setComment(null);
        return super.visit(x);
    }

    @Override
    protected void printDataType(SQLDataType x) {
        if (x instanceof SQLCharacterDataType) {
            //去掉字符串的CHARACTER SET和COLLATE
            SQLCharacterDataType dataType = (SQLCharacterDataType) x;
            dataType.setCharSetName(null);
            dataType.setCollate(null);
        } else if ("double".equals(x.getName()) || "float".equals(x.getName())) {
            //如果是double或float类型,就去掉数据长度
            x.getArguments().clear();
        } else if ("enum".equals(x.getName())) {
            ((SQLColumnDefinition) x.getParent()).setCharsetExpr(null);
        }
        super.printDataType(x);
    }

    @Override
    public boolean visit(MySqlPrimaryKey x) {
        //去掉主键的using
        x.setIndexType(null);
        return super.visit(x);
    }

    @Override
    public boolean visit(MySqlUnique x) {
        //重新生成索引名称和去掉using
        x.setName(unquote(x.getName().getSimpleName()) + atomicInteger.incrementAndGet());
        x.setIndexType(null);
        return super.visit(x);
    }

    @Override
    public boolean visit(MySqlTableIndex x) {
        //重新生成索引名称和去掉using
        x.setName(new SQLIdentifierExpr(unquote(x.getName().getSimpleName()) + atomicInteger.incrementAndGet()));
        x.setIndexType(null);
        return super.visit(x);
    }

    @Override
    public boolean visit(MySqlKey x) {
        //重新生成索引名称和去掉using
        x.setName(unquote(x.getName().getSimpleName()) + atomicInteger.incrementAndGet());
        x.setIndexType(null);
        return super.visit(x);
    }

    @Override
    public boolean visit(MySqlLockTableStatement x) {
        return false;
    }

    @Override
    public boolean visit(MySqlUnlockTablesStatement x) {
        return false;
    }

    @Override
    protected void printTableElements(List<SQLTableElement> tableElementList) {
        //移除外键 和 索引
        tableElementList.removeIf(sqlTableElement -> sqlTableElement instanceof MysqlForeignKey
                || sqlTableElement instanceof MySqlKey);
        super.printTableElements(tableElementList);
    }

    @Override
    public boolean visit(SQLSetStatement x) {
        List<SQLAssignItem> items = x.getItems();
        items.removeIf(item -> {
                    String target = item.getTarget().toString();
                    return "SQL_MODE".equals(target) || "time_zone".equals(target);
                }
        );
        if (items.size() > 0) {
            return super.visit(x);
        } else {
            x.setAfterSemi(false);
            return false;
        }
    }

    @Override
    public boolean visit(SQLCharExpr x) {
        if ("0000-00-00 00:00:00".equals(x.getText())) {
            x.setText("0001-01-01 00:00:00");
        }
        return super.visit(x);
    }

    @Override
    public boolean visit(SQLBinaryExpr x) {
        print0(x.getText());
        return false;
    }

    @Override
    public boolean visit(SQLCreateDatabaseStatement x) {
        x.setAfterSemi(false);
        return false;
    }

    @Override
    public boolean visit(SQLUseStatement x) {
        x.setAfterSemi(false);
        return false;
    }

    @Override
    public boolean visit(SQLStartTransactionStatement x) {
        x.setAfterSemi(false);
        return false;
    }
}
