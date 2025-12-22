package org.lime.expenseai.tool;

import org.lime.expenseai.model.Category;
import org.lime.expenseai.model.ExpenseDto;
import org.lime.expenseai.model.MonthlyTotalsResult;
import org.lime.expenseai.service.CategoryClassifier;
import org.lime.expenseai.service.ExpenseService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Component
public class ExpenseTools {

    private static final Logger log = Logger.getLogger(ExpenseTools.class.getName());

    private final ExpenseService expenseService;
    private final CategoryClassifier categoryClassifier;

    public ExpenseTools(ExpenseService expenseService, CategoryClassifier categoryClassifier) {
        this.expenseService = expenseService;
        this.categoryClassifier = categoryClassifier;
    }

    @Tool(description = "Add an expense item with date, category, amount, and description")
    public ExpenseDto addExpense(LocalDate date, String category, double amount, String description) {
        String normalizedCategory = normalizeCategory(category, description);
        ExpenseDto dto = new ExpenseDto(null, date, normalizedCategory, amount, description);
        expenseService.addExpense(dto);
        log.info("Tool addExpense called: " + dto);
        return dto;
    }

    @Tool(description = "Delete an expense item by id")
    public void deleteExpense(Long id) {
        log.info("Tool deleteExpense called: " + id);
        expenseService.deleteExpense(id);
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

    @Tool(description = "Correct an expense by id")
    public ExpenseDto updateExpense(Long id, LocalDate date, String category, Double amount, String description) {
        String normalizedCategory = (category == null) ? null : normalizeCategory(category, description);
        return expenseService.updateExpensePartial(id, date, normalizedCategory, amount, description);
    }

    @Tool(description = "Correct an expense by matching date and description (uses the most recent match if multiple)")
    public ExpenseDto updateExpenseByDateAndDescription(LocalDate date, String description, Double amount, String category) {
        String normalizedCategory = (category == null) ? null : normalizeCategory(category, description);
        ExpenseDto updated = expenseService.updateExpenseByDateAndDescription(date, description, amount, normalizedCategory);
        log.info("Tool updateExpenseByDateAndDescription called: " + updated);
        return updated;
    }

    @Tool(description = "Get monthly totals by category for a specific month (yyyy-MM)")
    public MonthlyTotalsResult getMonthlyTotals(String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        Map<String, Double> totals = expenseService.getByMonth(ym).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        exp -> normalizeCategory(exp.category(), exp.description()),
                        java.util.stream.Collectors.summingDouble(ExpenseDto::amount)
                ));
        double totalAmount = totals.values().stream().mapToDouble(Double::doubleValue).sum();
        MonthlyTotalsResult result = new MonthlyTotalsResult(totals, totalAmount);
        log.info("Tool getMonthlyTotals called: " + yearMonth + " -> " + result);
        return result;
    }

    @Tool(description = "Classify a category using embeddings when the category is ambiguous or unknown")
    public String classifyCategoryByEmbedding(String description) {
        String text = (description == null) ? "" : description.trim();
        String category = categoryClassifier.classify(text);
        log.info("Tool classifyCategoryByEmbedding called: " + text + " -> " + category);
        return category;
    }

    private String normalizeCategory(String category, String description) {
        // try enum first
        if (category != null) {
            try {
                return Category.valueOf(category.toUpperCase()).name();
            } catch (IllegalArgumentException ignored) {
                // fall back
            }
        }
        return categoryClassifier.classify(description != null ? description : category);
    }
}
