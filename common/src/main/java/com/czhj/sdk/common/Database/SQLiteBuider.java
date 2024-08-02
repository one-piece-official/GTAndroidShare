package com.czhj.sdk.common.Database;


import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SQLiteBuider {

    private static final String TAG = "SQLiteBuider";

    private static Map<String, String> adsColumn = null;


    private static List<String> convertMapToList(Map<String, String> columes){

        List<String> stringList = new ArrayList<>(columes.size());

        for (String key : columes.keySet()){

            String keyType;
            String valueType =  columes.get(key).toLowerCase();
            switch ( valueType ){
                case "int":
                case "long":
                    keyType = "integer";
                    break;
                case "java.lang.string":
                case "text":
                    keyType = "text";
                    break;
                default:
                    keyType = "blob";
                    break;
            }
            stringList.add(String.format("%s %s ", key, keyType));
        }

        return stringList;
    }


    public static class CreateTriggerBuilder{

        private String triggerName = null;
        private String onAction = null;
        private String onTableName = null;
        private String execSql = null;

        public CreateTriggerBuilder setTriggerName(String triggerName) {
            this.triggerName = triggerName;
            return this;
        }


        public CreateTriggerBuilder setOnAction(String onAction) {
            this.onAction = onAction;
            return this;
        }

        public CreateTriggerBuilder setOnTableName(String onTableName) {
            this.onTableName = onTableName;
            return this;
        }

        public CreateTriggerBuilder setExecSql(String execSql) {
            this.execSql = execSql;
            return this;
        }

        public String build(){

            String sql = String.format("create trigger if not exists %s after %s on %s begin %s end;", triggerName, onAction,onTableName,execSql);

            return sql;
        }
    }


    public static class Insert{

        String tableName;
        String sql;
        Map<String, Object> values;
        List<Object> columns;


        public String getTableName() {
            return tableName;
        }

        public String getSql() {
            return sql;
        }

        public Map<String, Object> getValues() {
            return values;
        }

        public List<Object> getColumns() {
            return columns;
        }

        public static class Builder {
            private String tableName = null;
            private Map<String, Object> columnValues = null;


            public void setTableName(String tableName) {
                this.tableName = tableName;
            }


            public void setColumnValues(Map<String, Object> values){
                this.columnValues = values;
            }

            public Insert build() {
                List<Object> columns = Arrays.asList(columnValues.keySet().toArray());
                StringBuilder builder = new StringBuilder("insert or replace into ");
                builder.append(tableName);

                StringBuilder keys = new StringBuilder("(");
                StringBuilder values = new StringBuilder("(");
                for (int i = 0; i < columns.size(); i++) {
                    keys.append(columns.get(i));
                    values.append("?");
                    if(i+1 < columns.size()){
                        keys.append(", ");
                        values.append(", ");
                    }

                }
                keys.append(")");
                values.append(")");
                builder.append(keys).append(" values ").append(values);

                Insert insert= new Insert();

                insert.tableName = tableName;
                insert.columns = columns;
                insert.values = columnValues;
                insert.sql =  builder.toString();

                return insert;
            }
        }


    }


    public static class Update{

        String tableName;
        String sql;
        Map<String, Object> values;
        String where;

        public String getSql() {
            return sql;
        }

        public static class Builder {
            private String tableName = null;
            private Map<String, Object> columnValues = null;

            private String sqlWhere = null;
            public void setTableName(String tableName) {
                this.tableName = tableName;
            }


            public void setColumnValues(Map<String, Object> values){
                this.columnValues = values;
            }

            public void setWhere(String where){
                this.sqlWhere = where;
            }

            public Update build() {
                StringBuilder builder = new StringBuilder("update ");
                builder.append(tableName);

                builder.append(" set ");

                for (Iterator<String> it = columnValues.keySet().iterator(); it.hasNext();) {
                    String key  = it.next();
                    builder.append(key + " = " + columnValues.get(key));
                    if (it.hasNext()) {
                        builder.append(", ");
                    }
                }


                if (!TextUtils.isEmpty(sqlWhere)){
                    builder.append(" "+sqlWhere);
                }

                Update update= new Update();

                update.tableName = tableName;
                update.values = columnValues;
                update.where = sqlWhere;
                update.sql =  builder.toString();

                return update;
            }
        }


    }


    public static class CreateTable{

        String tableName;
        String sql;


        public static class Builder {

            private String tableName = null;
            private Map<String, String> primaryKey = null;
            private Map<String, String> columns = null;
            private boolean mAutoincrement;

            public Builder setTableName(String tableName) {
                this.tableName = tableName;
                return this;
            }

            public Builder setPrimaryKey(String primaryKey, String keyType ) {

                if(this.primaryKey == null){
                    Map<String, String> tempColumes = new HashMap<>();
                    this.primaryKey = tempColumes;

                }
                this.primaryKey.put(primaryKey, keyType);

                return this;
            }

            public Builder autoincrement(boolean isAutoincrement) {

                this.mAutoincrement = isAutoincrement;
                return this;
            }

            public Builder setColumns(Map<String, String> columns) {

                this.columns = columns;
                return this;
            }





            public CreateTable build(){

                StringBuilder builder = new StringBuilder("create table if not exists ");


                builder.append(tableName);

                builder.append(" ( ");


                List<String> primarys = convertMapToList(primaryKey);
                if (primarys.size()>1){
                    List<String> columesString = convertMapToList(columns);

                    for (String colume :columesString){
                        builder.append(colume).append(" ,");
                    }


                    builder.append(" primary key ( ");

                    Iterator<String> sets = primaryKey.keySet().iterator();

                    while (sets.hasNext()){

                        builder.append(sets.next());
                        if(sets.hasNext()){
                            builder.append(" ,");
                        }else {
                            builder.append(" )");
                        }
                    }



                }else{

                    if (mAutoincrement){
                        builder.append(String.format("id integer primary key AUTOINCREMENT"));
                    }else {
                        builder.append(String.format("%s primary key ", primarys.get(0)));

                    }
                    columns.remove(primaryKey.keySet().iterator().next());
                    List<String> columesString = convertMapToList(columns);

                    for (String colume :columesString){
                        builder.append(" ,");
                        builder.append(colume).append(" ");
                    }

                }

                builder.append(" ); ");

                CreateTable createTable = new CreateTable();
                createTable.sql = builder.toString();
                createTable.tableName = tableName;
                return createTable;
            }
        }


    }

}
