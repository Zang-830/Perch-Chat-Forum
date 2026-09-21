package com.campus.community.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.community.admin.dto.AdminPostView;
import com.campus.community.admin.dto.AdminReportView;
import com.campus.community.admin.dto.AdminUserView;
import com.campus.community.admin.dto.DashboardStats;
import com.campus.community.category.entity.Category;
import com.campus.community.category.mapper.CategoryMapper;
import com.campus.community.comment.mapper.CommentMapper;
import com.campus.community.common.BizException;
import com.campus.community.common.PageResult;
import com.campus.community.file.entity.UploadedFile;
import com.campus.community.file.mapper.UploadedFileMapper;
import com.campus.community.post.entity.Post;
import com.campus.community.post.entity.PostImage;
import com.campus.community.post.mapper.PostImageMapper;
import com.campus.community.post.mapper.PostMapper;
import com.campus.community.report.entity.Report;
import com.campus.community.report.mapper.ReportMapper;
import com.campus.community.user.entity.User;
import com.campus.community.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AdminService {
    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final PostImageMapper postImageMapper;
    private final CategoryMapper categoryMapper;
    private final CommentMapper commentMapper;
    private final UploadedFileMapper fileMapper;
    private final ReportMapper reportMapper;

    public AdminService(UserMapper userMapper, PostMapper postMapper, PostImageMapper postImageMapper,
                        CategoryMapper categoryMapper, CommentMapper commentMapper,
                        UploadedFileMapper fileMapper, ReportMapper reportMapper) {
        this.userMapper = userMapper;
        this.postMapper = postMapper;
        this.postImageMapper = postImageMapper;
        this.categoryMapper = categoryMapper;
        this.commentMapper = commentMapper;
        this.fileMapper = fileMapper;
        this.reportMapper = reportMapper;
    }

    public DashboardStats dashboard() {
        return new DashboardStats(
                userMapper.selectCount(null),
                userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getStatus, "ACTIVE")),
                userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getStatus, "BANNED")),
                postMapper.selectCount(null),
                postMapper.selectCount(Wrappers.<Post>lambdaQuery().eq(Post::getStatus, "PUBLISHED")),
                postMapper.selectCount(Wrappers.<Post>lambdaQuery().eq(Post::getStatus, "HIDDEN")),
                commentMapper.selectCount(null),
                fileMapper.selectCount(Wrappers.<UploadedFile>lambdaQuery().eq(UploadedFile::getStatus, "ACTIVE")),
                reportMapper.selectCount(Wrappers.<Report>lambdaQuery().eq(Report::getStatus, "PENDING"))
        );
    }

    public PageResult<AdminUserView> users(String keyword, String status, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        LambdaQueryWrapper<User> query = Wrappers.lambdaQuery();
        if (keyword != null && !keyword.isBlank()) {
            String term = keyword.trim();
            query.and(wrapper -> wrapper.like(User::getUsername, term).or().like(User::getNickname, term));
        }
        if (status != null && !status.isBlank()) {
            query.eq(User::getStatus, status.trim().toUpperCase(Locale.ROOT));
        }
        long total = userMapper.selectCount(query);
        query.orderByDesc(User::getCreatedAt)
                .last("LIMIT " + safeSize + " OFFSET " + ((safePage - 1) * safeSize));
        List<AdminUserView> items = userMapper.selectList(query).stream()
                .map(user -> new AdminUserView(user.getId(), user.getUsername(), user.getNickname(),
                        user.getRole(), user.getStatus(), user.getCreatedAt()))
                .toList();
        return new PageResult<>(items, total, safePage, safeSize);
    }

    public PageResult<AdminPostView> posts(String keyword, String status, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        LambdaQueryWrapper<Post> query = Wrappers.lambdaQuery();
        if (keyword != null && !keyword.isBlank()) {
            query.like(Post::getTitle, keyword.trim());
        }
        if (status != null && !status.isBlank()) {
            query.eq(Post::getStatus, status.trim().toUpperCase(Locale.ROOT));
        }
        long total = postMapper.selectCount(query);
        query.orderByDesc(Post::getCreatedAt)
                .last("LIMIT " + safeSize + " OFFSET " + ((safePage - 1) * safeSize));
        List<AdminPostView> items = postMapper.selectList(query).stream().map(this::toAdminPost).toList();
        return new PageResult<>(items, total, safePage, safeSize);
    }

    public PageResult<AdminReportView> reports(String status, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        LambdaQueryWrapper<Report> query = Wrappers.lambdaQuery();
        if (status != null && !status.isBlank()) {
            query.eq(Report::getStatus, status.trim().toUpperCase(Locale.ROOT));
        }
        long total = reportMapper.selectCount(query);
        query.orderByDesc(Report::getCreatedAt)
                .last("LIMIT " + safeSize + " OFFSET " + ((safePage - 1) * safeSize));
        List<AdminReportView> items = reportMapper.selectList(query).stream().map(this::toAdminReport).toList();
        return new PageResult<>(items, total, safePage, safeSize);
    }

    @Transactional
    public void updateUserStatus(Long userId, String requestedStatus, Long operatorId) {
        String status = requestedStatus.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ACTIVE", "BANNED").contains(status)) {
            throw new BizException("用户状态仅支持 ACTIVE 或 BANNED");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        if (userId.equals(operatorId)) {
            throw new BizException("不能修改自己的账号状态");
        }
        if ("ADMIN".equals(user.getRole()) && "BANNED".equals(status)) {
            throw new BizException("不能封禁管理员账号");
        }
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Transactional
    public void updatePostStatus(Long postId, String requestedStatus) {
        String status = requestedStatus.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("PUBLISHED", "HIDDEN").contains(status)) {
            throw new BizException("帖子状态仅支持 PUBLISHED 或 HIDDEN");
        }
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BizException(404, "帖子不存在");
        }
        post.setStatus(status);
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.updateById(post);
    }

    @Transactional
    public void resolveReport(Long reportId, String requestedStatus, String note, Long operatorId) {
        String status = requestedStatus.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("RESOLVED", "REJECTED").contains(status)) {
            throw new BizException("举报处理状态仅支持 RESOLVED 或 REJECTED");
        }
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BizException(404, "举报记录不存在");
        }
        if (!"PENDING".equals(report.getStatus())) {
            throw new BizException("该举报已经处理完成");
        }
        report.setStatus(status);
        report.setHandledBy(operatorId);
        report.setHandleNote(note == null ? null : note.trim());
        report.setHandledAt(LocalDateTime.now());
        reportMapper.updateById(report);
    }

    private AdminPostView toAdminPost(Post post) {
        User user = userMapper.selectById(post.getUserId());
        Category category = post.getCategoryId() == null ? null : categoryMapper.selectById(post.getCategoryId());
        long imageCount = postImageMapper.selectCount(Wrappers.<PostImage>lambdaQuery()
                .eq(PostImage::getPostId, post.getId()));
        return new AdminPostView(post.getId(), post.getTitle(),
                user == null ? "未知用户" : user.getNickname(),
                category == null ? "未分类" : category.getName(), post.getStatus(),
                post.getLikeCount(), post.getCommentCount(), imageCount, post.getCreatedAt());
    }

    private AdminReportView toAdminReport(Report report) {
        User reporter = userMapper.selectById(report.getReporterId());
        String targetTitle = null;
        if ("POST".equals(report.getTargetType())) {
            Post post = postMapper.selectById(report.getTargetId());
            targetTitle = post == null ? "内容已不存在" : post.getTitle();
        }
        return new AdminReportView(report.getId(), report.getTargetType(), report.getTargetId(), targetTitle,
                reporter == null ? "未知用户" : reporter.getNickname(), report.getReason(), report.getStatus(),
                report.getHandleNote(), report.getCreatedAt(), report.getHandledAt());
    }
}

