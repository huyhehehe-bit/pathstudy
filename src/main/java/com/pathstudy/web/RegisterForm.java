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

    /** "STUDENT" (mặc định) hoặc "TEACHER". */
    private String role = "STUDENT";

    /** Khối lớp học sinh chọn khi đăng ký ("Lớp 10/11/12"). Bắt buộc với học sinh,
     *  bỏ trống với giáo viên — kiểm tra ở controller theo role. */
    private String grade;

    /** Mã xác thực giáo viên (chỉ dùng khi role=TEACHER). */
    private String teacherCode;
}
