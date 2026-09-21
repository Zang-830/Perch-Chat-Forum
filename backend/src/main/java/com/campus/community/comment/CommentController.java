package com.campus.community.comment;

import com.campus.community.comment.dto.CommentView;
import com.campus.community.comment.dto.CreateCommentRequest;
import com.campus.community.common.ApiResponse;
import com.campus.community.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public ApiResponse<List<CommentView>> list(@PathVariable Long postId) {
        return ApiResponse.success(commentService.list(postId));
    }

    @PostMapping
    public ApiResponse<CommentView> create(@PathVariable Long postId,
                                           @Valid @RequestBody CreateCommentRequest request) {
        return ApiResponse.success(commentService.create(postId, CurrentUser.id(), request));
    }
}

