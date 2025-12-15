package org.lime.expenseai.model;

import java.util.Map;

public record MonthlyTotalsResult(Map<String, Double> totals, double totalAmount) {
}
