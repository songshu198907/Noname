package request.congnos.query.model.xmlutils;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.XPath;
import org.dom4j.io.SAXReader;
import request.congnos.query.model.tracer.XMLTracer;
import request.congnos.query.model.tree.BPTree;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by Heng Song on 4/4/2016.
 */
public class XMLParser {
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(XMLParser.class);
    /**
     * The name of packages need to parse.
     */
    private static List<String> pkgNames;
    /**
     * Document .
     */
    private static Document document;
    /**
     * The map contains info about which model has already been parsed.
     */
    private static Map<String, String> pkgMap;
    /**
     * Queue used to work with multithreads program.
     */
    private static ConcurrentLinkedQueue<MapInfo> queue;
    /**
     * Map contains md5 checksum of the file.
     */
    private static Map<String, byte[]> checkSumMap;
    /**
     * B plus tree stores all info.
     */
    private static BPTree tree;
    /**
     * Buffer size .
     */
    private static int BUFSIZE = 2048;

    /**
     * Constructor.
     */
    private XMLParser() {
        loadName();
        tree = new BPTree(6);
        pkgMap = new HashMap<>();
        queue = new ConcurrentLinkedQueue<>();
        checkSumMap = new HashMap<>();

    }

    /**
     * Load the xml file with file name.
     *
     * @param fileName The name of the file. Make sure the file
     *                 is put in resources folder.
     */
    private static void loadFile(String fileName) {
        SAXReader reader = new SAXReader();
        try {

            document = reader.read(ClassLoader.getSystemResource(fileName));
            document.accept(new NameSpaceCleaner());
        } catch (DocumentException e) {
            LOGGER.error("Failed to load xml file:\n" + e.getMessage());
        }
    }

    /**
     * The method parses the model xml file and store info in tree.
     *
     * @param layerNames The name of business layers.
     * @throws InterruptedException Exception.
     *                              This program mainly uses xpath to find the reference jobs for
     *                              the each job until reach the physical view .
     */
    private static void trace(Set<String> layerNames) throws InterruptedException {
        String layerTemplate = "//namespace[./name/text()=\"%s\"]/folder[./name/text()='Items']//folder/name";
        String subjectTemplate = "//namespace[./name/text()=\"%s\"]/querySubject";
        for (String str : layerNames) {
            String layerPath = String.format(layerTemplate, str);
            String columnPath = "./parent::folder//calculation/name";
            LOGGER.debug("layer xpath :\t" + layerPath);
            XPath lPath = document.createXPath(layerPath);
            XPath cPath = document.createXPath(columnPath);
            List<Node> bTables = lPath.selectNodes(document);
            if (bTables.size() > 0) {
                bTables.forEach((bTable) -> {
                    String tableName = bTable.getStringValue();
                    LOGGER.debug("table name :" + tableName);
                    List<Node> cNames = cPath.selectNodes(bTable);
                    cNames.stream().forEach((column) -> {
                        MapInfo info = new MapInfo();
                        info.setBusinessLayer(str);
                        info.setReportTableName(tableName);
                        info.setReportColName(column.getStringValue());

                        if (!queue.offer(info)) {
                            LOGGER.error("Failed to push node into queue \n" + info);
                        }
                    });


                });
            }
            String tablePath = String.format(subjectTemplate, str);
            XPath tPath = document.createXPath(tablePath);
            List<Node> tables = tPath.selectNodes(document);
            tables.stream().forEach(table -> {
                XPath tbNamePath = document.createXPath("./name");
                XPath colNamePath = document.createXPath("./queryItem/name");
                String tbName = tbNamePath.selectSingleNode(table).getStringValue();
                List<Node> colNames = colNamePath.selectNodes(table);
                colNames.stream().forEach(colName -> {
                    String cName = colName.getStringValue();
                    MapInfo info = new MapInfo();
                    info.setBusinessLayer(str);
                    info.setReportTableName(tbName);
                    info.setReportColName(cName);
                    if (!queue.offer(info)) {
                        LOGGER.error("Failed to push node into queue \n" + info);
                    }
                });
            });
        }
        LOGGER.error(queue.size());
        int processor = Runtime.getRuntime().availableProcessors() / 2;
        List<Future> futureList = new ArrayList<>();
        ExecutorService service = Executors.newFixedThreadPool(processor);
        for (int i = 0; i < processor; i++) {
            XMLTracer tracer = new XMLTracer(document, queue, tree);
            futureList.add(service.submit(tracer));
        }
        service.shutdown();
        int fin = 0;
        while (true) {
            fin = 0;
            for (Future future : futureList) {
                if (future.isDone()) {
                    fin++;
                }
            }
            LOGGER.info(fin + "/" + processor + "futures are done! " + queue.size() + " elements in the queue");
            Thread.sleep(5000);
            if (fin == futureList.size()) {
                break;
            }
        }
        LOGGER.info("Build tree successfully ");

    }

