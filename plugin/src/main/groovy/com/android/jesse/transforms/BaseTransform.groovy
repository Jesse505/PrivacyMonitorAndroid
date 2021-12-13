package com.android.jesse.transforms

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.ide.common.internal.WaitableExecutor
import com.android.jesse.Log
import groovy.io.FileType
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils

import java.util.concurrent.Callable
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

abstract class BaseTransform extends Transform {



    public WaitableExecutor mWaitableExecutor = WaitableExecutor.useGlobalSharedThreadPool()

    /**
     * 需要处理的数据类型，有两种枚举类型
     * CLASSES 代表处理的 java 的 class 文件，RESOURCES 代表要处理 java 的资源
     * @return
     */
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    /**
     * 指 Transform 要操作内容的范围，官方文档 Scope 有 7 种类型：
     * 1. EXTERNAL_LIBRARIES        只有外部库
     * 2. PROJECT                   只有项目内容
     * 3. PROJECT_LOCAL_DEPS        只有项目的本地依赖(本地jar)
     * 4. PROVIDED_ONLY             只提供本地或远程依赖项
     * 5. SUB_PROJECTS              只有子项目。
     * 6. SUB_PROJECTS_LOCAL_DEPS   只有子项目的本地依赖项(本地jar)。
     * 7. TESTED_CODE               由当前变量(包括依赖项)测试的代码
     * @return
     */
    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    /**
     * 是否是增量编译
     */
    @Override
    boolean isIncremental() {
        return true
    }

    /**
     * 字节码转换的主要逻辑
     */
    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException,
            InterruptedException, IOException {
        onBeforeTransform()
        _transform(transformInvocation.context, transformInvocation.inputs,
                transformInvocation.outputProvider, transformInvocation.incremental)
        onAfterTransform()
    }

    void _transform(Context context, Collection<TransformInput> inputs, TransformOutputProvider outputProvider,
                    boolean isIncremental) throws IOException, TransformException, InterruptedException {

        if (!isModifyEnable()) {
            return
        }
        if (!isIncremental) {
            outputProvider.deleteAll()
        }

        /**Transform 的 inputs 有两种类型，一种是目录，一种是 jar 包，要分开遍历 */
        inputs.each { TransformInput input ->
            /**遍历目录*/
            input.directoryInputs.each { DirectoryInput directoryInput ->

                mWaitableExecutor.execute(new Callable<Object>() {
                    @Override
                    Object call() throws Exception {
                        processDirectoryInput(context, directoryInput, outputProvider, isIncremental)
                        return null
                    }
                })
            }

            /**遍历 jar*/
            input.jarInputs.each { JarInput jarInput ->

                mWaitableExecutor.execute(new Callable<Object>() {
                    @Override
                    Object call() throws Exception {
                        processJarInput(context, jarInput, outputProvider, isIncremental)
                        return null
                    }
                })
            }
        }
        mWaitableExecutor.waitForTasksWithQuickFail(true)
    }

