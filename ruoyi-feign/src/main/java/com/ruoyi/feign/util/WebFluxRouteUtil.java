package com.ruoyi.feign.util;

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.aop.support.AopUtils;

/**
 * 修复后的WebFlux路由解析工具类（解决PathPattern类型匹配错误）
 *
 * @author Saltyfish
 */
public class WebFluxRouteUtil {

    // 静态ApplicationContext（确保线程安全）
    private static volatile ApplicationContext applicationContext;
    // 缓存路径匹配器和Controller方法映射（避免重复遍历，提升性能）
    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    // Spring官方路径解析器（支持标准的路径参数、通配符）
    private static final PathPatternParser PATH_PATTERN_PARSER = new PathPatternParser();

    /**
     * 私有构造函数，防止工具类被实例化
     */
    private WebFluxRouteUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 设置Spring上下文（确保只初始化一次）
     */
    public static void setApplicationContext(ApplicationContext context) {
        if (applicationContext == null) {
            synchronized (WebFluxRouteUtil.class) {
                if (applicationContext == null) {
                    applicationContext = context;
                    // 初始化时预加载所有Controller方法到缓存
                    preloadControllerMethods();
                }
            }
        }
    }

    /**
     * 从ServerWebExchange中获取HandlerMethod（修复核心）
     */
    public static Method getHandlerMethod(ServerWebExchange exchange) {
        try {
            if (applicationContext == null) {
                System.err.println("ApplicationContext未初始化，无法获取HandlerMethod");
                return null;
            }

            // 1. 标准化请求路径和HTTP方法
            String requestPath = normalizePath(exchange.getRequest().getPath().value());
            String httpMethod = exchange.getRequest().getMethod().name();
            String cacheKey = httpMethod + ":" + requestPath;

            // 2. 优先从缓存获取，缓存未命中则实时匹配
            Method handlerMethod = METHOD_CACHE.get(cacheKey);
            if (handlerMethod != null) {
                return handlerMethod;
            }

            // 3. 实时匹配（缓存未命中时）
            handlerMethod = findControllerMethod(requestPath, httpMethod);
            if (handlerMethod != null) {
                METHOD_CACHE.put(cacheKey, handlerMethod); // 加入缓存
            }
            return handlerMethod;

        } catch (Exception e) {
            System.err.println("获取HandlerMethod失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 预加载所有Controller方法到缓存（启动时执行，避免每次请求遍历）
     */
    private static void preloadControllerMethods() {
        try {
            // 获取所有Controller和RestController
            Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(Controller.class);
            Map<String, Object> restControllers = applicationContext.getBeansWithAnnotation(RestController.class);
            controllers.putAll(restControllers);

            for (Object controller : controllers.values()) {
                // 修复：获取原始类（避免CGLIB代理类）
                Class<?> controllerClass = AopUtils.getTargetClass(controller);
                // 获取类级别的RequestMapping路径
                String classBasePath = getClassRequestMappingPath(controllerClass);

                // 遍历所有方法
                for (Method method : controllerClass.getDeclaredMethods()) {
                    // 获取方法的映射信息（路径+HTTP方法）
                    List<MappingInfo> mappingInfos = getMethodMappingInfos(method, classBasePath);
                    for (MappingInfo info : mappingInfos) {
                        String cacheKey = info.httpMethod + ":" + info.path;
                        METHOD_CACHE.put(cacheKey, method);
                        // 额外缓存路径模式（用于路径参数场景）
                        METHOD_CACHE.put("PATTERN:" + info.path + ":" + info.httpMethod, method);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("预加载Controller方法失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 根据路径和HTTP方法查找Controller方法（兼容路径参数、通配符，修复类型匹配错误）
     */
    public static Method findControllerMethod(String requestPath, String httpMethod) {
        // 1. 先尝试精确匹配
        String exactKey = httpMethod + ":" + requestPath;
        if (METHOD_CACHE.containsKey(exactKey)) {
            return METHOD_CACHE.get(exactKey);
        }

        // 2. 处理路径参数/通配符匹配（如/user/{id}）
        // 转换请求路径为PathContainer（关键修复：匹配方法需要的正确类型）
        PathContainer requestPathContainer = PathContainer.parsePath(requestPath);

        for (Map.Entry<String, Method> entry : METHOD_CACHE.entrySet()) {
            String cacheKey = entry.getKey();
            if (cacheKey.startsWith("PATTERN:") && cacheKey.endsWith(":" + httpMethod)) {
                // 提取路径模式字符串（去掉PATTERN:前缀和HTTP方法后缀）
                String patternStr = cacheKey.replace("PATTERN:", "").replace(":" + httpMethod, "");
                // 解析路径模式为PathPattern
                PathPattern pattern = PATH_PATTERN_PARSER.parse(patternStr);
                // 关键修复：传入PathContainer类型的参数到matches方法
                if (pattern.matches(requestPathContainer)) {
                    return entry.getValue();
                }
            }
        }

        return null;
    }

    /**
     * 获取类级别的RequestMapping路径
     */
    private static String getClassRequestMappingPath(Class<?> controllerClass) {
        RequestMapping classMapping = AnnotationUtils.findAnnotation(controllerClass, RequestMapping.class);
        if (classMapping != null && classMapping.value().length > 0) {
            return normalizePath(classMapping.value()[0]);
        }
        return "";
    }

    /**
     * 获取方法的映射信息（路径+HTTP方法）
     */
    private static List<MappingInfo> getMethodMappingInfos(Method method, String classBasePath) {
        List<MappingInfo> infos = new ArrayList<>();

        // 处理各种请求映射注解
        processGetMapping(method, classBasePath, infos);
        processPostMapping(method, classBasePath, infos);
        processPutMapping(method, classBasePath, infos);
        processDeleteMapping(method, classBasePath, infos);
        processPatchMapping(method, classBasePath, infos);
        processRequestMapping(method, classBasePath, infos);

        return infos;
    }

    // ========== 各种Mapping注解的处理方法 ==========
    private static void processGetMapping(Method method, String classBasePath, List<MappingInfo> infos) {
        if (method.isAnnotationPresent(GetMapping.class)) {
            GetMapping mapping = method.getAnnotation(GetMapping.class);
            addMappingInfos(mapping.value(), new String[]{"GET"}, classBasePath, infos);
        }
    }

    private static void processPostMapping(Method method, String classBasePath, List<MappingInfo> infos) {
        if (method.isAnnotationPresent(PostMapping.class)) {
            PostMapping mapping = method.getAnnotation(PostMapping.class);
            addMappingInfos(mapping.value(), new String[]{"POST"}, classBasePath, infos);
        }
    }

    private static void processPutMapping(Method method, String classBasePath, List<MappingInfo> infos) {
        if (method.isAnnotationPresent(PutMapping.class)) {
            PutMapping mapping = method.getAnnotation(PutMapping.class);
            addMappingInfos(mapping.value(), new String[]{"PUT"}, classBasePath, infos);
        }
    }

    private static void processDeleteMapping(Method method, String classBasePath, List<MappingInfo> infos) {
        if (method.isAnnotationPresent(DeleteMapping.class)) {
            DeleteMapping mapping = method.getAnnotation(DeleteMapping.class);
            addMappingInfos(mapping.value(), new String[]{"DELETE"}, classBasePath, infos);
        }
    }

    private static void processPatchMapping(Method method, String classBasePath, List<MappingInfo> infos) {
        if (method.isAnnotationPresent(PatchMapping.class)) {
            PatchMapping mapping = method.getAnnotation(PatchMapping.class);
            addMappingInfos(mapping.value(), new String[]{"PATCH"}, classBasePath, infos);
        }
    }

    private static void processRequestMapping(Method method, String classBasePath, List<MappingInfo> infos) {
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            String[] httpMethods = mapping.method().length > 0 ?
                Arrays.stream(mapping.method()).map(Enum::name).toArray(String[]::new) :
                new String[]{"GET", "POST", "PUT", "DELETE", "PATCH"};
            addMappingInfos(mapping.value(), httpMethods, classBasePath, infos);
        }
    }

    /**
     * 添加映射信息到列表
     */
    private static void addMappingInfos(String[] methodPaths, String[] httpMethods, String classBasePath, List<MappingInfo> infos) {
        if (methodPaths == null || methodPaths.length == 0) {
            return;
        }

        for (String methodPath : methodPaths) {
            String fullPath = normalizePath(classBasePath + methodPath);
            for (String httpMethod : httpMethods) {
                infos.add(new MappingInfo(fullPath, httpMethod));
            }
        }
    }

    /**
     * 标准化路径（处理首尾斜杠、多斜杠）
     */
    private static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        // 替换多个斜杠为单个，去除末尾斜杠（保留根路径/）
        path = path.replaceAll("/+", "/");
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * 内部映射信息封装类（简化，移除无用的rawPattern字段）
     */
    private static class MappingInfo {
        String path;          // 标准化后的完整路径
        String httpMethod;    // HTTP方法（GET/POST等）

        public MappingInfo(String path, String httpMethod) {
            this.path = path;
            this.httpMethod = httpMethod;
        }
    }
}