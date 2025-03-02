package tcc.com.viewer.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/module")
public class ModuleController {
    @PostMapping("/join")
    public void joinModules(@RequestParam String target, @RequestParam String source) {
        try {
            System.out.println(target + " " + source);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
