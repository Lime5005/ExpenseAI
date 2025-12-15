package org.lime.expenseai.model;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public record MonthlySummary(
    YearMonth month,
    double total,
    Map<String, Double> byCategory,
    double averageDailySpend,
    double vsLastMonthPercent,
    List<ExpenseDto> topExpenses
) {
}
