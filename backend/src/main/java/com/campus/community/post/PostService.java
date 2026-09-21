package com.campus.community.post;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.community.category.dto.CategoryView;
import com.campus.community.category.entity.Category;
import com.campus.community.category.mapper.CategoryMapper;
import com.campus.community.common.BizException;
import com.campus.community.common.PageResult;
import com.campus.community.file.ImageUploadService;
import com.campus.community.file.entity.UploadedFile;
import com.campus.community.post.dto.CreatePostRequest;
import com.campus.community.post.dto.LikeResult;
import com.campus.community.post.dto.PostView;
import com.campus.community.post.entity.Post;
import com.campus.community.post.entity.PostImage;
import com.campus.community.post.entity.PostLike;
import com.campus.community.post.mapper.PostImageMapper;
import com.campus.community.post.mapper.PostLikeMapper;
import com.campus.community.post.mapper.PostMapper;
import com.campus.community.user.dto.UserView;
import com.campus.community.user.entity.User;
import com.campus.community.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PostService {
    private final PostMapper postMapper;
    private final PostLikeMapper postLikeMapper;
    private final PostImageMapper postImageMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final ImageUploadService imageUploadService;

    public PostService(PostMapper postMapper, PostLikeMapper postLikeMapper, PostImageMapper postImageMapper,
                       UserMapper userMapper, CategoryMapper categoryMapper,
                       ImageUploadService imageUploadService) {
        this.postMapper = postMapper;
        this.postLikeMapper = postLikeMapper;
        this.postImageMapper = postImageMapper;
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
        this.imageUploadService = imageUploadService;
    }

    public PageResult<PostView> list(String keyword, Long categoryId, int page, int size, Long currentUserId) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 30);
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        List<PostView> items = postMapper.selectFeed(normalizedKeyword, categoryId,
                        (safePage - 1) * safeSize, safeSize)
                .stream().map(post -> toView(post, currentUserId, false)).toList();
        long total = postMapper.countFeed(normalizedKeyword, categoryId);
        return new PageResult<>(items, total, safePage, safeSize);
    }

    public PostView detail(Long id, Long currentUserId) {
        Post post = requirePublished(id);
        return toView(post, currentUserId, true);
    }

    @Transactional
    public PostView create(CreatePostRequest request, Long userId) {
        Category category = categoryMapper.selectById(request.categoryId());
        if (category == null || !Boolean.TRUE.equals(category.getEnabled())) {
            throw new BizException("所选分类不可用");
        }
        List<UploadedFile> uploadedFiles = imageUploadService.requireOwnedFiles(request.imageUrls(), userId);
        LocalDateTime now = LocalDateTime.now();
        Post post = new Post();
        post.setUserId(userId);
        post.setCategoryId(category.getId());
        post.setTitle(request.title().trim());
        post.setContent(request.content().trim());
        post.setSummary(buildSummary(request.content()));
        post.setStatus("PUBLISHED");
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        postMapper.insert(post);
        for (int index = 0; index < uploadedFiles.size(); index++) {
            UploadedFile file = uploadedFiles.get(index);
            PostImage image = new PostImage();
            image.setPostId(post.getId());
            image.setFileId(file.getId());
            image.setImageUrl(file.getPublicUrl());
            image.setSortOrder(index);
            postImageMapper.insert(image);
        }
        return toView(post, userId, true);
    }

    @Transactional
    public LikeResult toggleLike(Long postId, Long userId) {
        requirePublished(postId);
        PostLike existing = postLikeMapper.selectOne(Wrappers.<PostLike>lambdaQuery()
                .eq(PostLike::getPostId, postId)
                .eq(PostLike::getUserId, userId));
        boolean liked;
        int delta;
        if (existing == null) {
            PostLike like = new PostLike();
            like.setPostId(postId);
            like.setUserId(userId);
            like.setCreatedAt(LocalDateTime.now());
            postLikeMapper.insert(like);
            liked = true;
            delta = 1;
        } else {
            postLikeMapper.deleteById(existing.getId());
            liked = false;
            delta = -1;
        }
        postMapper.changeLikeCount(postId, delta);
        Post refreshed = postMapper.selectById(postId);
        return new LikeResult(liked, Math.max(refreshed.getLikeCount(), 0));
    }

    public Post requirePublished(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null || !"PUBLISHED".equals(post.getStatus())) {
            throw new BizException(404, "帖子不存在");
        }
        return post;
    }

    private PostView toView(Post post, Long currentUserId, boolean includeContent) {
        User user = userMapper.selectById(post.getUserId());
        Category category = post.getCategoryId() == null ? null : categoryMapper.selectById(post.getCategoryId());
        boolean liked = currentUserId != null && postLikeMapper.selectCount(Wrappers.<PostLike>lambdaQuery()
                .eq(PostLike::getPostId, post.getId())
                .eq(PostLike::getUserId, currentUserId)) > 0;
        List<String> imageUrls = postImageMapper.selectList(Wrappers.<PostImage>lambdaQuery()
                        .eq(PostImage::getPostId, post.getId())
                        .orderByAsc(PostImage::getSortOrder))
                .stream().map(PostImage::getImageUrl).toList();
        return new PostView(
                post.getId(), post.getTitle(), post.getSummary(), includeContent ? post.getContent() : null,
                post.getLikeCount(), post.getCommentCount(), post.getCreatedAt(),
                UserView.from(user), category == null ? null : CategoryView.from(category), imageUrls, liked
        );
    }

    private String buildSummary(String content) {
        String compact = content
                .replaceAll("(?m)^#{1,6}\\s+", "")
                .replaceAll("```[\\s\\S]*?```", " [code] ")
                .replaceAll("!\\[([^]]*)]\\([^)]+\\)", "$1")
                .replaceAll("\\[([^]]+)]\\([^)]+\\)", "$1")
                .replaceAll("[*_~`>|-]", " ")
                .strip()
                .replaceAll("\\s+", " ");
        return compact.length() <= 120 ? compact : compact.substring(0, 120) + "…";
    }
}
