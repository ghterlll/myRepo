// src/main/java/com/example/demo2/dto/UserProfileUpdateReq.java
package com.mobile.aura.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateReq {
    private String avatarUrl;
    private String nickname;

    /** Choose one: gender or genderCode */
    @Pattern(regexp="male|female|other", message="gender invalid")
    private String gender;       // Optional: male/female/other

    @Min(0)
    @Max(2)
    private Integer genderCode;  // Optional: 0=female, 1=male, 2=other

    private String birthday;     // yyyy-MM-dd
    private Integer heightCm;
    private Double  initialWeightKg;    // Initial weight
    private LocalDate initialWeightAt;  // Initial weight date

    private Double  latestWeightKg;
    private LocalDate latestWeightAt;   // Latest weight date
    private Double targetWeightKg;
    private String targetDeadline; // yyyy-MM-dd
    private Integer age;
    private String  location;
    private String  deviceType;
    private String  interests;
}