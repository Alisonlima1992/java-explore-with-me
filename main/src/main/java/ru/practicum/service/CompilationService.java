package ru.practicum.service;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;

import java.util.List;

public interface CompilationService {

    CompilationDto createCompilation(NewCompilationDto newCompilationDto);

    CompilationDto updateCompilation(Long compilationId, NewCompilationDto updateCompilationDto);

    void deleteCompilation(Long compilationId);

    List<CompilationDto> getCompilations(Boolean pinned, @Min(0) Integer from, @Positive Integer size);

    CompilationDto getCompilationById(Long compilationId);
}