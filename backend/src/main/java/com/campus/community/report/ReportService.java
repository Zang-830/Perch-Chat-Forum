package com.campus.community.report;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.community.common.BizException;
import com.campus.community.post.PostService;
import com.campus.community.report.dto.CreateReportRequest;
import com.campus.community.report.entity.Report;
import com.campus.community.report.mapper.ReportMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class ReportService {
    private final ReportMapper reportMapper;
    private final PostService postService;

    public ReportService(ReportMapper reportMapper, PostService postService) {
        this.reportMapper = reportMapper;
        this.postService = postService;
    }

    @Transactional
    public void create(CreateReportRequest request, Long reporterId) {
        String targetType = request.targetType().trim().toUpperCase(Locale.ROOT);
        if (!"POST".equals(targetType)) {
            throw new BizException("当前仅支持举报帖子");
        }
        postService.requirePublished(request.targetId());
        long duplicate = reportMapper.selectCount(Wrappers.<Report>lambdaQuery()
                .eq(Report::getReporterId, reporterId)
                .eq(Report::getTargetType, targetType)
                .eq(Report::getTargetId, request.targetId())
                .eq(Report::getStatus, "PENDING"));
        if (duplicate > 0) {
            throw new BizException("你已经举报过该内容，管理员正在处理中");
        }
        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(targetType);
        report.setTargetId(request.targetId());
        report.setReason(request.reason().trim());
        report.setStatus("PENDING");
        report.setCreatedAt(LocalDateTime.now());
        reportMapper.insert(report);
    }
}