    /**
     * 处理源码文件
     * 将修改过的字节码copy到dest,就可以实现编译期间干预字节码的目的
     */
    void processDirectoryInput(Context context, DirectoryInput directoryInput,
                               TransformOutputProvider outputProvider, boolean isIncremental) {
        //当前这个 Transform 输出目录
        File dest = outputProvider.getContentLocation(directoryInput.name,
                directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
        FileUtils.forceMkdir(dest)
        //当前这个 Transform 输入目录
        File dir = directoryInput.file

        if (isLogEnable()) {
            Log.i("BaseTransform", "> src.absolutePath: " + dir.absolutePath)
            Log.i("BaseTransform", "> dest.absolutePath: " + dest.absolutePath)
        }

        if (isIncremental) {
            String srcDirPath = dir.absolutePath
            String destDirPath = dest.absolutePath
            directoryInput.changedFiles.each {
                Map.Entry<File, Status> changedFile ->
                    Status status = changedFile.getValue()
                    File inputFile = changedFile.getKey()
                    String destFilePath = inputFile.absolutePath.replace(srcDirPath, destDirPath)
                    File destFile = new File(destFilePath)
                    switch (status) {
                        case Status.NOTCHANGED:
                            break
                        case Status.ADDED:
                        case Status.CHANGED:
                            FileUtils.touch(destFile)
                            transformSingleFile(context, dir, inputFile, destFile)
                            break
                        case Status.REMOVED:
                            if (destFile.exists()) {
                                FileUtils.forceDelete(destFile)
                            }
                            break
                    }
            }
        } else {
            transformDirectory(context, dir, dest)
        }

    }

    /**
     * 转换单个文件
     */
    void transformSingleFile(Context context, File dir, File inputFile, File destFile) {
        println("inputFile: " + inputFile.absolutePath)
        println("destFile: " + destFile.absolutePath)

        String className = inputFile.absolutePath.replace(dir.absolutePath + File.separator, "")
        if (className.endsWith(".class") && isShouldModify(className)) {
            File modified = modifyClassFile(dir, inputFile, context.getTemporaryDir())
            if (modified != null) {
                FileUtils.copyFile(modified, destFile)
                return
            }
        }
        FileUtils.copyFile(inputFile, destFile)
    }

    /**
     * 转换文件夹
     */
    void transformDirectory(Context context, File dir, File dest) {
        if (dir) {
            HashMap<String, File> modifyMap = new HashMap<>()
            /**遍历以某一扩展名结尾的文件*/
            dir.traverse(type: FileType.FILES, nameFilter: ~/.*\.class/) {
                File classFile ->
                    String className = classFile.absolutePath.replace(dir.absolutePath + File.separator, "")
                    if (isShouldModify(className)) {
                        File modified = modifyClassFile(dir, classFile, context.getTemporaryDir())
                        if (modified != null) {
                            /**key 为包名 + 类名，如：/cn/sensorsdata/autotrack/android/app/MainActivity.class*/
                            String ke = classFile.absolutePath.replace(dir.absolutePath, "")
                            modifyMap.put(ke, modified)
                        }
                    }
            }
            FileUtils.copyDirectory(dir, dest)
            modifyMap.entrySet().each {
                Map.Entry<String, File> en ->
                    File target = new File(dest.absolutePath + en.getKey())
                    if (target.exists()) {
                        target.delete()
                    }
                    FileUtils.copyFile(en.getValue(), target)
                    en.getValue().delete()
            }
        }
    }

    /**
     * 处理jar
     * 将修改过的字节码copy到dest,就可以实现编译期间干预字节码的目的
     */
    void processJarInput(Context context, JarInput jarInput,
                         TransformOutputProvider outputProvider, boolean isIncremental) {
        String destName = jarInput.file.name

        /**截取文件路径的 md5 值重命名输出文件,因为可能同名,会覆盖*/
        def hexName = DigestUtils.md5Hex(jarInput.file.absolutePath).substring(0, 8)
        /** 获取 jar 名字*/
        if (destName.endsWith(".jar")) {
            destName = destName.substring(0, destName.length() - 4)
        }
        /** 获得输出文件*/
        File dest = outputProvider.getContentLocation(destName + "_" + hexName,
                jarInput.contentTypes, jarInput.scopes, Format.JAR)

        if (isIncremental) {
            switch (jarInput.status) {
                case Status.NOTCHANGED:
                    break
                case Status.ADDED:
                case Status.CHANGED:
                    transformJar(context, jarInput.file, dest)
                    break
                case Status.REMOVED:
                    if (dest.exists()) {
                        FileUtils.forceDelete(dest)
                    }
                    break
            }
        } else {
            transformJar(context, jarInput.file, dest)
        }
    }

    /**
     *  转换Jar
     */
    void transformJar(Context context, File jarInputFile, File dest) {

        File modifiedJar = modifyJar(jarInputFile, context.getTemporaryDir(), true)
        if (modifiedJar == null) {
            modifiedJar = jarInputFile
        }
        FileUtils.copyFile(modifiedJar, dest)
    }


    File modifyClassFile(File dir, File classFile, File tempDir) {
        File modified = null
        try {
            String className = path2ClassName(classFile.absolutePath.replace(dir.absolutePath +
                    File.separator, ""))
            byte[] sourceClassBytes = IOUtils.toByteArray(new FileInputStream(classFile))
            byte[] modifiedClassBytes = modifyClass(sourceClassBytes)
            if (modifiedClassBytes) {
                modified = new File(tempDir, className.replace('.', '') +
                        '.class')
                if (modified.exists()) {
                    modified.delete()
                }
                modified.createNewFile()
                new FileOutputStream(modified).write(modifiedClassBytes)
            }
        } catch (Exception e) {
            e.printStackTrace()
            modified = classFile
        }
        return modified
    }

    String path2ClassName(String pathName) {
        pathName.replace(File.separator, ".").replace(".class", "")
    }

    File modifyJar(File jarFile, File tempDir, boolean nameHex) {
        /**
         * 读取原 jar
         */
        def file = new JarFile(jarFile, false)

        /**
         * 设置输出到的 jar
         */
        def hexName = ""
        if (nameHex) {
            hexName = DigestUtils.md5Hex(jarFile.absolutePath).substring(0, 8)
        }
        def outputJar = new File(tempDir, hexName + jarFile.name)
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(outputJar))
        Enumeration enumeration = file.entries()
        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) enumeration.nextElement()
            InputStream inputStream = null
            try {
                inputStream = file.getInputStream(jarEntry)
            } catch (Exception e) {
                return null
            }
            String entryName = jarEntry.getName()
            if (entryName.endsWith(".DSA") || entryName.endsWith(".SF")) {
                //ignore
            } else {
                JarEntry jarEntry2 = new JarEntry(entryName)
                jarOutputStream.putNextEntry(jarEntry2)

                byte[] modifiedClassBytes = null
                byte[] sourceClassBytes = IOUtils.toByteArray(inputStream)
                if (entryName.endsWith(".class")) {
                    if (isShouldModify(entryName)) {
                        modifiedClassBytes = modifyClass(sourceClassBytes)
                    }
                }
                if (modifiedClassBytes == null) {
                    modifiedClassBytes = sourceClassBytes
                }
                jarOutputStream.write(modifiedClassBytes)
                jarOutputStream.closeEntry()
            }
        }
        jarOutputStream.close()
        file.close()
        return outputJar
    }

    /**
     * 过滤需要修改的class文件
     * @param className eg: cn/sensorsdata/autotrack/android/app/MainActivity.class
     * @return
     */
    abstract boolean isShouldModify(String className);

    /**
     * 修改class文件
     * @param srcClass 源class
     * @return 目标class* @throws IOException
     */
    abstract byte[] modifyClass(byte[] srcClass) throws IOException;

    /**
     * 字节码修改是否开启，默认开启
     * @return
     */
    boolean isModifyEnable() {
        return true
    }

    boolean isLogEnable() {
        return true
    }

    void onBeforeTransform() {
        if (isLogEnable()) {
            Log.i("BaseTransform", "---------- ${getName()} 开始 ----------")
        }
    };

    void onAfterTransform() {
        if (isLogEnable()) {
            Log.i("BaseTransform", "---------- ${getName()} 结束 ----------")
        }
    };

}