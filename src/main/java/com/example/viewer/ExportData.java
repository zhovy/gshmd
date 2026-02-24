package com.example.viewer;

import com.example.viewer.entity.Post;
import com.example.viewer.mapper.PostMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 导出数据为 JSON 文件，用于 GitHub Pages 静态部署
 * 使用多线程并行导出评论数据
 */
public class ExportData {

    public static void main(String[] args) throws IOException, InterruptedException {
        ConfigurableApplicationContext context = SpringApplication.run(GshmdApplication.class, args);
        PostMapper mapper = context.getBean(PostMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 配置 Java 8 时间支持
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        objectMapper.registerModule(javaTimeModule);

        // 导出主贴列表
        List<Post> rootPosts = mapper.listRoot();
        System.out.println("导出主贴数量：" + rootPosts.size());

        // 使用固定大小线程池并行导出评论
        int threadCount = Math.min(rootPosts.size(), 10); // 最多 10 个线程
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        System.out.println("开始并行导出评论，使用 " + threadCount + " 个线程...");
        long startTime = System.currentTimeMillis();

        // 使用 CompletableFuture 并行导出所有评论
        Map<String, List<Post>> commentsMap = rootPosts.stream()
                .map(post -> CompletableFuture.supplyAsync(() -> {
                    List<Post> comments = mapper.listComments(post.getId());
                    System.out.println("已导出评论 - 主贴 ID: " + post.getId().substring(0, 8) + "..., 评论数：" + comments.size());
                    return Map.entry(post.getId(), comments);
                }, executor))
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        futureList -> {
                            // 等待所有任务完成
                            CompletableFuture<Map.Entry<String, List<Post>>>[] futures = 
                                    futureList.toArray(new CompletableFuture[0]);
                            CompletableFuture.allOf(futures).join();
                            
                            // 收集结果
                            Map<String, List<Post>> result = new HashMap<>();
                            for (CompletableFuture<Map.Entry<String, List<Post>>> future : futureList) {
                                Map.Entry<String, List<Post>> entry = future.join();
                                result.put(entry.getKey(), entry.getValue());
                            }
                            return result;
                        }
                ));

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
        
        long endTime = System.currentTimeMillis();
        System.out.println("评论导出完成，耗时：" + (endTime - startTime) + "ms");

        // 导出所有数据
        Map<String, Object> data = new HashMap<>();
        data.put("posts", rootPosts);
        data.put("comments", commentsMap);

        // 写入文件
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        Files.createDirectories(Paths.get("src/main/resources/static"));
        Files.write(Paths.get("src/main/resources/static/data.json"), json.getBytes());

        System.out.println("数据已导出到 src/main/resources/static/data.json");
        System.out.println("文件大小：" + (json.length() / 1024) + " KB");
        SpringApplication.exit(context);
    }
}
