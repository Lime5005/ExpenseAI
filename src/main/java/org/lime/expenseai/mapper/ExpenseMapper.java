package org.lime.expenseai.mapper;

import org.lime.expenseai.entity.Expense;
import org.lime.expenseai.model.ExpenseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExpenseMapper {
    ExpenseDto toDto(Expense expense);
    Expense toEntity(ExpenseDto expenseDto);
}
