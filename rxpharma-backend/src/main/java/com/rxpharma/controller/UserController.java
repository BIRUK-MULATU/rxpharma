package com.rxpharma.controller;

import com.rxpharma.dto.request.ChangePasswordRequest;
import com.rxpharma.dto.request.UpdateRoleRequest;
import com.rxpharma.entity.User;
import com.rxpharma.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // NEW — must be declared BEFORE /{id} to avoid path conflicts
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getPendingUsers() {
        return ResponseEntity.ok(userService.getPendingUsers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> updateUser(@PathVariable Long id,
                                           @RequestParam String fullName,
                                           @RequestParam String email,
                                           @RequestParam User.Role role) {
        return ResponseEntity.ok(userService.updateUser(id, fullName, email, role));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/change-password")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','CASHIER','SUPPLIER_MANAGER')")
    public ResponseEntity<Map<String, String>> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(
                id,
                request.getCurrentPassword(),
                request.getNewPassword(),
                request.getConfirmPassword()
        );
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> adminResetPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        userService.adminResetPassword(id, body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request) {
        User updated = userService.updateRole(id, request.getRole());
        return ResponseEntity.ok(Map.of(
                "message", "Role updated successfully",
                "userId", updated.getId().toString(),
                "newRole", updated.getRole().name()
        ));
    }

    // NEW — approve a pending Google sign-up
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> approveUser(@PathVariable Long id) {
        User updated = userService.approveUser(id);
        return ResponseEntity.ok(Map.of(
                "message", "User approved successfully",
                "userId", updated.getId().toString()
        ));
    }

    // NEW — deny a pending Google sign-up (removes the account)
    @DeleteMapping("/{id}/deny")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> denyUser(@PathVariable Long id) {
        userService.denyUser(id);
        return ResponseEntity.ok(Map.of(
                "message", "User registration denied and removed"
        ));
    }
}