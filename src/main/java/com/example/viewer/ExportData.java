package com.example.viewer;

import com.example.viewer.entity.Post;
import com.example.viewer.mapper.PostMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 导出数据为 JSON 文件，用于 GitHub Pages 静态部署
 */
public class ExportData {

    public static void main(String[] args) throws IOException {
        ConfigurableApplicationContext context = SpringApplication.run(GshmdApplication.class, args);
        PostMapper mapper = context.getBean(PostMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 导出主贴列表
        List<Post> rootPosts = mapper.listRoot();
        System.out.println("导出主贴数量：" + rootPosts.size());

        // 导出所有数据
        Map<String, Object> data = new HashMap<>();
        data.put("posts", rootPosts);

        // 导出每个主贴的评论
        Map<String, List<Post>> commentsMap = new HashMap<>();
        for (Post post : rootPosts) {
            List<Post> comments = mapper.listComments(post.getId());
            commentsMap.put(post.getId(), comments);
        }
        data.put("comments", commentsMap);

        // 写入文件
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        Files.createDirectories(Paths.get("src/main/resources/static"));
        Files.write(Paths.get("src/main/resources/static/data.json"), json.getBytes());

        System.out.println("数据已导出到 src/main/resources/static/data.json");
        SpringApplication.exit(context);
    }
}
