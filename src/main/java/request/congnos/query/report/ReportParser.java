package request.congnos.query.report;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.XPath;
import org.dom4j.io.SAXReader;
import request.congnos.query.model.xmlutils.MapInfo;
import request.congnos.query.model.xmlutils.NameSpaceCleaner;
import request.congnos.query.model.xmlutils.XMLParser;
import request.congnos.query.report.excelUtil.ExcelWriter;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Heng Song on 4/8/2016.
 */
public class ReportParser implements ReportOp {
    private final static Logger LOGGER = Logger.getLogger(ReportParser.class);
    private Document document;
    private Map<String, String[]> map;
    private Map<String, String> itemsMap;
    private Set<String> layers;
    private XMLParser parser;
    private String modelFileName;
    private List<ReportModel> further, stop;
    private HashMap<String, List<String>> queryMap;

    public ReportParser(String modelFileName) {
        layers = new HashSet<>();
        map = new HashMap<>();
        this.modelFileName = modelFileName;
        further = new ArrayList<>();
        stop = new ArrayList<>();
        itemsMap = new HashMap<>();
        queryMap = new HashMap<>();


    }

    @Override
    public void load(String fileName) {
        SAXReader reader = new SAXReader();
        try {
            document = reader.read(getClass().getClassLoader().getResourceAsStream(fileName));
            document.accept(new NameSpaceCleaner());
        } catch (DocumentException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void parse() {
        loadItemMap();
        loadQueryMap();
        searchDataItem();
        LOGGER.info("Report parsing done!");
        try {
            XMLParser.run(modelFileName, layers);
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (ReportModel reportModel : further) {

            Set<MapInfo> infos = reportModel.getInfos();

            Set<MapInfo> res = new HashSet<>();
            for (MapInfo info : infos) {
                MapInfo node = XMLParser.findElement(info);
                if (node == null) {
                    LOGGER.fatal(info);
                }
                res.add(node);
            }
            reportModel.setInfos(res);
//            LOGGER.info(reportModel);
        }


    }

    private void loadItemMap() {
        LOGGER.info("Start loading items in report now !");
        XPath path = document.createXPath("//queries/query[.//selection and not(.//queryOperation)]");
        List<Node> nodes = path.selectNodes(document);
        if (nodes.size() > 0) {
            nodes.stream().forEach(node -> {
                String schemaName = document.
                        createXPath("./@name").
                        selectSingleNode(node).
                        getStringValue();
                List<Node> items = document.createXPath("./selection/dataItem").selectNodes(node);
                items.stream().forEach(item -> {
                    String itemName = document.createXPath("./@name").selectSingleNode(item).getStringValue();

                    StringBuilder sb = new StringBuilder(schemaName.trim()).append(".").append(itemName.trim().replaceAll("\\[", "").replaceAll("]", ""));

                    String expression = document.createXPath("./expression").selectSingleNode(item).getStringValue();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    itemsMap.put(sb.toString(), expression);
                });
            });
            LOGGER.info(map);
        }
        LOGGER.info(" Load items successfully .");
    }

    private void searchDataItem() {
        LOGGER.info("Start parsing report now ...");
        XPath list = document.createXPath("//layout//pageBody//list");
        List<Node> nodeList = list.selectNodes(document);
        nodeList.stream().forEach(node -> {
            String schemaName = document.
                    createXPath("./@refQuery").
                    selectSingleNode(node).
                    getStringValue();
            List<Node> items = document.
                    createXPath(".//dataSource/dataItemValue/@refDataItem").
                    selectNodes(node);
            for (Node item : items) {
                String itemName = item.getStringValue();
                ReportModel model = new ReportModel();
                model.setRefDataItem(schemaName.trim() + "." + itemName);
                model.setName(itemName);
                if (itemsMap.containsKey(model.getRefDataItem())) {

                    model.setExpression(itemsMap.get(model.getRefDataItem()));
                } else if (queryMap.containsKey(model.getRefDataItem())) {
                    List<String> exps = queryMap.get(model.getRefDataItem());
                    StringBuilder sb = new StringBuilder();
                    for (String str : exps) {
                        sb.append(str + "\\n");
                    }
                    model.setExpression(sb.toString());
                    traceQueryItem(model, exps);
                    System.exit(1);

                } else {
                    model.setExpression("Sorry, it is join stuff .");
                }
                traceItem(model);
            }


        });
        LOGGER.info("Report parsed successfully.");
    }

    private void traceQueryItem(ReportModel model, List<String> exps) {
        Pattern pattern = Pattern.compile("(\\[([^\\[\\]]*)?]\\.){1,2}\\[([^\\[\\]]*)?]");
        Pattern calPatt = Pattern.compile("](\\s)*(\\+|-|\\*|/)(\\s)*\\[");
        boolean flag = false;
        List<String> totalRef = new ArrayList<>();
        for (String exp : exps) {
            LOGGER.info("exp " + exp);
            if (exp.contains("[")) {
                if (calPatt.matcher(exp).find() && !exp.contains("case")) {
                    String tmp = searchCalc(exp, exp.substring(exp.indexOf("[") + 1, exp.indexOf("]")));
                    Matcher matcher = pattern.matcher(tmp);
                    while (matcher.find()) {
                        LOGGER.info(matcher.group());
                        String key = matcher.group();
                        buildQueryMapInfo(model, key);
                    }
                } else {
                    Matcher matcher = pattern.matcher(exp);
                    while (matcher.find()) {
                        String key = matcher.group();
                        buildQueryMapInfo(model, key);
                    }
                }
            }
        }
        if (!flag) {
            stop.add(model);
        }

    }

    private void traceItem(ReportModel model) {
        Pattern pattern = Pattern.compile("(\\[([^\\[\\]]*)?]\\.){1,2}\\[([^\\[\\]]*)?]");
        Pattern calPatt = Pattern.compile("](\\s)*(\\+|-|\\*|/)(\\s)*\\[");
        if (model.getExpression().contains("[")) {
            String exp = model.getExpression();
            String refData = model.getRefDataItem();
            if (calPatt.matcher(exp).find() && !exp.contains("case")) {
                refData = searchCalc(exp, refData.split("\\.")[0]);
                Matcher matcher = pattern.matcher(refData);
                while (matcher.find()) {
                    buildMapInfo(model, matcher.group());
                }
                model.setRefDataItem(refData);
            } else {
                refData = exp;
                Matcher matcher = pattern.matcher(refData);
                while (matcher.find()) {
                    String key = matcher.group();
                    String replacement = findRoot(key);
                    refData = refData.replace(key, replacement);
                    buildMapInfo(model, replacement);
                }
                model.setRefDataItem(refData);
            }


            further.add(model);

        } else {
            stop.add(model);
        }

    }

    private void buildQueryMapInfo(ReportModel model, String exp) {
        String[] components = exp.split("\\.");
        String layerName = components[0].substring(components[0].indexOf("[") + 1, components[0].indexOf("]"));
        if (!layers.contains(layerName)) {
            layers.add(layerName);
        }
        List<MapInfo> infos = new ArrayList<>();
        findQueryRoot(infos, exp.replaceAll("\\[", "").replaceAll("]", ""));
        LOGGER.info("Size :" + infos.size());

    }

    private void findQueryRoot(List<MapInfo> infoList, String key) {
        if (!queryMap.containsKey(key)) {
            MapInfo info = new MapInfo();
            String[] tmp = key.split("\\.");
            info.setBusinessLayer(tmp[0].trim());
//            info.setReportColName(tmp[1].trim());
            if (tmp.length == 2) {
                info.setReportColName(tmp[1].trim());
            } else if (tmp.length == 3) {
                info.setReportTableName(tmp[1].trim());
                info.setReportColName(tmp[2].trim());
            }
            if (!infoList.contains(info))
                infoList.add(info);
        } else {
            List<String> list = queryMap.get(key);
            for (String str : list) {
                String val = str.replaceAll("\\[", "").replaceAll("]", "");
                findQueryRoot(infoList, val);
            }
        }
    }

    private void buildMapInfo(ReportModel model, String exp) {
        String[] components = exp.split("\\.");
        MapInfo info = new MapInfo();
        info.setBusinessLayer(components[0].substring(components[0].indexOf("[") + 1, components[0].indexOf("]")));
        String layerName = components[0].substring(components[0].indexOf("[") + 1, components[0].indexOf("]"));
        if (!layers.contains(layerName)) {
            layers.add(layerName);
        }
        if (components.length == 2) {
            String colName = components[1].substring(components[1].indexOf("[") + 1, components[1].indexOf("]"));
            info.setReportColName(colName);
        } else if (components.length == 3) {
            info.setReportTableName(components[1].substring(components[1].indexOf("[") + 1, components[1].indexOf("]")));
            info.setReportColName(components[2].substring(components[2].indexOf("[") + 1, components[2].indexOf("]")));
        }

        if (model.getInfos() == null) {
            Set infos = new HashSet<>();
            model.setInfos(infos);
        }
        if (!model.getInfos().contains(info)) {

            model.getInfos().add(info);
        }

    }


    private String findRoot(String key) {
        Pattern pattern = Pattern.compile("(\\[([^\\[\\]]*)?]\\.){1,2}\\[(.*)?]");

        String template = key.replaceAll("\\[", "").replaceAll("]", "");
        if (itemsMap.containsKey(template)) {
            String value = itemsMap.get(template);
            return findRoot(value);
        } else {
            if (key.contains("case")) {
//                key = key.replaceAll("\\n(\\s){2,}", " ");
                Matcher matcher = pattern.matcher(key);
                while (matcher.find()) {
                    String item = matcher.group();
                    String value = findRoot(item).replaceAll("\\n(\\s){2,}", " ");
                    key = key.replace(item, value);
                }
            }

            return key;


        }

    }

    private String searchCalc(String exp, String schema) {

        String[] rows = exp.
                replaceAll("][\\s\\)\\(]*(\\+|-|\\*|/)[\\s\\()]*\\[", "];[").
                split(";");
        for (String str : rows) {
            String key;
            if (str.indexOf("[") == str.lastIndexOf("[")) {
                key = schema + "." + str.substring(str.indexOf("[") + 1, str.indexOf("]"));
            } else {
                key = str;
            }
            String replacement = findRoot(key);
            exp = exp.replace(str, replacement);
        }

        return exp;
    }

    @Override
    public String[] search(String name) {
        if (!map.containsKey(name)) {
            LOGGER.error("Failed to find dataItem named:" + name);
            System.exit(1);
        }

        return map.get(name);
    }

    @Override
    public void export(File file) {

        ExcelWriter writer = new ExcelWriter();
        parse();
        LOGGER.info("total model number :" + further.size());
        LOGGER.info("Exporting ...");
        writer.write(further, stop, file);
        LOGGER.info("Stop File List : " + stop);
        further.clear();
        stop.clear();


    }

    private List<Node> search(String path, Object context) {
        XPath xPath = document.createXPath(path);
        return xPath.selectNodes(context);
    }

    private void loadQueryMap() {
        String query = "//queries/query";
        List<Node> queries = search(query, document);
        queries.stream().forEach(node -> {
            String queryName = node.selectSingleNode("./@name").getStringValue();
            if (node.selectSingleNode(".//queryOperation") == null) {
                List<Node> dataItems = node.selectNodes("./selection/dataItem");
                dataItems.stream().forEach(dataItem -> {
                    ArrayList<String> list = new ArrayList<String>();
                    String dIName = dataItem.selectSingleNode("./@name").getStringValue();
                    String expression = dataItem.selectSingleNode("./expression").getStringValue();
                    if (expression.contains("[")) {
                        if (expression.indexOf("[") == expression.lastIndexOf("[")) {
                            expression = "[" + queryName.trim() + "]" + "." + expression;
                        }
                    }
                    String key = queryName.trim() + "." + dIName.trim();
                    list.add(expression);
                    queryMap.put(key, list);
                });
            } else {
                Set<String> queryRefList = new HashSet<String>();
                List<Node> refNodes = node.selectNodes(".//queryRefs/queryRef/@refQuery");
                refNodes.stream().forEach(ref -> queryRefList.add(ref.getStringValue()));
                List<Node> queryItems = node.selectNodes(".//queryItem/@name");
                queryItems.stream().forEach(queryItem -> {
                    String itemName = queryItem.getStringValue();
                    List<String> list = new ArrayList<String>();
                    String key = queryName.trim() + "." + itemName.trim();
                    for (String str : queryRefList) {
                        String val = "[" + str.trim() + "]" + "." + "[" + itemName.trim() + "]";
                        list.add(val);
                    }
                    queryMap.put(key, list);
                });
            }
        });

    }

    public Set<String> getLayers() {
        return layers;
    }
}
