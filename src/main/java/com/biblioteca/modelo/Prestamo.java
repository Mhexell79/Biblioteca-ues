package com.biblioteca.modelo;

import java.time.LocalDate;

public record Prestamo(String id, String idUsuario, String idMaterial, LocalDate fecha, LocalDate vence, boolean activo) { }
