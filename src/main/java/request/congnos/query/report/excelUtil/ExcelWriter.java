package request.congnos.query.report.excelUtil;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import request.congnos.query.model.xmlutils.MapInfo;
import request.congnos.query.report.ReportModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Heng Song on 9/3/2015.
 * This class is used to generate excel report for the final result.
 */
public class ExcelWriter {
    private final static Logger LOGGER = Logger.getLogger(ExcelWriter.class);
    private String reportName;
    private Workbook workbook;
    private File file;
    private CellStyle cellStyle;
    private List<String> title;
    private List<String> mapInfo;

    public void write(List<ReportModel> infos, List<ReportModel> others, File file) {

        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            workbook = new XSSFWorkbook();
            Sheet report = workbook.createSheet("Report Columns");
            workbook.createCellStyle();
            cellStyle = workbook.createCellStyle();
            cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
            cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
            cellStyle.setWrapText(true);
            mapInfo = new ArrayList<>();
            Field[] fields = ReportModel.class.getDeclaredFields();
            List<String> header = new ArrayList<>();
            for (Field field : fields) {
                String name = field.getName();
                if (!name.equals("serialVersionUID") && !name.equals("infos")) {
                    header.add(name);
                }
            }
            fields = MapInfo.class.getDeclaredFields();
            for (Field field : fields) {
                String name = field.getName();
                if (!name.contains("serialVersionUID") && !(name.contains("calculation"))) {
                    mapInfo.add(name);
                }
            }
            setHeader(report, header);
            populateData(report, infos, header);
            for (int i = 0; i < header.size() + mapInfo.size(); i++) {
                report.autoSizeColumn(i);
            }
            if (others != null && !others.isEmpty()) {
                Sheet type2 = workbook.createSheet("Report Columns II");
                setPureHeader(type2, header);
                populatePureData(type2, header, others);
            }

            OutputStream outputStream = new FileOutputStream(file);
            workbook.write(outputStream);
            outputStream.close();
            LOGGER.info("Successfully export data to file: \t" + file.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    private void populateData(Sheet sheet, List<ReportModel> infos, List<String> header) throws InvocationTargetException, IllegalAccessException {
        int current = 2;
        for (ReportModel report : infos) {
            current = setData(sheet, current, header, report);
        }


    }

    private Method[] loadMethods(Class cls, List<String> names) {
        Method[] methods = new Method[names.size()];
        for (Method method : cls.getDeclaredMethods()) {
            String methodName = method.getName();
            if (methodName.contains("get")) {
                String tmp = methodName.replaceAll("get", "");
                char[] name = tmp.toCharArray();
                name[0] = Character.toLowerCase(name[0]);
                tmp = new String(name);
                int index = names.indexOf(tmp);
                if (index >= 0)
                    methods[index] = method;
            }
        }
        return methods;

    }

    private void populatePureData(Sheet sheet, List<String> header, List<ReportModel> infos) throws InvocationTargetException, IllegalAccessException {
        Method[] methods = loadMethods(ReportModel.class, header);
        final int base = 1;
        for (int row = 0; row < infos.size(); row++) {
            Row line = sheet.createRow(row + base);
            for (int col = 0; col < header.size(); col++) {
                Cell cell = line.createCell(col);
                Object obj = methods[col].invoke(infos.get(row));
                cell.setCellValue((String) obj);
                if (row == infos.size() - 1) {
                    sheet.autoSizeColumn(col);
                }
            }
        }

    }

    private int setData(Sheet sheet,
                        int startRow,
                        List<String> header,
                        ReportModel model
    ) throws InvocationTargetException, IllegalAccessException {

        List<MapInfo> infos = new ArrayList<>(model.getInfos());
        Method[] modelMethods = loadMethods(MapInfo.class, mapInfo);
        Method[] methods = loadMethods(ReportModel.class, header);


        for (int row = 0; row < infos.size(); row++) {
            MapInfo curInfo = infos.get(row);
            Row curRow = sheet.createRow(row + startRow);
            int col;
            for (col = 0; col < header.size(); col++) {
                Cell cell = curRow.createCell(col);
                Method method = methods[col];
                String value = (String) method.invoke(model);
                cell.setCellStyle(cellStyle);
                cell.setCellValue(value);
                if (row == infos.size() - 1) {
                    sheet.autoSizeColumn(col);
                    sheet.addMergedRegion(new CellRangeAddress(startRow, startRow + row, col, col));
                }
            }
            int base = header.size();
            for (col = 0; col < mapInfo.size(); col++) {
                Cell cell = curRow.createCell(base + col);
                Method method = modelMethods[col];
                String value;
//                String value = (String) method.invoke(curInfo);
                Object obj = null;
                try {
                    obj = method.invoke(curInfo);
                } catch (Exception e) {

                    LOGGER.error(method.getName());
                    LOGGER.error(curInfo);
                    LOGGER.error(obj);
                    LOGGER.error(row);

                }
                if (obj instanceof List) {
                    List item = (List) obj;
                    value = item.toString();
                } else {
                    value = String.valueOf(obj);
                }
                cell.setCellStyle(cellStyle);
                cell.setCellValue(value);
                if (row == infos.size() - 1) {
                    sheet.autoSizeColumn(col + base);
                }
            }


        }


        return startRow + infos.size();
    }

    private void setPureHeader(Sheet sheet, List<String> titleList) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        font.setFontHeight((short) (14 * 20));
        style.setFont(font);
        style.setBorderBottom(CellStyle.BORDER_THIN);
        style.setBorderLeft(CellStyle.BORDER_THIN);
        style.setBorderRight(CellStyle.BORDER_THIN);
        style.setBorderTop(CellStyle.BORDER_THIN);
        style.setFillForegroundColor(IndexedColors.SEA_GREEN.getIndex());
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setAlignment(CellStyle.ALIGN_CENTER);
        Row header = sheet.createRow(0);
        for (int i = 0; i < titleList.size(); i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(titleList.get(i));
            cell.setCellStyle(style);
        }
    }

    private void setHeader(Sheet sheet, List<String> titleList) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        font.setFontHeight((short) (14 * 20));
        style.setFont(font);
        style.setBorderBottom(CellStyle.BORDER_THIN);
        style.setBorderLeft(CellStyle.BORDER_THIN);
        style.setBorderRight(CellStyle.BORDER_THIN);
        style.setBorderTop(CellStyle.BORDER_THIN);
        style.setFillForegroundColor(IndexedColors.SEA_GREEN.getIndex());
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setAlignment(CellStyle.ALIGN_CENTER);
        for (int row = 0; row < 2; row++) {
            Row header = sheet.createRow(row);
            for (int curCell = 0; curCell < titleList.size(); curCell++) {
                Cell cell = header.createCell(curCell);
                cell.setCellValue(titleList.get(curCell));
//            cell.setCellValue(reportTitle.get(curCell));


//                style.setVerticalAlignment(CellStyle.ALIGN_CENTER_SELECTION);
                cell.setCellStyle(style);
                if (row == 1) {
                    sheet.addMergedRegion(new CellRangeAddress(0, 1, curCell, curCell));
                }
            }
        }
        int base = titleList.size();
        Row row = sheet.getRow(0);
        for (int col = 0; col < mapInfo.size(); col++) {
            Cell cell = row.createCell(base + col);
            cell.setCellStyle(style);
            cell.setCellValue("Model Info ");
        }
        sheet.addMergedRegion(new CellRangeAddress(0, 0, base, base + mapInfo.size() - 1));
        row = sheet.createRow(1);
        for (int col = 0; col < mapInfo.size(); col++) {
            Cell cell = row.createCell(base + col);
            cell.setCellStyle(style);
            cell.setCellValue(mapInfo.get(col));

        }

    }


}