    /**
     * Without providing any layer name, this method.
     * will search in the xml to find layer name itself
     * from package item in xml file.
     *
     * @return A set of layer names .
     */
    private static Set<String> findLayer() {
        Set<String> result = new HashSet<>();
        XPath pkgName = document.createXPath("//package/name");
        List<Node> pkgs = pkgName.selectNodes(document);

        if (pkgNames == null) {
            throw new RuntimeException("Fail to load the package name lists");
        }
        String template = "//name[text()='%s']//following-sibling::definition/set/refobj";
        pkgs.stream().filter(pkgNode -> pkgNames.contains(pkgNode.getStringValue())).forEach((node) -> {
            String name = node.getStringValue();


            if (!pkgMap.containsKey(name)) {
                String path = String.format(template, name);
                XPath bl = document.createXPath(path);
                Node layerNode = bl.selectSingleNode(document);
                String total = layerNode.getStringValue();
                String layerName = total.substring(total.indexOf("[") + 1, total.indexOf("]"));
                pkgMap.put(name, layerName);
                result.add(layerName);

            } else {
                result.add(pkgMap.get(name));
            }
        });
        LOGGER.debug(pkgMap);
        return result;
    }

    /**
     * Method used to search item from tree.
     *
     * @param infos String array including layer name , table name and column name.
     * @return MapInfo Object.
     * A MapInfo object is returned with all info.
     */
    public static MapInfo findElement(String... infos) {
        if (infos.length != 3) {
            throw new RuntimeException("Three paras should be set for findElement method not " + infos.length);
        }
        int hash = infos[0].hashCode();
        hash = 31 * hash + infos[1].hashCode();
        hash = 31 * hash + infos[2].hashCode();
        return (MapInfo) tree.get(hash);

    }

    /**
     * Method used to search item from tree.
     *
     * @param info MapInfo object contains layer name and column name.
     * @return A MapInfo object is returned with all info.
     */
    public static MapInfo findElement(MapInfo info) {
        return (MapInfo) tree.get(info.hashCode());
    }

