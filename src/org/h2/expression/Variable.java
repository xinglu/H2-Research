/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.expression;

import org.h2.command.Parser;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.value.Value;

/**
 * A user-defined variable, for example: @ID.
 */
public class Variable extends Expression {
	// 对于如下语句:
	// stmt.executeUpdate("SET @topVariableName=3");
	// sql = "select @topVariableName";
	// sql = "select @nullVariableName"; //不存在的变量名，此时值为null
	//子段name和lastValue分别是
	//topVariableName, 3
	//nullVariableName, null
    private final String name;
    private Value lastValue;
    
    //只看到在org.h2.command.Parser.readTerm()中使用
    public Variable(Session session, String name) {
        this.name = name;
        lastValue = session.getVariable(name);
    }

    public int getCost() {
        return 0;
    }

    public int getDisplaySize() {
        return lastValue.getDisplaySize();
    }

    public long getPrecision() {
        return lastValue.getPrecision();
    }

    public String getSQL() {
    	//调用这个方法而不是直接调用StringUtils.quoteIdentifier性能更好，因为大多数情况就是一个普通的标识符，没有什么特殊的，
        //这时就不必要再重新构造一个加引号的字符串
        return "@" + Parser.quoteIdentifier(name);
    }

    public int getScale() {
        return lastValue.getScale();
    }

    public int getType() {
        return lastValue.getType();
    }

    public Value getValue(Session session) {
        lastValue = session.getVariable(name);
        return lastValue;
    }

    public boolean isEverything(ExpressionVisitor visitor) {
        switch(visitor.getType()) {
        case ExpressionVisitor.EVALUATABLE:
            // the value will be evaluated at execute time
        case ExpressionVisitor.SET_MAX_DATA_MODIFICATION_ID:
            // it is checked independently if the value is the same as the last time
        case ExpressionVisitor.OPTIMIZABLE_MIN_MAX_COUNT_ALL:
        case ExpressionVisitor.READONLY:
        case ExpressionVisitor.INDEPENDENT:
        case ExpressionVisitor.NOT_FROM_RESOLVER:
        case ExpressionVisitor.QUERY_COMPARABLE:
        case ExpressionVisitor.GET_DEPENDENCIES:
        case ExpressionVisitor.GET_COLUMNS:
            return true;
        case ExpressionVisitor.DETERMINISTIC:
        	//因为变量本身就是可变的，所以并不确定，
        	//当要判断当前表达式(即变量)是否满足ExpressionVisitor.DETERMINISTIC(确定性)时显然返回false
            return false;
        default:
            throw DbException.throwInternalError("type="+visitor.getType());
        }
    }

    public void mapColumns(ColumnResolver resolver, int level) {
        // nothing to do
    }

    public Expression optimize(Session session) {
        return this;
    }

    public void setEvaluatable(TableFilter tableFilter, boolean value) {
        // nothing to do
    }

    public void updateAggregate(Session session) {
        // nothing to do
    }

    public String getName() {
        return name;
    }

}
