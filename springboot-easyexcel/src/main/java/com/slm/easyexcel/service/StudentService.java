package com.slm.easyexcel.service;

import com.slm.easyexcel.model.Student;
import com.slm.easyexcel.utils.ExcelUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.List;

@Slf4j
@Service
public class StudentService {

    @SneakyThrows
    public void export(HttpServletResponse response) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        ExcelUtil.export(response.getOutputStream(), Student.class, "学生列表", List.of(
                new Student("老王", 26, 1, sdf.parse("2023-11-27"), 170.5D, Boolean.TRUE)
        ));
    }

    @SneakyThrows
    public void importExcel(MultipartFile file) {
        List<Student> read = ExcelUtil.read(file, Student.class);
        read.forEach(s -> {
            log.info(s.toString());
        });
    }

}
