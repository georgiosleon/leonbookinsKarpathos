package com.leonsoft.endpoints.rest.json;


import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
