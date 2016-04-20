package request.congnos.query.model.tracer;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.XPath;
import request.congnos.query.model.tree.BPTree;
import request.congnos.query.model.xmlutils.MapInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Heng Song on 4/6/2016.
 * This class operates xml traversal.
 * For each column shown in the job ,
 * XMLTracer keep searching the referenced jobs
 * until find the physical columns in the db .
 */
public class XMLTracer implements Callable {
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(XMLTracer.class);
    /**
     * \
     * Document generated from xml .
     */
    private Document document;
    /**
     * Queue stored business layer columns
     */
    private ConcurrentLinkedQueue queue;
    /**
     * B+ tree used to store element and later used to search.
     */
    private BPTree tree;

    /**
     * Constructor.
     *
     * @param document Document created by dom4j.
     * @param queue    queue contains business layer columns.
     * @param tree     B plus tree.
     */
    public XMLTracer(Document document, ConcurrentLinkedQueue queue, BPTree tree) {

        this.document = document;
        this.queue = queue;
        this.tree = tree;


    }

    /**
     * call method.
     *
     * @return null.
     * This method will find the referenced items of business layer column.
     * If the referenced items belongs to physical db, the info of reference
     * items will be saved. Otherwise, it will keep searching .
     */
    @Override
    public Object call() {
        try {
            while (!queue.isEmpty()) {
                MapInfo info = (MapInfo) queue.poll();
                String layer = info.getBusinessLayer();
                String table = info.getReportTableName();
                String colName = info.getReportColName();
                /* The xpath to locate business layers in model xml */
                String colTmplate = "//namespace[./name[starts-with(.,\"%s\") and ends-with(.,\"%s\")]]//*[./name[starts-with(.,\"%s\") and ends-with(.,\"%s\")]]//*[./name[starts-with(.,\"%s\") and ends-with(.,\"%s\")]]";
                String colPath;
                /* Since dom4j does not support query String with ampersand, the string with ampersand is spilt into substring*/
                String[] layerArr = layer.split("&");
                String[] tabArr = table.split("&");
                String[] colArr = colName.split("&");

                colPath = String.format(colTmplate,
                        layerArr[0],
                        layerArr.length > 1 ? layerArr[1] : "",
                        tabArr[0], tabArr.length > 1 ?
                                tabArr[1] : "",
                        colArr[0],
                        colArr.length > 1 ? colArr[1] : "");


                long threadId = Thread.currentThread().getId();
                LOGGER.debug(threadId + ": colPath \n" + colPath);
                XPath calculation = document.createXPath(colPath);
                Node calcNode = calculation.selectSingleNode(document);
                XPath expression = document.createXPath("./expression");
                List<Node> exp = expression.selectNodes(calcNode);
                if (exp.size() == 1) {
                    XPath rXpath = document.createXPath(".//refobj");
                    List<Node> refjobs = rXpath.selectNodes(exp.get(0));
                    Node refjob = null;
                    if (refjobs.size() > 1) {
                        refjob = refjobs.get(1);
                    } else {
                        refjob = refjobs.get(0);
                    }
                    StringBuilder sb = new StringBuilder();
                    refjobs.stream().forEach(rfb -> sb.append(rfb.asXML()).append("\n"));
                    LOGGER.debug(threadId + "****ref job is " + refjob.getStringValue());
                    String value = refjob.getStringValue();
                    String[] tmp = value.split("\\.");
                    MapInfo.DataView view = new MapInfo.DataView();
                    List<MapInfo.DataView> views = new ArrayList<>();
                    info.setExpression(sb.toString().trim());
                    String db = tmp[0].substring(tmp[0].indexOf("[") + 1, tmp[0].indexOf("]"));
                    table = tmp[1].substring(tmp[1].indexOf("[") + 1, tmp[1].indexOf("]"));
                    String col = tmp[2].substring(tmp[2].indexOf("[") + 1, tmp[2].indexOf("]"));
                    views.add(findView(db, table, col));
                    info.setDataViews(views);
                    tree.insertOrUpdate(info.hashCode(), info);
                } else {
                    /**
                     * No expression is found which is regard as invalid.
                     */
                    LOGGER.fatal(exp.size() + " from " + colPath);
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Method used to find view.
     *
     * @param infos Array of String contains layer name ,table name and column name.
     * @return object used to save view info.
     * This method first will try to tell if the current table is
     * a real table in db or the virtual table referenced to other
     * item. If a real table , the db info will be returned. Otherwise,
     * it will keep searching until get the real table.
     */
    private MapInfo.DataView findView(String... infos) {
        MapInfo.DataView map = new MapInfo.DataView();
        if (infos.length != 3) {
            LOGGER.error("findView method accept 3" +
                    " parameters only not :" + infos.length);
            System.exit(1);
        }
        String db = infos[0];
        String table = infos[1];
        String col = infos[2];
        /* The xpath used to locate data source  */
        String endPath = "//namespace[./name/text()=\"%s\"]" +
                "/querySubject[./name/text()=\"%s\"]//dbQuery/sources/dataSourceRef";
        XPath checkEnd = document.createXPath(String.format(endPath, db, table));
        // The xpath to locate physical columns.
        String futherPath = "//*[./name/text()=\"%s\"]" +
                "//*[./name/text()=\"%s\"]//*[./name/text()=\"%s\"]//refobj";
        XPath furtherDown = document.createXPath(String.format(futherPath, db, table, col));
        List<Node> endNodes = checkEnd.selectNodes(document);
        if (endNodes.size() > 0) {
            String tmp = endNodes.get(0).getStringValue();
            tmp = tmp.substring(tmp.lastIndexOf("[") + 1, tmp.lastIndexOf("]"));
            String[] dbs = searchDB(tmp);
            map.setDbName(dbs[0]);
            map.setSchema(dbs[1]);
            map.setTable(table);
            map.setColName(col);
        } else {
            List<Node> children = furtherDown.selectNodes(document);
            Node refJob = children.get(children.size() - 1);
            String value = refJob.getStringValue();
            String[] tmp = value.split("\\.");
            db = tmp[0].substring(tmp[0].indexOf("[") + 1, tmp[0].indexOf("]"));
            table = tmp[1].substring(tmp[1].indexOf("[") + 1, tmp[1].indexOf("]"));
            col = tmp[2].substring(tmp[2].indexOf("[") + 1, tmp[2].indexOf("]"));
            return findView(db, table, col);

        }
        return map;
    }

    /**
     * SearchDB method searchers datasource info .
     *
     * @param name The name of datasource
     * @return schema and db name.
     */
    private String[] searchDB(String name) {
        String dsPath = "//dataSource[./name/text()=\"%s\"]";
        XPath dPath = document.createXPath(String.format(dsPath, name));
        Node dataSourceNode = dPath.selectSingleNode(document);
        XPath cmDS = document.createXPath("./cmDataSource");
        XPath schema = document.createXPath("./schema");
        Node cmNode = cmDS.selectSingleNode(dataSourceNode);
        Node schemaNode = schema.selectSingleNode(dataSourceNode);
        String[] dbInfo = new String[]{
                cmNode.getStringValue(), schemaNode.getStringValue()
        };
        return dbInfo;

    }
}