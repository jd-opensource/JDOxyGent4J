package com.jd.oxygent.core.oxygent.mcpservers.engine;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Utility class for scanning and loading classes from a given package.
 * Uses reflection to find all classes in a package and its subpackages.
 */
@Slf4j
public class ClassScanner {

    /**
     * Finds all classes in the specified package and its subpackages.
     *
     * @param packageName The package name to scan (e.g., "com.example.myapp")
     * @return List of Class objects found in the package
     * @throws Exception if there's an error accessing the package resources
     */
    public static List<Class<?>> findClasses(String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(path);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            File directory = new File(resource.getFile());

            if (directory.exists()) {
                findClassesInDirectory(packageName, directory, classes);
            }
        }

        return classes;
    }

    /**
     * Recursively finds all classes in a directory and its subdirectories.
     *
     * @param packageName The base package name for the classes
     * @param directory The directory to scan
     * @param classes The list to add found classes to
     */
    private static void findClassesInDirectory(String packageName, File directory, List<Class<?>> classes) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                findClassesInDirectory(packageName + "." + file.getName(), file, classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    classes.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    log.warn("Could not load class: {}", className);
                }
            }
        }
    }
}