package com.campus.community;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan({
        "com.campus.community.user.mapper",
        "com.campus.community.category.mapper",
        "com.campus.community.post.mapper",
        "com.campus.community.comment.mapper",
        "com.campus.community.file.mapper",
        "com.campus.community.report.mapper"
})
@SpringBootApplication
public class CommunityApplication {
    public static void main(String[] args) {
        SpringApplication.run(CommunityApplication.class, args);
    }
}
