package com.android.jesse.collect;

import com.android.jesse.collect.bean.PrivacyCollectBean;

import java.util.List;

public interface ExcelBuildDataListener {
    List<String> buildData(int sheetIndex, PrivacyCollectBean bean);
}
