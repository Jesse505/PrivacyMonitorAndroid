package com.android.jesse.visitor

import com.android.jesse.Log
import com.android.jesse.TraceBuildConfig
import org.objectweb.asm.*

class TraceClassVisitor extends ClassVisitor implements Opcodes {

    private final
    static String SDK_API_CLASS = "com/android/jesse/privacymonitor/LogUtil"

    TraceBuildConfig mTraceBuildConfig
    String traceClassName
    boolean mLogEnable

    TraceClassVisitor(final ClassVisitor classVisitor, TraceBuildConfig traceBuildConfig, boolean logEnable) {
        super(Opcodes.ASM6, classVisitor)
        mTraceBuildConfig = traceBuildConfig
        mLogEnable = logEnable
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces)
        traceClassName = name
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

        Set<String> monitorMethodSet = new HashSet<>()


        MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions)
        methodVisitor = new MonitorDefaultMethodVisitor(methodVisitor, access, name, desc) {
            @Override
            void visitMethodInsn(int opcode, String owner, String name1, String desc1, boolean itf) {

                if (!mTraceBuildConfig.getmTraceMents()?.isEmpty()) {
                    for (int i = 0; i < mTraceBuildConfig.getmTraceMents().size(); i++) {
                        TraceBuildConfig.TraceMent replaceMent = mTraceBuildConfig.getmTraceMents().get(i)
                        if (owner == replaceMent.getSrcClass() && name1 == replaceMent.getSrcMethodName()) {

                            monitorMethodSet.add("${owner.replace("/", ".")}#$name1")

                            if (mLogEnable) {
                                Log.i("TraceClassVisitor", "在${traceClassName}类中调用了" +
                                        "${replaceMent.getSrcClass()}类的${replaceMent.getSrcMethodName()}方法")
                            }
                        }
                    }
                }
                super.visitMethodInsn(opcode, owner, name1, desc1, itf)
            }

            @Override
            protected void onMethodExit(int opcode) {
                super.onMethodExit(opcode)

                for (int i = 0; i < monitorMethodSet.size(); i++) {
                    methodVisitor.visitLdcInsn(String.valueOf("${traceClassName.replace("/", ".")}#$name"))
                    methodVisitor.visitLdcInsn(String.valueOf(monitorMethodSet[i]))
                    methodVisitor.visitMethodInsn(INVOKESTATIC, SDK_API_CLASS, "trackViewOnClick", "(Ljava/lang/String;Ljava/lang/String;)V", false)
                }
            }
        }
        return methodVisitor
    }

}