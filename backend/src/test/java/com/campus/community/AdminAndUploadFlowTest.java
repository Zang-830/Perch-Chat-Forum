package com.campus.community;

import com.campus.community.user.entity.User;
import com.campus.community.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Base64;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAndUploadFlowTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    void uploadImagePublishAndModeratePost() throws Exception {
        JsonNode memberAuth = register("image_user", "图片同学");
        String memberToken = memberAuth.at("/data/token").asText();

        byte[] png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
        MockMultipartFile image = new MockMultipartFile("file", "campus.png", "image/png", png);
        String uploadResponse = mockMvc.perform(multipart("/api/files/images")
                        .file(image)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String imageUrl = objectMapper.readTree(uploadResponse).at("/data/url").asText();

        mockMvc.perform(get(imageUrl))
                .andExpect(status().isOk())
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertEquals(
                        "image/png", result.getResponse().getContentType()));

        String categoryResponse = mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long categoryId = objectMapper.readTree(categoryResponse).at("/data/0/id").asLong();
        String postPayload = objectMapper.writeValueAsString(Map.of(
                "categoryId", categoryId,
                "title", "带图片的校园动态",
                "content", "这是一条用于验证图片上传和内容审核流程的动态。",
                "imageUrls", new String[]{imageUrl}
        ));
        String postResponse = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageUrls[0]").value(imageUrl))
                .andReturn().getResponse().getContentAsString();
        long postId = objectMapper.readTree(postResponse).at("/data/id").asLong();

        JsonNode adminAuth = register("admin_test", "测试管理员");
        String adminToken = adminAuth.at("/data/token").asText();
        long adminId = adminAuth.at("/data/user/id").asLong();
        User admin = userMapper.selectById(adminId);
        admin.setRole("ADMIN");
        userMapper.updateById(admin);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalImages").isNumber());

        mockMvc.perform(put("/api/admin/posts/{id}/status", postId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"HIDDEN\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/{id}", postId))
                .andExpect(status().isNotFound());
    }

    private JsonNode register(String username, String nickname) throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "nickname", nickname,
                "password", "Campus123!"
        ));
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }
}
