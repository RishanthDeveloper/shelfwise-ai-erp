package com.shelfwise.dto;

import com.shelfwise.model.User.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {

    private boolean success;
    private String token;
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private Role role;
    private String avatarUrl;
    private String message;
}
