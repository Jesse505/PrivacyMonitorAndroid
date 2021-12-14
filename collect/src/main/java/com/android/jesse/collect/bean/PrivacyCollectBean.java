package com.android.jesse.collect.bean;

public class PrivacyCollectBean {
    public String monitorMethod;
    public String monitorStackTrace;
    public String privacyMethod;

    public PrivacyCollectBean(String monitorMethod, String monitorStackTrace, String privacyMethod) {
        this.monitorMethod = monitorMethod;
        this.monitorStackTrace = monitorStackTrace;
        this.privacyMethod = privacyMethod;
    }
}
