package request.congnos.query.report;

import request.congnos.query.model.xmlutils.MapInfo;

import java.util.List;
import java.util.Set;

/**
 * Created by Heng Song on 4/11/2016.
 */
public class ReportModel {
    private String name;
    private String refDataItem;
    private List<String> expression;
    private Set<MapInfo> infos;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRefDataItem() {
        return refDataItem;
    }

    public void setRefDataItem(String refDataItem) {
        this.refDataItem = refDataItem;
    }

    public List<String> getExpression() {
        return expression;
    }

    public void setExpression(List<String> expression) {
        this.expression = expression;
    }

    public Set<MapInfo> getInfos() {
        return infos;
    }

    public void setInfos(Set<MapInfo> infos) {
        this.infos = infos;
    }

    @Override
    public String toString() {
        return "ReportModel{" +
                "name='" + name + '\'' +
                ", refDataItem='" + refDataItem + '\'' +
                ", expression='" + expression + '\'' +
                ", infos=" + infos +
                '}';
    }
}
