package com.campus.community.report;

import com.campus.community.common.ApiResponse;
import com.campus.community.report.dto.CreateReportRequest;
import com.campus.community.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody CreateReportRequest request) {
        reportService.create(request, CurrentUser.id());
        return ApiResponse.success();
    }
}

