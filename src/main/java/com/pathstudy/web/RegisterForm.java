package com.pathstudy.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterForm {

    @NotBlank(message = "Vui lòng nhập họ tên")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự")
    private String password;
}
