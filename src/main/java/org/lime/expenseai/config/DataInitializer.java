package org.lime.expenseai.config;

import org.lime.expenseai.model.ExpenseDto;
import org.lime.expenseai.service.ExpenseService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedExpenses(ExpenseService expenseService) {
        return args -> {
            if (expenseService.getAllExpenses().isEmpty()) {
                expenseService.addExpense(new ExpenseDto(null, LocalDate.of(2025, 6, 2), "FOOD", 30, "Dinner"));
                expenseService.addExpense(new ExpenseDto(null, LocalDate.of(2025, 6, 8), "SHOPPING", 300, "Buy clothes"));
                expenseService.addExpense(new ExpenseDto(null, LocalDate.of(2025, 6, 15), "GROCERIES", 88.99, "Supermarket"));
            }
        };
    }
}
