package com.biblioteca.repositorio;

import com.biblioteca.modelo.Material;
import com.biblioteca.modelo.Prestamo;
import com.biblioteca.modelo.Usuario;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import com.biblioteca.servicio.PasswordUtil;

// Clase encargada de gestionar toda la comunicación entre la aplicación y MongoDB
public class BibliotecaRepository {
    
    // Variables para manejar las tres "tablas" (colecciones en MongoDB) principales
    private final MongoCollection<Document> usuarios, materiales, prestamos;
    
    // Constructor: recibe la conexión a la base de datos y prepara las colecciones
    public BibliotecaRepository(MongoDatabase db) {
        usuarios = db.getCollection("usuarios"); 
        materiales = db.getCollection("materiales"); 
        prestamos = db.getCollection("prestamos");
        
        // Crea un índice en el email de los usuarios para hacer búsquedas mucho más rápidas
        usuarios.createIndex(new Document("email", 1));
        
        // Carga datos iniciales de prueba si el sistema está nuevo
        inicializarSiVacio();
    }
    
    // Método privado que inyecta datos por defecto si las colecciones están vacías
    private void inicializarSiVacio() {
        // Si no hay usuarios, crea un administrador inicial
        if (usuarios.countDocuments() == 0) {
            String password = "MiClaveSegura123";
            if (password != null && !password.isBlank()) {
                guardarUsuario(new Usuario("USR001", "Bibliotecario Admin", "admin@ues.edu.sv", "Docente", PasswordUtil.hash(password)));
            }
        }
        // Si no hay materiales, crea 3 libros/revistas de ejemplo
        if (materiales.countDocuments() == 0) {
            guardarMaterial(new Material("MAT001", "Libro", "Cien Años de Soledad", 1967, "Gabriel García Márquez", "978-8424115531", true));
            guardarMaterial(new Material("MAT002", "Libro", "Clean Code", 2008, "Robert C. Martin", "978-0132350884", true));
            guardarMaterial(new Material("MAT003", "Revista", "National Geographic", 2024, "National Geographic Society", "ISSN-0027-9358", true));
        }
    }
    
    // Busca un usuario por su correo y su contraseña encriptada (para el Login)
    public Usuario autenticar(String email, String hash) {
        Document d = usuarios.find(new Document("email", email).append("passwordHash", hash)).first();
        return d == null ? null : usuario(d);
    }
    
    // Obtiene una lista completa con todos los materiales de la biblioteca
    public List<Material> materiales() {
        List<Material> out = new ArrayList<>(); 
        for (Document d : materiales.find()) out.add(material(d)); 
        return out;
    }
    
    // Obtiene una lista completa con todos los usuarios registrados
    public List<Usuario> usuarios() {
        List<Usuario> out = new ArrayList<>(); 
        for (Document d : usuarios.find()) out.add(usuario(d)); 
        return out;
    }
    
    // Inserta un nuevo material en la base de datos transformando el objeto a formato BSON (Document)
    public void guardarMaterial(Material m) {
        materiales.insertOne(new Document("_id", m.id()).append("tipo", m.tipo()).append("titulo", m.titulo()).append("anio", m.anio()).append("autor", m.autor()).append("codigo", m.codigo()).append("disponible", m.disponible()));
    }
    
    // Inserta un nuevo usuario en la base de datos
    public void guardarUsuario(Usuario u) {
        usuarios.insertOne(new Document("_id", u.id()).append("nombre", u.nombre()).append("email", u.email()).append("tipo", u.tipo()).append("passwordHash", u.passwordHash()));
    }
    
    // Registra un nuevo préstamo Y cambia el estado del material a "No disponible"
    public void guardarPrestamo(Prestamo p) {
        prestamos.insertOne(new Document("_id", p.id()).append("idUsuario", p.idUsuario()).append("idMaterial", p.idMaterial()).append("fecha", p.fecha().toString()).append("vence", p.vence().toString()).append("activo", p.activo()));
        materiales.updateOne(eq("_id", p.idMaterial()), set("disponible", false));
    }
    
    // Elimina un usuario por su ID. Retorna true si se eliminó exitosamente
    public boolean eliminarUsuario(String id) { 
        return usuarios.deleteOne(eq("_id", id)).getDeletedCount() > 0; 
    }
    
    // Elimina un material, pero primero verifica que nadie lo tenga prestado actualmente
    public boolean eliminarMaterial(String id) {
        if (prestamos.find(new Document("idMaterial", id).append("activo", true)).first() != null) {
            throw new IllegalArgumentException("No se puede eliminar: el material tiene un préstamo activo.");
        }
        return materiales.deleteOne(eq("_id", id)).getDeletedCount() > 0;
  
    }
    
    // Busca y devuelve los datos de un material específico usando su ID
    public Material buscarMaterial(String id) { 
        Document d = materiales.find(eq("_id", id)).first(); 
        return d == null ? null : material(d); 
    }
    
    // Busca y devuelve los datos de un usuario específico usando su ID
    public Usuario buscarUsuario(String id) { 
        Document d = usuarios.find(eq("_id", id)).first(); 
        return d == null ? null : usuario(d); 
    }
    
    // Verifica si un correo electrónico ya está registrado en la base de datos
    public boolean existeEmail(String email) { 
        return usuarios.find(eq("email", email)).first() != null; 
    }
    
    // Revisa si un usuario específico tiene algún material que aún no ha devuelto
    public boolean tienePrestamoActivo(String uid) { 
        return prestamos.find(new Document("idUsuario", uid).append("activo", true)).first() != null; 
    }
    
    // Cuenta el número total de préstamos registrados (históricos y activos)
    public long totalPrestamos() { 
        return prestamos.countDocuments(); 
    }
    
    // Procesa la devolución: marca el préstamo como inactivo Y el material como disponible
    public void devolver(String id) {
        Document p = prestamos.find(new Document("_id", id).append("activo", true)).first();
        if (p == null) throw new IllegalArgumentException("Préstamo no encontrado o ya devuelto.");
        
        prestamos.updateOne(eq("_id", id), set("activo", false)); 
        materiales.updateOne(eq("_id", p.getString("idMaterial")), set("disponible", true));
    }
    
    // Devuelve todos los registros de préstamos (como Documentos crudos de Mongo)
    public List<Document> prestamos() { 
        return prestamos.find().into(new ArrayList<>()); 
    }
    
    // Función auxiliar (Helper) para convertir un Documento de MongoDB a un objeto 'Usuario' de Java
    private static Usuario usuario(Document d) { 
        return new Usuario(d.getString("_id"), d.getString("nombre"), d.getString("email"), d.getString("tipo"), d.getString("passwordHash")); 
    }
    
    // Función auxiliar (Helper) para convertir un Documento de MongoDB a un objeto 'Material' de Java
    private static Material material(Document d) { 
        return new Material(d.getString("_id"), d.getString("tipo"), d.getString("titulo"), d.getInteger("anio", 0), d.getString("autor"), d.getString("codigo"), d.getBoolean("disponible", true)); 
    }
}