package org.lime.expenseai.model;

import java.time.LocalDate;

public record ExpenseDto(
    Long id,
    LocalDate date,
    String category,
    double amount,
    String description
) {
}