    /**
     * Main method of this class.
     * It will check if the target model has been parsed or not .
     * The program only parses xml when the target xml has not been parsed
     * or has been changed. Other with it will deserialization from file.
     *
     * @param fileName The file name of model xml. The file should be
     *                 put in resources folder.
     * @param layers
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public static void run(String fileName, Set<String> layers) throws IOException, NoSuchAlgorithmException {
        new XMLParser();
        if (loadCheckSum()) {
            deserialization();
        }
        boolean all = false;
        MessageDigest md = MessageDigest.getInstance("MD5");

        InputStream in = ClassLoader.getSystemResourceAsStream(fileName);
        DigestInputStream din = new DigestInputStream(in, md);
        byte[] buff = new byte[BUFSIZE];
        while (din.read(buff) != -1) ;
        din.close();
        byte[] digest = md.digest();
        Set<String> existing = new HashSet<>();
        if (layers == null || layers.isEmpty()) {
            String key = fileName + ".all";
            if (checkSumMap.containsKey(key) && Arrays.equals(digest, checkSumMap.get(key))) {
                return;
            } else {
                checkSumMap.put(key, digest);
                all = true;
            }
        } else {
            for (String layer : layers) {
                String key = fileName + "." + layer;
                if (checkSumMap.containsKey(key) && Arrays.equals(digest, checkSumMap.get(key))) {
                    existing.add(layer);
                } else {
                    key = fileName + ".all";
                    if (checkSumMap.containsKey(key) && Arrays.equals(digest, checkSumMap.get(key))) {
                        existing.add(layer);
                    }
                }

            }
            layers.removeAll(existing);
        }
        if (layers.isEmpty() && !all) {
            return;
        } else {
            layers.stream().forEach(layer -> {
                LOGGER.info(fileName + "." + layer + " has been changed or hasn't been serialized .\n It needs to be parsed!");
                        checkSumMap.put(fileName + "." + layer, digest);
                    }
            );

            loadFile(fileName);
            if (all) {
                layers = findLayer();
            }
            if (!all && layers.isEmpty()) {
                return;
            }
            try {
                trace(layers);
                serialization();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    public static void reload(String fileName, Set<String> layers) {
        try {
            loadCheckSum();
            new XMLParser();
            loadFile(fileName);
            deserialization();
            LOGGER.info("Start reloading file :" + fileName + " layer: " + layers);
            trace(layers);
            serialization();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Serialize the tree object and check sum map.
     * Persist them in the file.
     */
    public static void serialization() {
        try {
            File folder = new File("tmp");
            if (!folder.exists()) {
                folder.mkdirs();
            }
            FileOutputStream os = new FileOutputStream(new File(folder, "bpTree.data"));
            ObjectOutputStream ostream = new ObjectOutputStream(os);
            ostream.writeObject(tree);
            os = new FileOutputStream(new File(folder, "pkMap.data"));
            ostream = new ObjectOutputStream(os);
            ostream.writeObject(pkgMap);
            os = new FileOutputStream(new File(folder, "checkSum.data"));
            ostream = new ObjectOutputStream(os);
            ostream.writeObject(checkSumMap);
            LOGGER.info("Serialize successfully ! ");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load tree info from file .
     */

    public static void deserialization() {
        try {
            File file = new File("tmp/bpTree.data");
            FileInputStream fin = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fin);
            Object obj = in.readObject();
            if (obj instanceof BPTree) {
                LOGGER.info("Deserialize successfully!");
                tree = (BPTree) obj;

            }

            file = new File("tmp/pkMap.data");
            fin = new FileInputStream(file);
            in = new ObjectInputStream(fin);
            obj = in.readObject();
            pkgMap = (Map) obj;

            return;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        LOGGER.error("Fail to deserialize!");


    }

    /**
     * Load check sum map.
     *
     * @return boolean
     * If the file exists return true otherwise return false.     *
     */
    private static boolean loadCheckSum() {
        File file = new File("tmp/checkSum.data");
        FileInputStream fin = null;
        boolean flag = true;
        try {
            fin = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fin);
            Object obj = in.readObject();
            checkSumMap = (Map) obj;
        } catch (FileNotFoundException e) {
            flag = false;
        } catch (ClassNotFoundException e) {
            flag = false;
        } catch (IOException e) {
            flag = false;
        } finally {
            return flag;
        }

    }

    /**
     * Method to load total nodes number
     *
     * @return number of nodes
     */
    public static int getTotalNodes() {
        return tree.getTotalNodes();
    }

    /**
     * Load the package name need to parse.
     */
    private void loadName() {
        try {
            pkgNames = IOUtils.readLines(getClass().getClassLoader().getResourceAsStream("packageName.dat"));
        } catch (IOException e) {
            LOGGER.error("Fail to load packageName.dat. \t " + e.getMessage());
        }
    }
}
