package com.chaveiro_abencoado.back.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CadastroRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String nome;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
             message = "Senha deve ter no mínimo 8 caracteres, 1 maiúscula e 1 número")
    private String senha;
}
