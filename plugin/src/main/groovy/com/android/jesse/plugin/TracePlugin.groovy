package com.android.jesse.plugin

import com.android.build.gradle.AppExtension
import com.android.jesse.Log
import com.android.jesse.extension.TraceExtension
import com.android.jesse.transforms.TraceTransform
import org.gradle.api.Plugin
import org.gradle.api.Project

class TracePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        TraceExtension traceExtension = project.extensions.create('privacyTrace', TraceExtension)

        //注册
        AppExtension appExtension = project.extensions.findByType(AppExtension.class)
        appExtension.registerTransform(new TraceTransform(traceExtension))
    }
}