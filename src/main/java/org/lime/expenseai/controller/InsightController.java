package org.lime.expenseai.controller;

import org.lime.expenseai.model.Insight;
import org.lime.expenseai.service.InsightService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/insight")
public class InsightController {

    private final InsightService insightService;

    public InsightController(InsightService insightService) {
        this.insightService = insightService;
    }

    @GetMapping
    public Insight getInsight(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                              @RequestParam(name = "lang", required = false) String lang,
                              @RequestParam(name = "currency", required = false) String currency) {
        return insightService.analyze(month, lang, currency);
    }
}
