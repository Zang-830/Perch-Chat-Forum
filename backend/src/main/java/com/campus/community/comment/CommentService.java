package com.campus.community.comment;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.community.comment.dto.CommentView;
import com.campus.community.comment.dto.CreateCommentRequest;
import com.campus.community.comment.entity.Comment;
import com.campus.community.comment.mapper.CommentMapper;
import com.campus.community.post.PostService;
import com.campus.community.post.mapper.PostMapper;
import com.campus.community.user.dto.UserView;
import com.campus.community.user.entity.User;
import com.campus.community.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentService {
    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final PostService postService;
    private final UserMapper userMapper;

    public CommentService(CommentMapper commentMapper, PostMapper postMapper,
                          PostService postService, UserMapper userMapper) {
        this.commentMapper = commentMapper;
        this.postMapper = postMapper;
        this.postService = postService;
        this.userMapper = userMapper;
    }

    public List<CommentView> list(Long postId) {
        postService.requirePublished(postId);
        return commentMapper.selectList(Wrappers.<Comment>lambdaQuery()
                        .eq(Comment::getPostId, postId)
                        .eq(Comment::getStatus, "VISIBLE")
                        .orderByAsc(Comment::getCreatedAt))
                .stream().map(this::toView).toList();
    }

    @Transactional
    public CommentView create(Long postId, Long userId, CreateCommentRequest request) {
        postService.requirePublished(postId);
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(request.content().trim());
        comment.setStatus("VISIBLE");
        comment.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(comment);
        postMapper.incrementCommentCount(postId);
        return toView(comment);
    }

    private CommentView toView(Comment comment) {
        User user = userMapper.selectById(comment.getUserId());
        return new CommentView(comment.getId(), comment.getContent(), comment.getCreatedAt(), UserView.from(user));
    }
}

