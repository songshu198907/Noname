import org.junit.Test;
import request.congnos.query.model.xmlutils.MapInfo;
import request.congnos.query.model.xmlutils.XMLParser;
import request.congnos.query.report.ReportParser;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;


/**
 * Created by Heng Song on 3/31/2016.
 */
public class XMLTest {



    @Test
    public void testXMLParser() throws Exception {
        Set<String> layers = new HashSet<>();
//        layers.add("BUSINESS_VIEW");

        XMLParser.run("mrdt_model.xml", layers);
        XMLParser.run("sctr_model.xml", layers);
        System.out.println("total nodes :" + XMLParser.getTotalNodes());
        MapInfo res = XMLParser.findElement("REQST_MRDT Business Layer", "Firm", "Firm ID");
        System.out.println(res);
        res = XMLParser.findElement("REQST_MRDT Business Layer", "FOCUS", "Filing Count");
        System.out.println(res);
        res = XMLParser.findElement("REQST_MRDT Business Layer", "FOCUS", "I0210 | Cash Seg W/Fed & Oth Regs");
        System.out.println(res);
        res = XMLParser.findElement("BUSINESS_VIEW", "Filing Meta", "SEQ_NB");
        System.out.println(res);

    }


    @Test
    public void fastTest() {
        long start = System.currentTimeMillis();
        XMLParser.deserialization();
        MapInfo res = XMLParser.findElement("REQST_MRDT Business Layer", "Firm", "Firm ID | Firm");
        System.out.println(res);
        res = XMLParser.findElement("REQST_MRDT Business Layer", "FOCUS", "Filing Count");
        System.out.println(res);
        res = XMLParser.findElement("REQST_MRDT Business Layer", "Firm", "Firm ID");
        System.out.println(res);
        res = XMLParser.findElement("REQST_MRDT Business Layer", "VIEW ONLY Items", "VIEW_ONLY_Firm Name");
        System.out.println(res);

        long end = System.currentTimeMillis();
        long duration = end - start;
        System.out.println("time elpsed for duration: " + duration);


    }

    @Test
    public void testReport() throws IOException, NoSuchAlgorithmException {
        ReportParser parser = new ReportParser("report_model.xml");
        parser.load("report_spec2.xml");
        parser.export(new File("report_spec2_2.xlsx"));
        parser.load("report_spec1.xml");
        parser.export(new File("report_spec1_2.xlsx"));
        parser = new ReportParser("mrdt_model.xml");
        parser.load("mrdt_report.xml");
        parser.export(new File("mrdt_report2.xlsx"));


    }


}
