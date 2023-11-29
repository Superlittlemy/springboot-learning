package com.slm.easypoi.controller.controller;

import com.slm.easypoi.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/excel")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    /**
     * 导出
     */
    @GetMapping("export")
    public void export(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode("学生列表.xlsx", StandardCharsets.UTF_8));
        studentService.export(response);
    }

    /**
     * 导出
     */
    @PostMapping("import")
    public void importExcel(MultipartFile file) {
        studentService.importExcel(file);
    }

}
