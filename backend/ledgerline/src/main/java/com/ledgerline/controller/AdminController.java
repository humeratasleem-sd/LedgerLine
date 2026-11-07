package com.ledgerline.controller;

import java.util.List;

import com.ledgerline.dto.AccountPermissionRequest;
import com.ledgerline.dto.AccountResponse;
import com.ledgerline.dto.AccountStatusRequest;
import com.ledgerline.dto.AuthenticatedUserResponse;
import com.ledgerline.dto.CreateAccountRequest;
import com.ledgerline.dto.UserRoleRequest;
import com.ledgerline.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public List<AuthenticatedUserResponse> listUsers() {
        return adminService.listUsers();
    }

    @PatchMapping("/users/{userId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateUserStatus(
            Authentication authentication,
            @PathVariable long userId,
            @Valid @RequestBody AccountStatusRequest request) {
        adminService.updateUserStatus(authentication, userId, request);
    }

    @PatchMapping("/users/{userId}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateUserRole(
            Authentication authentication,
            @PathVariable long userId,
            @Valid @RequestBody UserRoleRequest request) {
        adminService.updateUserRole(authentication, userId, request);
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(
            Authentication authentication,
            @Valid @RequestBody CreateAccountRequest request) {
        return adminService.createAccount(authentication, request);
    }

    @PutMapping("/accounts/{accountId}/permissions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignAccountPermission(
            Authentication authentication,
            @PathVariable long accountId,
            @Valid @RequestBody AccountPermissionRequest request) {
        adminService.assignAccountPermission(authentication, accountId, request);
    }
}
