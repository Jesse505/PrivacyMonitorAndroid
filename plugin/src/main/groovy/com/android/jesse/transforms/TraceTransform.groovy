package com.android.jesse.transforms

import com.android.jesse.TraceBuildConfig
import com.android.jesse.extension.TraceExtension
import com.android.jesse.visitor.TraceClassVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

class TraceTransform extends BaseTransform {

    private HashSet<String> exclude = new HashSet<>()
    private TraceExtension traceExtension
    private TraceBuildConfig traceBuildConfig

    TraceTransform(TraceExtension traceExtension) {
        this.traceExtension = traceExtension
    }

    @Override
    boolean isShouldModify(String className) {
        //不需要修改字节码的一些包前缀
        exclude.add('android/support')
        exclude.add('androidx')

        traceBuildConfig?.mBlackPackageList?.each {
            String blackPackage -> exclude.add(blackPackage)
        }

        Iterator<String> iterator = exclude.iterator()
        while (iterator.hasNext()) {
            String packageName = iterator.next()
            if (className.startsWith(packageName)) {
                return false
            }
        }

        //自动生成的一些文件不需要修改字节码
        if (className.contains('R$') ||
                className.contains('R2$') ||
                className.contains('R.class') ||
                className.contains('R2.class') ||
                className.contains('BuildConfig.class')) {
            return false
        }

        //配置的一些class不需要修改字节码
        for (int i = 0; i < traceBuildConfig?.mBlackClassList?.size(); i++) {
            if (traceBuildConfig?.mBlackClassList?.get(i) ==
                    className.replace(".class", "")) {
                return false
            }
        }

        return true
    }

    @Override
    byte[] modifyClass(byte[] srcClass) throws IOException {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS)
        ClassVisitor classVisitor = new TraceClassVisitor(classWriter, traceBuildConfig, isLogEnable())
        ClassReader cr = new ClassReader(srcClass)
        cr.accept(classVisitor, ClassReader.SKIP_FRAMES)
        return classWriter.toByteArray()
    }

    @Override
    String getName() {
        return "TraceTransform"
    }

    @Override
    void onBeforeTransform() {
        super.onBeforeTransform()
        final TraceBuildConfig traceConfig = initConfig()
        traceConfig.parseTraceConfigFile()
        traceBuildConfig = traceConfig
    }

    @Override
    boolean isModifyEnable() {
        return traceExtension.enable
    }

    @Override
    boolean isLogEnable() {
        return traceExtension.logEnable
    }

    private TraceBuildConfig initConfig() {
        return new TraceBuildConfig(traceExtension.traceConfigFile)
    }
}