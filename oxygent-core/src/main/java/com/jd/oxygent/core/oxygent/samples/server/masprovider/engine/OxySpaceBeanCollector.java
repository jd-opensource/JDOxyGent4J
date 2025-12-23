package com.jd.oxygent.core.oxygent.samples.server.masprovider.engine;

import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public final class OxySpaceBeanCollector {

    private static volatile OxySpaceBeanCollector instance;
    private final Map<String, List<BaseOxy>> oxySpaceBeans = new ConcurrentHashMap<>();
    @Getter
    private final Map<String, OxySpaceBean> oxySpaceBeansAnnotationMapping = new ConcurrentHashMap<>();
    private final Map<String, Method> oxySpaceMethods = new ConcurrentHashMap<>();
    @Getter
    private List<BaseOxy> defaultStartupOxySpaceBeans = null;
    public static String DEFAULT_STARTUP_OXYSPACE_BEAN_NAME = "system_default_oxy_space_bean_name";
    private String basePackage;
    private boolean initialized = false;
    private final Object initLock = new Object();
    private final boolean isDelayLoadOxyBeanSpace = true;

    private OxySpaceBeanCollector(String basePackage) {
        this.basePackage = basePackage;
    }

    public static OxySpaceBeanCollector getInstance() {
        return getInstance(Config.getApp().getScanOxygentPath());
    }

    public static OxySpaceBeanCollector getInstance(String basePackage) {
        if (instance == null) {
            synchronized (OxySpaceBeanCollector.class) {
                if (instance == null) {
                    instance = new OxySpaceBeanCollector(basePackage);
                    instance.init();
                }
            }
        }
        return instance;
    }

    public void init() {
        if (initialized) {
            return;
        }

        synchronized (initLock) {
            if (initialized) {
                return;
            }

            try {
                scanOxySpaceBeans();
                initialized = true;
                log.info("OxySpaceBeanCollector initialized with " + oxySpaceBeans.size() + " bean groups");
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize OxySpaceBeanCollector", e);
            }
        }
    }

    private void scanOxySpaceBeans() throws Exception {
        // Get all classes in the package
        Set<Class<?>> classes = getClassesWithOxySpaceBeanMethods(basePackage);

        for (Class<?> clazz : classes) {
            // Instantiate the class
            Object instance = createInstance(clazz);
            if (instance == null) {
                continue;
            }

            // Scan methods
            scanMethods(instance, clazz);
        }
    }

    /**************************************************************************
     *  Class of OxySpaceBean related methods collecting
     **************************************************************************/

    private Set<Class<?>> getClassesWithOxySpaceBeanMethods(String packageName) throws Exception {
        Set<Class<?>> classesWithOxySpaceBean = new HashSet<>();
        String path = packageName.replace('.', '/');

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(path);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if ("file".equals(resource.getProtocol())) {
                findClassesWithOxySpaceBean(new File(resource.getFile()), packageName, classesWithOxySpaceBean);
            } else if ("jar".equals(resource.getProtocol())) {
                // Handle classes in JAR files
                findClassesInJarWithOxySpaceBean(resource, packageName, classesWithOxySpaceBean);
            }
        }

        return classesWithOxySpaceBean;
    }


    private void findClassesWithOxySpaceBean(File directory, String packageName, Set<Class<?>> result) throws Exception {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // Find classes in subdirectories
                findClassesWithOxySpaceBean(file, packageName + "." + file.getName(), result);
            } else if (file.getName().endsWith(".class")) {
                // Check if the class has an OxySpaceBean methods
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                checkAndAddClassIfHasOxySpaceBean(className, result);
            }
        }
    }

    private void checkAndAddClassIfHasOxySpaceBean(String className, Set<Class<?>> result) {
        try {
            Class<?> clazz = Class.forName(className);

            // Check if the class has a public no-argument constructor
            if (!hasPublicNoArgConstructor(clazz)) {
                return;
            }

            // Check if the class has methods annotated with @OxySpaceBean
            if (hasOxySpaceBeanMethod(clazz)) {
                result.add(clazz);
            }

        } catch (Exception e) {
            // Ignore classes that cannot be loaded
            log.warn("Failed to load class: " + className + " - " + e.getMessage());
        } catch (NoClassDefFoundError e) {
            log.warn("Failed to load class: " + className + " - " + e.getMessage());
        }
    }

    private boolean hasPublicNoArgConstructor(Class<?> clazz) {
        try {
            return clazz.getDeclaredConstructor() != null;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private boolean hasOxySpaceBeanMethod(Class<?> clazz) {
        try {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(OxySpaceBean.class)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load method: " + clazz.getName() + " - " + e.getMessage());
        } catch (NoClassDefFoundError e) {
            log.warn("Failed to load class: " + clazz.getName() + " - " + e.getMessage());
        }
        return false;
    }

    private void findClassesInJarWithOxySpaceBean(URL jarUrl, String packageName, Set<Class<?>> result) {
        // Not implemented yet
        log.warn("JAR scanning not fully implemented for: " + jarUrl);
    }

    /**************************************************************************
     *  OxySpaceBean related methods handling
     **************************************************************************/

    private Object createInstance(Class<?> clazz) {
        try {
            // Check if there is a public no-argument constructor
            if (clazz.getDeclaredConstructors().length == 0 ||
                    Arrays.stream(clazz.getDeclaredConstructors())
                            .noneMatch(c -> c.getParameterCount() == 0)) {
                return null;
            }

            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.info("Failed to create instance of " + clazz.getName() + ": " + e.getMessage());
            return null;
        }
    }

    private void scanMethods(Object instance, Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {
            OxySpaceBean annotation = method.getAnnotation(OxySpaceBean.class);
            if (annotation == null || !annotation.enabled()) {
                continue;
            }

            processOxySpaceMethod(instance, method, annotation);
        }
    }

    private void processOxySpaceMethod(Object instance, Method method, OxySpaceBean annotation) {
        String methodKey = instance.getClass().getSimpleName() + "." + method.getName();
        String beanName = getBeanName(annotation, methodKey);
        Object result = null;
        if (isDelayLoadOxyBeanSpace == false) {
            try {
                // Set method accessible
                method.setAccessible(true);

                // Execute method to get return value
                result = method.invoke(instance);

            } catch (Exception e) {
                log.error("Failed to execute OxySpaceBean method: " + methodKey + " - " + e.getMessage());
            }
            if (result != null) {
                processMethodResult(result, beanName, annotation);
                log.info("Processed OxySpaceBean method: " + methodKey);
            }
        }
        oxySpaceBeansAnnotationMapping.put(beanName, annotation);
        oxySpaceMethods.put(beanName, method);

    }

    private void processMethodResult(Object result, String beanName, OxySpaceBean annotation) {
        if (result instanceof List<?>) {
            List<BaseOxy> list = null;
            try {
                list = (List<BaseOxy>) result;
            } catch (ClassCastException e) {
                log.error("Failed to cast OxySpaceBean method result to List<BaseOxy>: " + e.getMessage());
                return;
            }

            if (oxySpaceBeans.containsKey(beanName)) {
                throw new RuntimeException(String.format("OxySpaceBean name must unique, because \"%s\" already exists. please check all of the @OxySpaceBean's name setting", beanName));
            }

            List<BaseOxy> beanList = oxySpaceBeans.computeIfAbsent(beanName, k -> new ArrayList<BaseOxy>());
            beanList.addAll(list);

            if (annotation.defaultStart()) {
                if (defaultStartupOxySpaceBeans == null) {
                    defaultStartupOxySpaceBeans = list;
                    oxySpaceBeans.put(beanName, defaultStartupOxySpaceBeans);
                    // Global should have only one default oxy space name
                    if (DEFAULT_STARTUP_OXYSPACE_BEAN_NAME == null) {
                        DEFAULT_STARTUP_OXYSPACE_BEAN_NAME = beanName;
                    }
                } else {
                    log.error("Default startup OxySpaceBean already exists. please check all of the @OxySpaceBean's name setting");
//                    throw new RuntimeException("Default startup OxySpaceBean already exists.");
                }
            }

        } else if (result instanceof Map) {
            // Handle Map return value
            Map<String, List<BaseOxy>> map = null;
            try {
                map = (Map<String, List<BaseOxy>>) result;
            } catch (ClassCastException e) {
                log.error("Failed to cast OxySpaceBean method result to Map<String, List<BaseOxy>>: " + e.getMessage());
                return;
            }

            for (Map.Entry<String, List<BaseOxy>> entry : map.entrySet()) {
                String key = entry.getKey();
                List<BaseOxy> valueList = entry.getValue();

                List<BaseOxy> beanList = oxySpaceBeans.computeIfAbsent(key, k -> new ArrayList<>());
                beanList.addAll(valueList);
            }
        } else {
            log.error("OxySpaceBean method must return List<BaseOxy> or Map<String, List<BaseOxy>>, but got: " + result.getClass().getSimpleName());
        }
    }

    private String getBeanName(OxySpaceBean annotation, String methodKey) {
        if (!annotation.name().isEmpty()) {
            return annotation.name();
        } else if (!annotation.value().isEmpty()) {
            return annotation.value();
        }
        return methodKey;
    }


    public Map<String, List<BaseOxy>> getAllOxySpaceBeans() {
        checkInitialized();
        return Collections.unmodifiableMap(new HashMap<>(oxySpaceBeans));
    }

    public List<BaseOxy> getOxySpaceBeans(String name) {
        checkInitialized();
        if (isDelayLoadOxyBeanSpace) {
            try {
                Method method = oxySpaceMethods.get(name);
                if (method != null) {
                    method.setAccessible(true);
                    Object result = method.invoke(instance);
                    processMethodResult(result, name, oxySpaceBeansAnnotationMapping.get(name));
                }
            } catch (Exception e) {
                log.error("get OxySpaceBean install by this name: " + name + " fail-> " + e.getMessage());
            }
        }
        return Collections.unmodifiableList(oxySpaceBeans.getOrDefault(name, new ArrayList<>()));
    }

    public Set<String> getOxySpaceBeanNames() {
        checkInitialized();
        return Collections.unmodifiableSet(new HashSet<>(oxySpaceBeans.keySet()));
    }

    private void checkInitialized() {
        if (!initialized) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (!initialized) {
                throw new IllegalStateException("OxySpaceBeanCollector not initialized. Call init() first.");
            }
        }
    }

    public void refresh() {
        synchronized (initLock) {
            oxySpaceBeans.clear();
            oxySpaceMethods.clear();
            initialized = false;
            init();
        }
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("initialized", initialized);
        status.put("beanGroups", oxySpaceBeans.size());
        status.put("totalBeans", oxySpaceBeans.values().stream().mapToInt(List::size).sum());
        status.put("methodsProcessed", oxySpaceMethods.size());
        return status;
    }


    public static void main(String[] args) {
        // Create collector
        OxySpaceBeanCollector collector = OxySpaceBeanCollector.getInstance();
        collector.init();

        // Get status
        System.out.println("\n=== Collector Status ===");
        Map<String, Object> status = collector.getStatus();
        status.forEach((key, value) -> System.out.println(key + ": " + value));

        // Get all Bean groups
        System.out.println("\n=== Bean OxySpace ===");
        Set<String> beanNames = collector.getOxySpaceBeanNames();
        beanNames.forEach(name -> {
            List<BaseOxy> beans = collector.getOxySpaceBeans(name);
            System.out.println(name + ": " + beans.size() + " items");
        });
        System.out.println("\n=== get defaultStartupOxySpaceBeans ===");
        System.out.println(collector.getDefaultStartupOxySpaceBeans());

    }
}