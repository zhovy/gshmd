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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 导出数据为 JSON 文件，用于 GitHub Pages 静态部署
 * 使用多线程并行导出评论数据
 * 分页导出以支持移动端快速加载
 */
public class ExportData {

    private static final int POSTS_PER_PAGE = 500; // 每页帖子数量

    public static void main(String[] args) throws IOException, InterruptedException {
        ConfigurableApplicationContext context = SpringApplication.run(GshmdApplication.class, args);
        
        // 确保应用启动完成
        Thread.sleep(1000);
        
        System.out.println("开始执行数据导出...");
        PostMapper mapper = context.getBean(PostMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();

        // 配置 Java 8 时间支持
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        objectMapper.registerModule(javaTimeModule);

        // 导出主贴列表
        System.out.println("正在查询主贴列表...");
        List<Post> rootPosts = mapper.listRoot();
        System.out.println("导出主贴数量：" + rootPosts.size());

        if (rootPosts.isEmpty()) {
            System.out.println("警告：没有查询到任何主贴数据！");
        }

        String outputDir = "src/main/resources/static";
        Files.createDirectories(Paths.get(outputDir, "comments"));

        // 1. 导出轻量级搜索索引（包含所有帖子和评论，用于搜索和列表展示）
        System.out.println("正在导出搜索索引...");
        List<Map<String, Object>> searchIndex = new ArrayList<>();
        for (Post post : rootPosts) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", post.getId());
            item.put("content", post.getContent());
            item.put("createdAt", post.getCreatedAt());
            
            // 添加评论内容到搜索索引
            List<Post> comments = mapper.listComments(post.getId());
            if (!comments.isEmpty()) {
                List<String> commentContents = comments.stream()
                        .map(Post::getContent)
                        .collect(Collectors.toList());
                item.put("comments", commentContents);
            }
            
            searchIndex.add(item);
        }

        String indexJson = objectMapper.writeValueAsString(searchIndex);
        Files.write(Paths.get(outputDir + "/search-index.json"), indexJson.getBytes());
        System.out.println("已导出搜索索引，大小：" + (indexJson.length() / 1024) + " KB");

        // 2. 分页导出完整帖子数据（用于详情页）
        int totalPages = (int) Math.ceil((double) rootPosts.size() / POSTS_PER_PAGE);
        List<Map<String, Object>> pageIndexes = new ArrayList<>();

        System.out.println("开始分页导出帖子，共 " + totalPages + " 页...");
        for (int page = 0; page < totalPages; page++) {
            int start = page * POSTS_PER_PAGE;
            int end = Math.min(start + POSTS_PER_PAGE, rootPosts.size());
            List<Post> pagePosts = rootPosts.subList(start, end);

            String json = objectMapper.writeValueAsString(pagePosts);
            String fileName = outputDir + "/posts-" + page + ".json";
            Files.write(Paths.get(fileName), json.getBytes());

            Map<String, Object> pageIndex = new HashMap<>();
            pageIndex.put("page", page);
            pageIndex.put("count", pagePosts.size());
            pageIndexes.add(pageIndex);

            System.out.println("已导出第 " + page + " 页，帖子数：" + pagePosts.size());
        }

        // 3. 并行导出评论
        int threadCount = Math.min(rootPosts.size(), 10);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        System.out.println("开始并行导出评论，使用 " + threadCount + " 个线程...");
        long startTime = System.currentTimeMillis();

        Map<String, Integer> commentIndexMap = Collections.synchronizedMap(new HashMap<>());
        List<Post> postsToExport = new ArrayList<>(rootPosts);

        List<CompletableFuture<Void>> futures = postsToExport.stream()
                .map(post -> CompletableFuture.runAsync(() -> {
                    List<Post> comments = mapper.listComments(post.getId());
                    if (!comments.isEmpty()) {
                        try {
                            String json = objectMapper.writeValueAsString(comments);
                            String safeId = post.getId().replace("/", "_");
                            String fileName = outputDir + "/comments/" + safeId + ".json";
                            Files.write(Paths.get(fileName), json.getBytes());
                            commentIndexMap.put(post.getId(), comments.size());
                            System.out.println("已导出评论 - 主贴 ID: " + post.getId().substring(0, 8) + "..., 评论数：" + comments.size());
                        } catch (IOException e) {
                            System.err.println("导出评论失败 - 主贴 ID: " + post.getId() + ", 错误：" + e.getMessage());
                        }
                    }
                }, executor))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);

        long endTime = System.currentTimeMillis();
        System.out.println("评论导出完成，耗时：" + (endTime - startTime) + "ms");

        // 4. 写入主索引文件
        Map<String, Object> index = new HashMap<>();
        index.put("totalPages", totalPages);
        index.put("totalPosts", rootPosts.size());
        index.put("postsPerPage", POSTS_PER_PAGE);
        index.put("pages", pageIndexes);
        index.put("comments", commentIndexMap);

        String mainIndexJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(index);
        Files.write(Paths.get(outputDir + "/index.json"), mainIndexJson.getBytes());

        System.out.println("数据已导出到 " + outputDir + "/");
        System.out.println("总页数：" + totalPages);
        System.out.println("有评论的主贴数：" + commentIndexMap.size());
        SpringApplication.exit(context);
    }
}
