package com.leonsoft.api;


import com.leonsoft.models.Booking;
import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class ErrorController {


    @GetMapping("/error")
    public boolean errorGet() {
        log.info("/error/get/");
        return true;
    }

    @PostMapping("/error")
    public boolean errorPost() {
        log.info("/error/post/");
        return true;
    }


}
