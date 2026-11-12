package com.biblioteca.modelo;

// Importamos la herramienta de Java para manejar fechas simples (sin horas)
import java.time.LocalDate;

// "record" crea una estructura inmutable para guardar los datos del préstamo
public record Prestamo(
    String id,           // Identificador único de este préstamo en la base de datos
    String idUsuario,    // Identificador de la persona que se lleva el material
    String idMaterial,   // Identificador del libro, revista o recurso prestado
    LocalDate fecha,     // Fecha en la que el usuario se llevó el material
    LocalDate vence,     // Fecha límite para que el usuario devuelva el material
    boolean activo       // Estado: true si no lo ha devuelto, false si ya fue entregado
) { 
    // Llaves vacías porque Java se encarga de crear los métodos internos automáticamente
}
