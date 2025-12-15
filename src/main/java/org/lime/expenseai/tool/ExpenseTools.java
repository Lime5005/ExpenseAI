package org.lime.expenseai.tool;

import org.lime.expenseai.model.ExpenseDto;
import org.lime.expenseai.service.ExpenseService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.logging.Logger;

@Component
public class ExpenseTools {

    private static final Logger log = Logger.getLogger(ExpenseTools.class.getName());

    private final ExpenseService expenseService;

    public ExpenseTools(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @Tool(description = "Add an expense item with date, category, amount, and description")
    public ExpenseDto addExpense(LocalDate date, String category, double amount, String description) {
        ExpenseDto dto = new ExpenseDto(null, date, category, amount, description);
        expenseService.addExpense(dto);
        log.info("Tool addExpense called: " + dto);
        return dto;
    }

    @Tool(description = "List expenses for a specific date (yyyy-MM-dd)")
    public List<ExpenseDto> getExpensesByDate(String date) {
        log.info("Tool getExpensesByDate called: " + date);
        return expenseService.getByDate(LocalDate.parse(date));
    }

    @Tool(description = "List expenses for a specific month (yyyy-MM)")
    public List<ExpenseDto> getExpensesByMonth(String yearMonth) {
        log.info("Tool getExpensesByMonth called: " + yearMonth);
        return expenseService.getByMonth(YearMonth.parse(yearMonth));
    }
}
