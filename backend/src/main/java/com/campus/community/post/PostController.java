package com.campus.community.post;

import com.campus.community.common.ApiResponse;
import com.campus.community.common.PageResult;
import com.campus.community.post.dto.CreatePostRequest;
import com.campus.community.post.dto.LikeResult;
import com.campus.community.post.dto.PostView;
import com.campus.community.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public ApiResponse<PageResult<PostView>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(postService.list(keyword, categoryId, page, size, CurrentUser.optionalId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<PostView> detail(@PathVariable Long id) {
        return ApiResponse.success(postService.detail(id, CurrentUser.optionalId()));
    }

    @PostMapping
    public ApiResponse<PostView> create(@Valid @RequestBody CreatePostRequest request) {
        return ApiResponse.success(postService.create(request, CurrentUser.id()));
    }

    @PostMapping("/{id}/like")
    public ApiResponse<LikeResult> toggleLike(@PathVariable Long id) {
        return ApiResponse.success(postService.toggleLike(id, CurrentUser.id()));
    }
}

