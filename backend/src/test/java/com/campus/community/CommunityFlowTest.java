package com.campus.community;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommunityFlowTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerPublishAndReadPost() throws Exception {
        String registerBody = """
                {"username":"campus_user","nickname":"校园同学","password":"Campus123!"}
                """;

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        JsonNode registerJson = objectMapper.readTree(registerResponse);
        String token = registerJson.at("/data/token").asText();

        String categoryResponse = mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").exists())
                .andReturn().getResponse().getContentAsString();
        long categoryId = objectMapper.readTree(categoryResponse).at("/data/0/id").asLong();

        String postBody = """
                {"categoryId":%d,"title":"第一次在校园社区发帖","content":"这是用于验证完整发布流程的一段正文内容。"}
                """.formatted(categoryId);
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("第一次在校园社区发帖"));

        mockMvc.perform(get("/api/posts").param("keyword", "第一次在校园社区发帖"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.items[0].author.nickname").value("校园同学"));
    }
}
