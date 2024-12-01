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

            File classesDir = new File(buildDirectory, "/classes");
            File libDir = new File(buildDirectory, "/lib");

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

            // Prepare output jar file name
            File fatJar = new File(buildDirectory, project.getBuild().getFinalName() + "-fat.jar");
            JarOutputStream jos = new JarOutputStream(new FileOutputStream(fatJar));

            // 创建 classes 目录条目
            addDir(jos, "classes/");
            // 创建 lib 目录条目
            addDir(jos, "lib/");


            // Add project classes
            addClasses(jos, classesDir, "");

            for (File dependencyJar : libDir.listFiles()) {
                addJar(jos, dependencyJar);
            }
            jos.close();
        } catch (Exception e) {
            throw new MojoExecutionException("Error assembling fat jar", e);
        }
    }

    private void addClasses(JarOutputStream jos, File source, String prefix) throws IOException {
        if (source.isDirectory()) {
            for (File file : FileUtils.listFiles(source, null, true)) {
                String entryName = prefix + source.toURI().relativize(file.toURI()).getPath();
                if (file.isFile()) {
                    jos.putNextEntry(new JarEntry("classes/" + entryName));
                    FileUtils.copyFile(file, jos);
                    jos.closeEntry();
                }
            }
        }
    }

    private void addJar(JarOutputStream jos, File jarFile) throws IOException {
        JarEntry jarEntry = new JarEntry("lib/" + jarFile.getName());
        jos.putNextEntry(jarEntry);
        FileUtils.copyFile(jarFile, jos);
        jos.closeEntry();
    }

    private static void addDir(JarOutputStream jos, String dir) throws IOException {
        JarEntry libDirEntry = new JarEntry(dir);
        jos.putNextEntry(libDirEntry);
        jos.closeEntry();
    }

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
