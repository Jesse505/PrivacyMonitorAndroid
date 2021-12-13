package com.android.jesse.collect;

import android.content.Context;
import android.util.Log;

import com.android.jesse.collect.bean.PrivacyCollectBean;
import com.android.jesse.collect.util.ExcelUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PrivacyCollect {

    private static final String EXCEL_FILE_NAME = "privacy_result.xls";

    static List<PrivacyCollectBean> collectBeans = new ArrayList<>();

    public static void stopCollect(Context context) {
        String filePath = context.getExternalFilesDir(null) + File.separator + EXCEL_FILE_NAME;
        Log.i("zyf", "保存地址 > " + filePath);
        ArrayList<String> sheetNameList = new ArrayList<>();
        sheetNameList.add("隐私合规");
        ArrayList<String> colNameList = new ArrayList<>();
        colNameList.add("调用堆栈");
        colNameList.add("隐私函数名");
        ArrayList<List<String>> colList = new ArrayList<>();
        colList.add(colNameList);
        ExcelUtil.initExcel(filePath, sheetNameList, colList);

        ArrayList<PrivacyCollectBean> privacyCollectBeans = new ArrayList<>();
        privacyCollectBeans.addAll(collectBeans);
        ExcelUtil.writeObjListToExcel(filePath, 0, privacyCollectBeans, new ExcelBuildDataListener() {

            @Override
            public List<String> buildData(int sheetIndex, PrivacyCollectBean bean) {
                ArrayList<String> strings = new ArrayList<>();
                if (null != bean) {
                    strings.add(bean.monitorMethod);
                    strings.add(bean.privacyMethod);
                }
                return strings;
            }
        });
        collectBeans.clear();
    }

    public static void appendData(String monitorMethod, String privacyMethod) {
        Log.i("zyf", "在" + monitorMethod + "方法中调用了" + privacyMethod + "方法");
        collectBeans.add(new PrivacyCollectBean(monitorMethod, privacyMethod));
    }
}
