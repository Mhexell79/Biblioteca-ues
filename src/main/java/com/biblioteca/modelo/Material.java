package com.biblioteca.modelo;

// Estructura para guardar los datos de libros y revistas del catálogo
public record Material(
    String id,                 // Código único del material (ej. MAT001)
    String tipo,               // Categoría: "Libro" o "Revista"
    String titulo,             // Nombre principal de la obra
    int anio,                  // Año en que fue publicado
    String editorial,          // Casa editorial (Subproceso 1.1)
    int cantidadDisponible,    // Ejemplares disponibles en inventario (>= 0)
    String autor,              // Específico de Libro (Subproceso 1.2)
    String isbn,               // Específico de Libro (Subproceso 1.2)
    String numeroEdicion,      // Específico de Revista (Subproceso 1.3)
    String volumen             // Específico de Revista (Subproceso 1.3)
) {
    // Método auxiliar para saber rápidamente si hay al menos 1 ejemplar para prestar
    public boolean estaDisponible() {
        return cantidadDisponible > 0;
    }
}