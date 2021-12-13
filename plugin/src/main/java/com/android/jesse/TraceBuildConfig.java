package com.android.jesse;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TraceBuildConfig {

    private static final String TAG = "TraceBuildConfig";

    private final String mTraceConfigFile;

    private final List<TraceMent> mTraceMents;
    private final List<String> mBlackClassList;
    private final List<String> mBlackPackageList;


    public TraceBuildConfig(String traceConfigFile) {
        this.mTraceConfigFile = traceConfigFile;
        mTraceMents = new ArrayList<>();
        mBlackClassList = new ArrayList<>();
        mBlackPackageList = new ArrayList<>();
    }

    public List<TraceMent> getmTraceMents() {
        return mTraceMents;
    }

    public List<String> getmBlackClassList() {
        return mBlackClassList;
    }

    public List<String> getmBlackPackageList() {
        return mBlackPackageList;
    }

    public void parseTraceConfigFile() {
        if (Util.isNullOrNil(mTraceConfigFile)) {
            Log.w(TAG, "traceConfigFile not config");
            return;
        }
        File traceConfigFile = new File(mTraceConfigFile);
        if (!traceConfigFile.exists()) {
            Log.w(TAG, "trace config file not exist %s", traceConfigFile.getAbsoluteFile());
            return;
        }
        String traceConfigStr = Util.readFileAsString(traceConfigFile.getAbsolutePath());
        String[] configArray = traceConfigStr.split("\n");

        for (String config : configArray) {
            if (config.startsWith("-keepclass ")) {
                mBlackClassList.add(config.replace("-keepclass ", ""));
            } else if (config.startsWith("-keeppackage ")) {
                mBlackPackageList.add(config.replace("-keeppackage ", ""));
            } else if (config.startsWith("-tracemethod ")) {
                mTraceMents.add(getTraceMent(config.replace("-tracemethod ", "")));
            }
        }
    }

    /**
     * 解析被检测跟踪的方法配置
     *
     * @param traceStr
     * @return
     */
    private TraceMent getTraceMent(String traceStr) {
        TraceMent traceMent = new TraceMent();
        String[] srcStr = traceStr.split("\\.");
        if (srcStr.length == 2) {
            traceMent.setSrcClass(srcStr[0]);
            traceMent.setSrcMethodName(srcStr[1]);
        }
        return traceMent;
    }

    public static class TraceMent {
        private String srcClass;
        private String srcMethodName;

        public String getSrcClass() {
            return srcClass;
        }

        public void setSrcClass(String srcClass) {
            this.srcClass = srcClass;
        }

        public String getSrcMethodName() {
            return srcMethodName;
        }

        public void setSrcMethodName(String srcMethodName) {
            this.srcMethodName = srcMethodName;
        }

        @Override
        public String toString() {
            return "TraceMent{" +
                    "srcClass='" + srcClass + '\'' +
                    ", srcMethodName='" + srcMethodName + '\'' +
                    '}';
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }
}
