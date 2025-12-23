package com.jd.oxygent.core.oxygent.samples.server.utils;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author OxyGent Team
 * @version 1.0.0
 * @description Create instances based on class names
 * @since 1.0.0
 */
public final class RecursivePackageInstantiator {
    private static final String BASE_PACKAGE = "com.jd.oxygent.core.oxygent.oxy";

    /**
     * Create instance based on class name (search all sub-packages)
     */
    public static BaseOxy createInstance(String className, Map<String, Object> params) {

        try {
            Class<?> clazz = findClassInPackage(className);
            if (clazz != null) {

                Object instance = null;
                try {
                    // First try no-argument constructor
                    instance = clazz.getDeclaredConstructor().newInstance();
                } catch (NoSuchMethodException nsme) {
                    try {
                        // 1. Get builder() method
                        Method builderMethod = clazz.getMethod("builder");

                        // 2. Get Builder instance
                        Object builderInstance = builderMethod.invoke(null);

                        // 3. Get build() method
                        Method buildMethod = builderInstance.getClass().getMethod("build");

                        // 4. Ensure method is accessible
                        if (!buildMethod.canAccess(builderInstance)) {
                            buildMethod.setAccessible(true);
                        }

                        // 5. Invoke build() method
                        instance = buildMethod.invoke(builderInstance);

                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create instance using builder", e);
                    }
                }
                Map<String, Method> methods = new java.util.HashMap<>();
                for (Method method : clazz.getMethods()) {
                    methods.put(method.getName(), method);
                }

                for (Map.Entry<String, Object> paramsEntry : params.entrySet()) {
                    String name = JsonUtils.toCamelCase(paramsEntry.getKey());
                    Object value = paramsEntry.getValue();
                    Method method = methods.get(propertyToSetter(name));
                    method.setAccessible(true);
                    method.invoke(instance, value);
                }
                return (BaseOxy) instance;
            }
            throw new ClassNotFoundException("Class not found: " + className + " in package " + BASE_PACKAGE + " and its subpackages");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance for: " + className, e);
        }
    }

    private static String propertyToSetter(String propertyName) {
        if (propertyName == null || propertyName.isEmpty()) {
            return propertyName;
        }
        if (propertyName.contains("is")) {
            return propertyName.replaceFirst("is", "set");
        }
        // Capitalize first letter and add "set" prefix
        return "set" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
    }

    /**
     * Find class in package and its sub-packages
     */
    private static Class<?> findClassInPackage(String className) {
        try {
            // First try direct loading (full package name + class name)
            String fullClassName = BASE_PACKAGE + "." + className;
            try {
                return Class.forName(fullClassName);
            } catch (ClassNotFoundException e) {
                // Continue searching sub-packages
            }

            // Recursively search sub-packages
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String packagePath = BASE_PACKAGE.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(packagePath);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if ("jar".equals(resource.getProtocol())) {
                    Class<?> clazz = searchInJar(resource, className, BASE_PACKAGE);
                    if (clazz != null) {
                        return clazz;
                    }
                } else {
                    File directory = new File(resource.getFile());
                    if (directory.exists()) {
                        Class<?> clazz = searchInDirectory(directory, className, BASE_PACKAGE);
                        if (clazz != null) {
                            return clazz;
                        }
                    }
                }
            }

            return null;
        } catch (Exception e) {
            throw new RuntimeException("Error searching for class: " + className, e);
        }
    }

    /**
     * search for class in directory
     */
    private static Class<?> searchInJar(URL jarUrl, String className, String basePackage) {
        try {
            String jarPath = jarUrl.getFile();

            // Extract JAR file path (remove "file:" and "!/..." parts)
            if (jarPath.startsWith("file:")) {
                jarPath = jarPath.substring(5);
            }
            int exclamationIndex = jarPath.indexOf("!");
            if (exclamationIndex != -1) {
                jarPath = jarPath.substring(0, exclamationIndex);
            }

            // URL decoding (handle special characters like spaces)
            jarPath = URLDecoder.decode(jarPath, StandardCharsets.UTF_8);

            try (JarFile jarFile = new JarFile(jarPath)) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    // Check if it's within the base package and is a .class file
                    if (entryName.startsWith(basePackage.replace('.', '/')) && entryName.endsWith(className + ".class")) {
                        // Convert to full class name
                        String fullClassName = entryName.replace('/', '.').substring(0, entryName.length() - 6); // 去掉 .class
                        try {
                            return Class.forName(fullClassName);
                        } catch (ClassNotFoundException ignored) {
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Recursively search for class in directory
     */
    private static Class<?> searchInDirectory(File directory, String className, String currentPackage) {
        if (!directory.exists()) {
            return null;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return null;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // Recursively search sub-directories
                String subPackage = currentPackage + "." + file.getName();
                Class<?> clazz = searchInDirectory(file, className, subPackage);
                if (clazz != null) {
                    return clazz;
                }
            } else if (file.getName().endsWith(".class")) {
                // Check class files
                String simpleClassName = file.getName().substring(0, file.getName().length() - 6);
                if (simpleClassName.equals(className)) {
                    try {
                        String fullClassName = currentPackage + "." + simpleClassName;
                        return Class.forName(fullClassName);
                    } catch (ClassNotFoundException e) {
                        // Continue searching
                    }
                }
            }
        }
        return null;
    }
}