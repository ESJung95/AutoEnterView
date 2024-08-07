package com.ctrls.auto_enter_view.dto.common;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemporaryPasswordDto {

  @NotBlank(message = "이메일은 필수로 입력되어야 합니다.")
  @Email(message = "이메일 형식이 올바르지 않습니다.")
  private String email;

  @NotBlank(message = "이름은 필수로 입력되어야 합니다.")
  @Pattern(regexp = "^.{2,20}$", message = "이름은 2글자 이상 20자 이하여야 합니다.")
  private String name;
}