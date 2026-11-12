package com.biblioteca.servicio;

// Importaciones necesarias para manejar el texto y los algoritmos de encriptación de Java
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

// Clase de utilidad para encriptar contraseñas (hashing)
public final class PasswordUtil {
    
    // Constructor privado: evita que se creen objetos de esta clase usando "new PasswordUtil()"
    private PasswordUtil() { }
    
    // Método estático que toma una contraseña de texto plano y devuelve su versión encriptada
    public static String hash(String password) {
        try { 
            // Convierte el texto usando el algoritmo SHA-256 y lo formatea como un código hexadecimal
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8))); 
        }
        catch (NoSuchAlgorithmException e) { 
            // Lanza un error si el sistema operativo de la computadora no soporta el algoritmo SHA-256
            throw new IllegalStateException(e); 
        }
    }
}
