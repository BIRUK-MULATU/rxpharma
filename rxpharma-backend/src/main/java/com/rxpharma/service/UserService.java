package com.rxpharma.service;

import com.rxpharma.entity.User;
import com.rxpharma.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    public User updateUser(Long id, String fullName, String email, User.Role role) {
        User user = getUserById(id);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(role);
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    public void changePassword(Long id, String currentPassword,
                               String newPassword, String confirmPassword) {
        User user = getUserById(id);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new com.rxpharma.exception.BadRequestException("Current password is incorrect");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new com.rxpharma.exception.BadRequestException("New password and confirm password do not match");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new com.rxpharma.exception.BadRequestException("New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void adminResetPassword(Long id, String newPassword) {
        User user = getUserById(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public User updateRole(Long id, User.Role role) {
        User user = getUserById(id);
        user.setRole(role);
        return userRepository.save(user);
    }

    // NEW — approve a pending Google sign-up
    public User approveUser(Long id) {
        User user = getUserById(id);
        user.setApproved(true);
        return userRepository.save(user);
    }

    // NEW — deny/reject a pending sign-up — removes the account entirely
    public void denyUser(Long id) {
        User user = getUserById(id);
        if (user.isApproved()) {
            throw new com.rxpharma.exception.BadRequestException(
                    "Cannot deny a user that is already approved");
        }
        userRepository.delete(user);
    }

    // NEW — list all users awaiting admin approval
    public List<User> getPendingUsers() {
        return userRepository.findByApprovedFalse();
    }
}