package com.biblioteca.repositorio;

import com.biblioteca.modelo.Material;
import com.biblioteca.modelo.Prestamo;
import com.biblioteca.modelo.Usuario;
import com.biblioteca.servicio.PasswordUtil;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;

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
        // Si no hay materiales, crea 3 libros/revistas de ejemplo con el formato completo
        if (materiales.countDocuments() == 0) {
            guardarMaterial(new Material("MAT001", "Libro", "Cien Años de Soledad", 1967, "Sudamericana", 3, "Gabriel García Márquez", "978-8424115531", "", ""));
            guardarMaterial(new Material("MAT002", "Libro", "Clean Code", 2008, "Prentice Hall", 2, "Robert C. Martin", "978-0132350884", "", ""));
            guardarMaterial(new Material("MAT003", "Revista", "National Geographic", 2024, "National Geographic Society", 5, "", "", "Ed. 412", "Vol. 245"));
        }
    }
    
    // Busca un usuario por su correo y su contraseña encriptada (para el Login)
    public Usuario autenticar(String email, String hash) {
        Document d = usuarios.find(new Document("email", email).append("passwordHash", hash)).first();
        return d == null ? null : usuario(d);
    }
    
    // Obtiene una lista completa con todos los materiales de la biblioteca (Subproceso 1.5)
    public List<Material> materiales() {
        List<Material> out = new ArrayList<>(); 
        for (Document d : materiales.find()) out.add(material(d)); 
        return out;
    }

    // Busca materiales filtrando por código, título, autor o tipo (Subproceso 1.4)
    public List<Material> buscarMateriales(String criterio, String texto) {
        String q = texto == null ? "" : texto.trim().toLowerCase();
        List<Material> resultados = new ArrayList<>();
        for (Material m : materiales()) {
            if (q.isEmpty()) {
                resultados.add(m);
                continue;
            }
            boolean coincide = switch (criterio) {
                case "Código" -> m.id().toLowerCase().contains(q);
                case "Título" -> m.titulo().toLowerCase().contains(q);
                case "Autor" -> m.autor().toLowerCase().contains(q);
                case "Tipo" -> m.tipo().toLowerCase().contains(q);
                default -> m.id().toLowerCase().contains(q)
                        || m.titulo().toLowerCase().contains(q)
                        || m.autor().toLowerCase().contains(q)
                        || m.tipo().toLowerCase().contains(q);
            };
            if (coincide) {
                resultados.add(m);
            }
        }
        return resultados;
    }
    
    // Obtiene una lista completa con todos los usuarios registrados
    public List<Usuario> usuarios() {
        List<Usuario> out = new ArrayList<>(); 
        for (Document d : usuarios.find()) out.add(usuario(d)); 
        return out;
    }
    
    // Inserta un nuevo material validando unicidad, rangos y campos obligatorios (Subprocesos 1.1, 1.2 y 1.3)
    public void guardarMaterial(Material m) {
        validarDatosMaterial(m);
        String codigoUnico = m.id().trim().toUpperCase();
        if (buscarMaterial(codigoUnico) != null) {
            throw new IllegalArgumentException("Código duplicado: ya existe un material con el código " + codigoUnico + ".");
        }
        try {
            materiales.insertOne(new Document("_id", codigoUnico)
                    .append("tipo", m.tipo())
                    .append("titulo", m.titulo().trim())
                    .append("anio", m.anio())
                    .append("editorial", m.editorial().trim())
                    .append("cantidadDisponible", m.cantidadDisponible())
                    .append("autor", m.autor() != null ? m.autor().trim() : "")
                    .append("isbn", m.isbn() != null ? m.isbn().trim() : "")
                    .append("numeroEdicion", m.numeroEdicion() != null ? m.numeroEdicion().trim() : "")
                    .append("volumen", m.volumen() != null ? m.volumen().trim() : ""));
        } catch (MongoException e) {
            throw new RuntimeException("Error de escritura en MongoDB al registrar el material: " + e.getMessage(), e);
        }
    }

    // Actualiza los datos permitidos de un material existente (Subproceso 1.6)
    public void modificarMaterial(Material m) {
        validarDatosMaterial(m);
        if (buscarMaterial(m.id()) == null) {
            throw new IllegalArgumentException("No existe un material registrado con el código " + m.id() + ".");
        }
        try {
            materiales.updateOne(eq("_id", m.id()), new Document("$set", new Document("tipo", m.tipo())
                    .append("titulo", m.titulo().trim())
                    .append("anio", m.anio())
                    .append("editorial", m.editorial().trim())
                    .append("cantidadDisponible", m.cantidadDisponible())
                    .append("autor", m.autor() != null ? m.autor().trim() : "")
                    .append("isbn", m.isbn() != null ? m.isbn().trim() : "")
                    .append("numeroEdicion", m.numeroEdicion() != null ? m.numeroEdicion().trim() : "")
                    .append("volumen", m.volumen() != null ? m.volumen().trim() : "")));
        } catch (MongoException e) {
            throw new RuntimeException("Error de escritura en MongoDB al modificar el material: " + e.getMessage(), e);
        }
    }

    // Valida las reglas de entrada de materiales (Sección 4.4)
    private void validarDatosMaterial(Material m) {
        if (m.id() == null || m.id().isBlank()) {
            throw new IllegalArgumentException("El código único del material es obligatorio.");
        }
        if (m.titulo() == null || m.titulo().isBlank()) {
            throw new IllegalArgumentException("El título del material es obligatorio.");
        }
        if (m.editorial() == null || m.editorial().isBlank()) {
            throw new IllegalArgumentException("La editorial es obligatoria.");
        }
        int anioMax = LocalDate.now().getYear();
        if (m.anio() < 1450 || m.anio() > anioMax) {
            throw new IllegalArgumentException("El año de publicación debe estar entre 1450 y " + anioMax + ".");
        }
        if (m.cantidadDisponible() < 0) {
            throw new IllegalArgumentException("La cantidad disponible debe ser un entero no negativo (0 o mayor).");
        }
    }
    
    // Inserta un nuevo usuario en la base de datos
    public void guardarUsuario(Usuario u) {
        usuarios.insertOne(new Document("_id", u.id()).append("nombre", u.nombre()).append("email", u.email()).append("tipo", u.tipo()).append("passwordHash", u.passwordHash()));
    }
    
    // Registra un nuevo préstamo y descuenta 1 ejemplar de la cantidad disponible
    public void guardarPrestamo(Prestamo p) {
        prestamos.insertOne(new Document("_id", p.id()).append("idUsuario", p.idUsuario()).append("idMaterial", p.idMaterial()).append("fecha", p.fecha().toString()).append("vence", p.vence().toString()).append("activo", p.activo()));
        materiales.updateOne(eq("_id", p.idMaterial()), inc("cantidadDisponible", -1));
    }
    
    // Elimina un usuario por su ID. Retorna true si se eliminó exitosamente
    public boolean eliminarUsuario(String id) { 
        return usuarios.deleteOne(eq("_id", id)).getDeletedCount() > 0; 
    }
    
    // Elimina un material validando que no tenga préstamos activos ni dependencias históricas (Subproceso 1.7)
    public boolean eliminarMaterial(String id) {
        if (prestamos.find(new Document("idMaterial", id).append("activo", true)).first() != null) {
            throw new IllegalArgumentException("No se puede eliminar: el material tiene un préstamo activo.");
        }
        if (prestamos.find(new Document("idMaterial", id)).first() != null) {
            throw new IllegalArgumentException("No se puede eliminar: existen registros históricos de préstamos que dependen de este material.");
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
    
    // Procesa la devolución: marca el préstamo como inactivo y devuelve 1 ejemplar al stock
    public void devolver(String id) {
        Document p = prestamos.find(new Document("_id", id).append("activo", true)).first();
        if (p == null) throw new IllegalArgumentException("Préstamo no encontrado o ya devuelto.");
        
        prestamos.updateOne(eq("_id", id), set("activo", false)); 
        materiales.updateOne(eq("_id", p.getString("idMaterial")), inc("cantidadDisponible", 1));
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
        return new Material(
            d.getString("_id"),
            d.getString("tipo"),
            d.getString("titulo"),
            d.getInteger("anio", 0),
            d.getString("editorial") != null ? d.getString("editorial") : "Sin editorial",
            d.getInteger("cantidadDisponible", 0),
            d.getString("autor") != null ? d.getString("autor") : "",
            d.getString("isbn") != null ? d.getString("isbn") : "",
            d.getString("numeroEdicion") != null ? d.getString("numeroEdicion") : "",
            d.getString("volumen") != null ? d.getString("volumen") : ""
        ); 
    }
}