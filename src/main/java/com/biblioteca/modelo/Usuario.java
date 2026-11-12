package com.biblioteca.modelo;

// Estructura inmutable para representar a una persona registrada en la biblioteca
public record Usuario(
    String id,            // Identificador único del usuario en la base de datos
    String nombre,        // Nombre completo de la persona
    String documento,     //Dui o Carnet
    String contacto,      // Telefono
    String email,         // Correo electrónico utilizado para contacto y acceso al sistema
    String tipo,          // Rol de la persona (ejemplo: "Administrador", "Estudiante", "Profesor")
    String passwordHash,  // Contraseña encriptada por seguridad (nunca debe estar en texto legible)
    boolean activo        // Estado del usuario
) { 
    // Las llaves vacías indican que Java genera todos los métodos internos automáticamente
}
