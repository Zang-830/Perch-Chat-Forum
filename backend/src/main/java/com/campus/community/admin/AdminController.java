package com.campus.community.admin;

import com.campus.community.admin.dto.AdminPostView;
import com.campus.community.admin.dto.AdminReportView;
import com.campus.community.admin.dto.AdminUserView;
import com.campus.community.admin.dto.DashboardStats;
import com.campus.community.admin.dto.ResolveReportRequest;
import com.campus.community.admin.dto.UpdateStatusRequest;
import com.campus.community.common.ApiResponse;
import com.campus.community.common.PageResult;
import com.campus.community.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<DashboardStats> dashboard() {
        return ApiResponse.success(adminService.dashboard());
    }

    @GetMapping("/users")
    public ApiResponse<PageResult<AdminUserView>> users(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminService.users(keyword, status, page, size));
    }

    @PutMapping("/users/{id}/status")
    public ApiResponse<Void> updateUserStatus(@PathVariable Long id,
                                              @Valid @RequestBody UpdateStatusRequest request) {
        adminService.updateUserStatus(id, request.status(), CurrentUser.id());
        return ApiResponse.success();
    }

    @GetMapping("/posts")
    public ApiResponse<PageResult<AdminPostView>> posts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminService.posts(keyword, status, page, size));
    }

    @PutMapping("/posts/{id}/status")
    public ApiResponse<Void> updatePostStatus(@PathVariable Long id,
                                              @Valid @RequestBody UpdateStatusRequest request) {
        adminService.updatePostStatus(id, request.status());
        return ApiResponse.success();
    }

    @GetMapping("/reports")
    public ApiResponse<PageResult<AdminReportView>> reports(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminService.reports(status, page, size));
    }

    @PutMapping("/reports/{id}")
    public ApiResponse<Void> resolveReport(@PathVariable Long id,
                                           @Valid @RequestBody ResolveReportRequest request) {
        adminService.resolveReport(id, request.status(), request.note(), CurrentUser.id());
        return ApiResponse.success();
    }
}

