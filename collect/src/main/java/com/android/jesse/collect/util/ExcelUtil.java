package com.android.jesse.collect.util;

import com.android.jesse.collect.ExcelBuildDataListener;
import com.android.jesse.collect.bean.PrivacyCollectBean;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.format.Alignment;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.format.Colour;
import jxl.write.Label;
import jxl.write.WritableCell;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

public class ExcelUtil {

    private static final String UTF8_ENCODING = "UTF-8";

    private static WritableFont arial14font;
    private static WritableCellFormat arial14format;
    private static WritableFont arial10font;
    private static WritableCellFormat arial10format;
    private static WritableFont arial12font;
    private static WritableCellFormat arial12format;

    /**
     * 表格初始化
     *
     * @param filePath      文件路径
     * @param sheetNameList 表格名字列表
     * @param colNameList   列名字列表
     */
    public static void initExcel(String filePath, List<String> sheetNameList, List<List<String>> colNameList) {
        format();
        WritableWorkbook workbook = null;

        try {
            File file = new File(filePath);
            if (file.exists()) {
                file.deleteOnExit();
            }
            if (!(file.getParentFile().exists())) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();

            workbook = Workbook.createWorkbook(file);

            if (null != sheetNameList) {
                for (int i = 0; i < sheetNameList.size(); i++) {
                    //设置表格的名字
                    WritableSheet sheet = workbook.createSheet(sheetNameList.get(i), i);
                    //创建标题栏
                    sheet.addCell((WritableCell) (new Label(0, 0, filePath, arial14format)));
                    //设置第一行的数据
                    if (i < colNameList.size()) {
                        List<String> colNames = colNameList.get(i);
                        if (null != colNames) {
                            for (int j = 0; j < colNames.size(); j++) {
                                sheet.addCell(new Label(j, 0, colNames.get(j), arial10format));
                            }
                        }
                    }
                    //设置行高
                    sheet.setRowView(0, 340);
                }
            }
            workbook.write();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void writeObjListToExcel(String filePath, int sheetIndex,
                                           List<PrivacyCollectBean> collectBeanList, ExcelBuildDataListener listener) {
        if (collectBeanList == null || collectBeanList.isEmpty()) {
            return;
        }
        WritableWorkbook writebook = null;
        InputStream inputStream = null;

        try {
            WorkbookSettings workbookSettings = new WorkbookSettings();
            workbookSettings.setEncoding(UTF8_ENCODING);
            inputStream = new FileInputStream(new File(filePath));
            Workbook workbook = Workbook.getWorkbook(inputStream);
            writebook = workbook.createWorkbook(new File(filePath), workbook);
            WritableSheet sheet = writebook.getSheet(sheetIndex);
            for (int i = 0; i < collectBeanList.size(); i++) {
                if (null != listener) {
                    List<String> list = listener.buildData(sheetIndex, collectBeanList.get(i));
                    if (null != list) {
                        for (int j = 0; j < list.size(); j++) {
                            sheet.addCell(new Label(j, i + 1, list.get(j), arial12format));
                            if (list.get(j).length() <= 4) {
                                //设置列宽
                                sheet.setColumnView(i, list.get(j).length() + 8);
                            } else {
                                //设置列宽
                                sheet.setColumnView(i, list.get(j).length() + 5);
                            }
                        }
                    }
                }

                //设置行高
                sheet.setRowView(i + 1, 500);
            }
            writebook.write();
            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writebook != null) {
                try {
                    writebook.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }

    private static void format() {
        try {
            arial14font = new WritableFont(WritableFont.ARIAL, 14, WritableFont.BOLD);
            arial14font.setColour(Colour.LIGHT_BLUE);
            arial14format = new WritableCellFormat(arial14font);
            arial14format.setAlignment(Alignment.CENTRE);
            arial14format.setBorder(Border.ALL, BorderLineStyle.THIN);
            arial14format.setBackground(Colour.VERY_LIGHT_YELLOW);
            arial10font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
            arial10format = new WritableCellFormat(arial10font);
            arial10format.setAlignment(Alignment.CENTRE);
            arial10format.setBorder(Border.ALL, BorderLineStyle.THIN);
            arial10format.setBackground(Colour.GRAY_25);
            arial12font = new WritableFont(WritableFont.ARIAL, 10);
            arial12format = new WritableCellFormat(arial12font);
            //对齐格式
            arial10format.setAlignment(Alignment.CENTRE);
            //设置边框
            arial12format.setBorder(Border.ALL, BorderLineStyle.THIN);
        } catch (WriteException e) {
            e.printStackTrace();
        }
    }
}
