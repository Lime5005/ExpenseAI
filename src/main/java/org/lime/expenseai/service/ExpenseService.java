package org.lime.expenseai.service;

import org.lime.expenseai.entity.Expense;
import org.lime.expenseai.mapper.ExpenseMapper;
import org.lime.expenseai.model.ExpenseDto;
import org.lime.expenseai.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ExpenseService {
    private final ExpenseRepository expenseRepository;

    private final ExpenseMapper expenseMapper;

    // Constructor for ExpenseService, injecting ExpenseRepository and ExpenseMapper
    public ExpenseService(ExpenseRepository expenseRepository,
                          ExpenseMapper expenseMapper) {
        this.expenseRepository = expenseRepository;
        this.expenseMapper = expenseMapper;
    }

    public java.util.List<org.lime.expenseai.model.ExpenseDto> getAllExpenses() {
        return expenseRepository.findAll().stream()
                .map(expenseMapper::toDto)
                .toList();
    }

    public List<ExpenseDto> getByMonth(YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        return expenseRepository.findByDateBetween(startDate, endDate).stream().map(expenseMapper::toDto).toList();
    }

    public List<ExpenseDto> getByDate(LocalDate date) {
        return expenseRepository.findByDate(date).stream().map(expenseMapper::toDto).toList();
    }

    // Adds a new expense
    public void addExpense(ExpenseDto expenseDto) {
        Expense expense = expenseMapper.toEntity(expenseDto);
        expenseRepository.save(expense);
    }

    // Updates an existing expense
    public ExpenseDto updateExpense(Long id, ExpenseDto expenseDto) {
        if (!expenseRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Expense not found");
        }
        Expense expense = expenseMapper.toEntity(expenseDto);
        expense.setId(id);
        Expense updatedExpense = expenseRepository.save(expense);
        return expenseMapper.toDto(updatedExpense);
    }

    public ExpenseDto updateExpenseByDateAndDescription(LocalDate date, String description, ExpenseDto newValues) {
        Expense match = expenseRepository.findByDate(date).stream()
                .filter(e -> e.getDescription() != null && e.getDescription().equalsIgnoreCase(description))
                .max(Comparator.comparingLong(Expense::getId))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Expense not found for date/description"));

        ExpenseDto dtoWithId = new ExpenseDto(
                match.getId(),
                newValues.date(),
                newValues.category(),
                newValues.amount(),
                newValues.description()
        );
        return updateExpense(match.getId(), dtoWithId);
    }

}
