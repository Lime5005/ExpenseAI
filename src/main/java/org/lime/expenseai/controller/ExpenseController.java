package org.lime.expenseai.controller;

import org.lime.expenseai.model.ExpenseDto;
import org.lime.expenseai.service.ExpenseService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {
    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping("/month/{yearMonth}")
    public List<ExpenseDto> getExpensesByMonth(@PathVariable @DateTimeFormat(pattern = "yyyy-MM")
                                               YearMonth yearMonth) {
        if (expenseService.getByMonth(yearMonth).isEmpty()) return List.of();
        return expenseService.getByMonth(yearMonth);
    }

    @GetMapping("/date/{date}")
    public java.util.List<ExpenseDto> getExpensesByDate(@PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                                  LocalDate date) {
        // if no result, return empty list
        if (expenseService.getByDate(date).isEmpty()) return List.of();
        return expenseService.getByDate(date);
    }

    @PostMapping
    @ResponseStatus(CREATED)
    public void addExpense(@RequestBody ExpenseDto expenseDto) {
        expenseService.addExpense(expenseDto);
    }

    @PutMapping("/{id}")
    public ExpenseDto updateExpense(@PathVariable Long id, @RequestBody ExpenseDto expenseDto) {
        return expenseService.updateExpense(id, expenseDto);
    }

    @GetMapping
    public List<ExpenseDto> getAllExpenses() {
        return expenseService.getAllExpenses();
    }

}
