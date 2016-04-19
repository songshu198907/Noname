package request.congnos.query.model.xmlutils;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Heng Song on 4/5/2016.
 */
public class MapInfo implements Comparable, Serializable {
    private static final long serialVersionUID = 1L;
    private String businessLayer;
    private String reportTableName;
    private String reportColName;
    private String expression;
    private String calculation;
    private List<DataView> dataViews;

    public String getBusinessLayer() {
        return businessLayer;
    }

    public void setBusinessLayer(String businessLayer) {
        this.businessLayer = businessLayer;
    }

    public String getReportTableName() {
        return reportTableName;
    }

    public void setReportTableName(String reportTableName) {
        this.reportTableName = reportTableName;
    }

    public String getReportColName() {
        return reportColName;
    }

    public void setReportColName(String reportColName) {
        this.reportColName = reportColName;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getCalculation() {
        return calculation;
    }

    public void setCalculation(String calculation) {
        this.calculation = calculation;
    }

    public List<DataView> getDataViews() {
        return dataViews;
    }

    public void setDataViews(List<DataView> dataViews) {
        this.dataViews = dataViews;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MapInfo)) return false;

        MapInfo mapInfo = (MapInfo) o;

        if (!getBusinessLayer().equals(mapInfo.getBusinessLayer())) return false;
        return getReportColName().equals(mapInfo.getReportColName());

    }

    @Override
    public int hashCode() {
        int result = getBusinessLayer().hashCode();
        result = 31 * result + getReportColName().hashCode();
        return result;
    }

    @Override
    public int compareTo(Object o) {
        return this.hashCode() - o.hashCode();
    }

    @Override
    public String toString() {
        return "MapInfo{" +
                "businessLayer='" + businessLayer + '\'' +
                ", reportTableName='" + reportTableName + '\'' +
                ", reportColName='" + reportColName + '\'' +
                ", expression='" + expression + '\'' +
                ", calculation='" + calculation + '\'' +
                ", dataViews=" + dataViews +
                '}';
    }

    public static class DataView implements Serializable {

        private static final long serialVersionUID = 1L;
        protected String table;
        protected String schema;
        protected String colName;

//        public String getDbName() {
//            return dbName;
//        }
//
//        public void setDbName(String dbName) {
//            this.dbName = dbName;
//        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public String getColName() {
            return colName;
        }

        public void setColName(String colName) {
            this.colName = colName;
        }

        public String getSchema() {
            return schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
        }

        @Override
        public String toString() {
            return "DataView{" +
                    "table='" + table + '\'' +
                    ", schema='" + schema + '\'' +
                    ", colName='" + colName + '\'' +
                    '}';
        }
    }

}
