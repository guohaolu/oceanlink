package org.example.controller;

import org.example.pojo.query.SkywalkingQuery;
import org.example.service.SkywalkingService;
import org.example.support.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author guohao.lu
 */
@Controller("/skywalking")
public class SkywalkingController {
    @Autowired
    private SkywalkingService skywalkingService;

    @PostMapping("/test1")
    public R<String> test1(@RequestBody SkywalkingQuery query) {
        skywalkingService.saveTpWarehouseAge();
        return R.ok("test1");
    }
}
