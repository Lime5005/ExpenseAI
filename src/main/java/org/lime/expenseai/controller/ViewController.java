package org.lime.expenseai.controller;

import org.lime.expenseai.model.ExpenseDto;
import org.lime.expenseai.service.ExpenseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
public class ViewController {

    private final ExpenseService expenseService;

    public ViewController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("expenses", expenseService.getAllExpenses());
        return "index";
    }

    @PostMapping("/ui/expenses")
    public String addExpense(@RequestParam LocalDate date,
                             @RequestParam String category,
                             @RequestParam double amount,
                             @RequestParam String description) {
        expenseService.addExpense(new ExpenseDto(null, date, category, amount, description));
        return "redirect:/";
    }
}
