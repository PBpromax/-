package com.campushub.profile.dto;

import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @Size(max = 64) String nickname,
        @Size(max = 32) String studentId,
        @Size(max = 32) String qq,
        @Size(max = 64) String wechat,
        @Size(max = 32) String phone,
        @Size(max = 64) String campus,
        @Size(max = 64) String college,
        @Size(max = 64) String major,
        @Size(max = 32) String grade,
        @Size(max = 255) String bio,
        Boolean contactVisible
) {
}
