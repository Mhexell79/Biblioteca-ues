package com.biblioteca.modelo;

// "record" genera automáticamente una estructura para guardar datos sin escribir código extra
public record Material(
    String id,          // Identificador único del material en la base de datos (MongoDB)
    String tipo,        // Categoría del material (ej. Libro, Revista, Tesis)
    String titulo,      // Nombre principal de la obra
    int anio,           // Año en que fue publicado
    String autor,       // Persona o entidad que creó la obra
    String codigo,      // Código interno de clasificación o ISBN
    boolean disponible  // Estado: true si se puede prestar, false si está ocupado
) { 
    // Las llaves vacías indican que no necesitamos agregar lógica adicional manual
}
