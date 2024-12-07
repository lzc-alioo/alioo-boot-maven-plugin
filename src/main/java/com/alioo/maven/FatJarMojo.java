package com.alioo.maven;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

@Mojo(name = "run", defaultPhase = LifecyclePhase.PACKAGE)
public class FatJarMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;


    public void execute() throws MojoExecutionException {
        try {
            getLog().info("Fat JAR(@alioo-boot-maven-plugin) project:" + project);
            getLog().info("Fat JAR(@alioo-boot-maven-plugin) project.getBuild():" + toString(project.getBuild()));
            getLog().info("Fat JAR(@alioo-boot-maven-plugin) project.getDependencies():" + project.getDependencies());

            String buildDirectory = project.getBuild().getDirectory();

            //创建plugin.properties
            File fatjarDir = new File(buildDirectory, "/" + project.getArtifactId());
            if (!fatjarDir.exists()) {
                fatjarDir.mkdirs();
            }

            File classesDir = new File(buildDirectory, "/classes");
            File metaInfDir = new File(buildDirectory, "/classes/META-INF");
            File libDir = new File(fatjarDir.getAbsolutePath(), "/lib");


            if (!libDir.exists()) {
                libDir.mkdirs();
            }
            for (Object artifactObj : project.getDependencyArtifacts()) {
                Artifact artifact = (Artifact) artifactObj;
                if (artifact.getScope() != null && artifact.getScope().equals("provided")) {
                    continue;
                }
                File sourceFile = new File(artifact.getFile().getAbsolutePath());
                //如果依赖的jar包是system类型，则直接将依赖的jar包添加到fatJar中的lib目录下
                getLog().info("Fat JAR(@alioo-boot-maven-plugin) dependencyArtifactPath =>" + sourceFile.getAbsolutePath());
                FileUtils.copyFile(sourceFile, new File(libDir, sourceFile.getName()));
            }

            //把classesDir复制到fatjarDir目录下,并且排除掉其中子目录/classes/META-INF
            FileUtils.copyDirectory(classesDir, new File(fatjarDir.getAbsolutePath() + "/classes"), filter -> !filter.getAbsolutePath().contains("/classes/META-INF"));
            FileUtils.copyDirectory(metaInfDir, new File(fatjarDir.getAbsolutePath() + "/META-INF"));

            //将fatjarDir目录下所有文件打包到一个新的jar包中
            File fatJar = new File(buildDirectory, project.getArtifactId() + ".jar");
            JarOutputStream jos = new JarOutputStream(new FileOutputStream(fatJar));
            addDirectory(jos, fatjarDir);
            jos.close();
//
//            // 创建目录 classes/META-INF/lib
//            mkdirDir(jos, "classes/");
//            mkdirDir(jos, "META-INF/");
//            mkdirDir(jos, "lib/");
//
//            // Add project classes
//            addClasses(jos, classesDir);
//            addMetaInf(jos, metaInfDir);
//            addJar(jos, libDir);
//
//            jos.close();

            //fatJar解压到当前目录

        } catch (Exception e) {
            throw new MojoExecutionException("Error assembling fat jar", e);
        }
    }

//    private void addClasses(JarOutputStream jos, File source) throws IOException {
//        if (source.isDirectory()) {
//            for (File file : FileUtils.listFiles(source, null, true)) {
//                String entryName = source.toURI().relativize(file.toURI()).getPath();
//                if (entryName.contains("META-INF")) {
//                    continue;
//                }
//                jos.putNextEntry(new JarEntry("classes/" + entryName));
//                FileUtils.copyFile(file, jos);
//                jos.closeEntry();
//            }
//        }
//    }

    private void addDirectory(JarOutputStream jos, File source) throws IOException {
        if (source.isDirectory()) {
            for (File file : FileUtils.listFiles(source, null, true)) {
                String entryName = source.toURI().relativize(file.toURI()).getPath();
                jos.putNextEntry(new JarEntry(entryName));
                FileUtils.copyFile(file, jos);
                jos.closeEntry();
            }
        }
    }

//    private void addJar(JarOutputStream jos, File source) throws IOException {
//        if (source.isDirectory()) {
//            for (File file : FileUtils.listFiles(source, null, true)) {
//                JarEntry jarEntry = new JarEntry("lib/" + file.getName());
//                jos.putNextEntry(jarEntry);
//                FileUtils.copyFile(file, jos);
//                jos.closeEntry();
//            }
//        }
//    }
//
//    private static void mkdirDir(JarOutputStream jos, String dir) throws IOException {
//        JarEntry libDirEntry = new JarEntry(dir);
//        jos.putNextEntry(libDirEntry);
//        jos.closeEntry();
//    }

    public static String toString(Build build) {
        //采用json的风格，输出入参对象的各个属性
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"directory\":\"").append(build.getDirectory()).append("\",");
        sb.append("\"sourceDirectory\":\"").append(build.getSourceDirectory()).append("\",");
        sb.append("\"testSourceDirectory\":\"").append(build.getTestSourceDirectory()).append("\",");
        sb.append("\"outputDirectory\":\"").append(build.getOutputDirectory()).append("\",");
        sb.append("\"testOutputDirectory\":\"").append(build.getTestOutputDirectory()).append("\",");
        sb.append("\"extensions\":\"").append(build.getExtensions()).append("\",");
        sb.append("\"defaultGoal\":\"").append(build.getDefaultGoal()).append("\",");
        sb.append("\"resources\":\"").append(build.getResources()).append("\",");
        sb.append("\"testResources\":\"").append(build.getTestResources()).append("\",");
        sb.append("\"finalName\":\"").append(build.getFinalName()).append("\",");
        sb.append("\"filters\":\"").append(build.getFilters()).append("\"");
        sb.append("}");

        return sb.toString();


    }
}
