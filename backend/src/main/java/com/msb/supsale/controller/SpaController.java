package com.msb.supsale.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({"/customer", "/login", "/staff/**", "/admin/**", "/sale/**", "/cc"})
    public String forwardToFrontend() {
        return "forward:/index.html";
    }
}