package com.encurtador.infrastructure.adapter.in.web;

import com.encurtador.domain.exception.CodeAlreadyExistsException;
import com.encurtador.domain.exception.InvalidManagementKeyException;
import com.encurtador.domain.exception.LinkExpiredException;
import com.encurtador.domain.exception.LinkNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Erro de validação");
        problem.setType(URI.create("https://problemas.encurtador.com/erro-de-validacao"));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Dados de entrada inválidos");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Erro de validação");
        problem.setType(URI.create("https://problemas.encurtador.com/erro-de-validacao"));
        return problem;
    }

    @ExceptionHandler(CodeAlreadyExistsException.class)
    public ProblemDetail handleCodeAlreadyExists(CodeAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Código já em uso");
        problem.setType(URI.create("https://problemas.encurtador.com/codigo-em-uso"));
        return problem;
    }

    @ExceptionHandler(LinkNotFoundException.class)
    public ProblemDetail handleLinkNotFound(LinkNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Link não encontrado");
        problem.setType(URI.create("https://problemas.encurtador.com/link-nao-encontrado"));
        return problem;
    }

    @ExceptionHandler(LinkExpiredException.class)
    public ProblemDetail handleLinkExpired(LinkExpiredException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.GONE, ex.getMessage());
        problem.setTitle("Link expirado");
        problem.setType(URI.create("https://problemas.encurtador.com/link-expirado"));
        return problem;
    }

    @ExceptionHandler(InvalidManagementKeyException.class)
    public ProblemDetail handleInvalidManagementKey(InvalidManagementKeyException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Não autorizado");
        problem.setType(URI.create("https://problemas.encurtador.com/chave-invalida"));
        return problem;
    }
}
