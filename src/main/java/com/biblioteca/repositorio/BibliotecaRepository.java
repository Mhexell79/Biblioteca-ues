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
    
    private final MongoCollection<Document> usuarios, materiales, prestamos;
    
    public BibliotecaRepository(MongoDatabase db) {
        usuarios = db.getCollection("usuarios"); 
        materiales = db.getCollection("materiales"); 
        prestamos = db.getCollection("prestamos");
        
        usuarios.createIndex(new Document("email", 1));
        inicializarSiVacio();
    }
    
    private void inicializarSiVacio() {
        // Usuario administrador inicial con documento, contacto y estado activo
        if (usuarios.countDocuments() == 0) {
            String password = "MiClaveSegura123";
            if (password != null && !password.isBlank()) {
                guardarUsuario(new Usuario(
                        "USR001", "Bibliotecario Admin", "00000000-0", "2222-0000",
                        "admin@ues.edu.sv", "Docente", PasswordUtil.hash(password), true
                ));
            }
        }
        // Materiales iniciales de prueba
        if (materiales.countDocuments() == 0) {
            guardarMaterial(new Material("MAT001", "Libro", "Cien Años de Soledad", 1967, "Sudamericana", 3, "Gabriel García Márquez", "978-8424115531", "", ""));
            guardarMaterial(new Material("MAT002", "Libro", "Clean Code", 2008, "Prentice Hall", 2, "Robert C. Martin", "978-0132350884", "", ""));
            guardarMaterial(new Material("MAT003", "Revista", "National Geographic", 2024, "National Geographic Society", 5, "", "", "Ed. 412", "Vol. 245"));
        }
    }
    
    public Usuario autenticar(String email, String hash) {
        Document d = usuarios.find(new Document("email", email).append("passwordHash", hash)).first();
        return d == null ? null : usuario(d);
    }

    // ==================== PROCESO 1: MATERIALES ====================
    
    public List<Material> materiales() {
        List<Material> out = new ArrayList<>(); 
        for (Document d : materiales.find()) out.add(material(d)); 
        return out;
    }

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
            if (coincide) resultados.add(m);
        }
        return resultados;
    }
    
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

    private void validarDatosMaterial(Material m) {
        if (m.id() == null || m.id().isBlank()) throw new IllegalArgumentException("El código único del material es obligatorio.");
        if (m.titulo() == null || m.titulo().isBlank()) throw new IllegalArgumentException("El título del material es obligatorio.");
        if (m.editorial() == null || m.editorial().isBlank()) throw new IllegalArgumentException("La editorial es obligatoria.");
        int anioMax = LocalDate.now().getYear();
        if (m.anio() < 1450 || m.anio() > anioMax) {
            throw new IllegalArgumentException("El año de publicación debe estar entre 1450 y " + anioMax + ".");
        }
        if (m.cantidadDisponible() < 0) {
            throw new IllegalArgumentException("La cantidad disponible debe ser un entero no negativo (0 o mayor).");
        }
    }

    public boolean eliminarMaterial(String id) {
        if (prestamos.find(new Document("idMaterial", id).append("activo", true)).first() != null) {
            throw new IllegalArgumentException("No se puede eliminar: el material tiene un préstamo activo.");
        }
        if (prestamos.find(new Document("idMaterial", id)).first() != null) {
            throw new IllegalArgumentException("No se puede eliminar: existen registros históricos de préstamos que dependen de este material.");
        }
        return materiales.deleteOne(eq("_id", id)).getDeletedCount() > 0;
    }

    public Material buscarMaterial(String id) { 
        Document d = materiales.find(eq("_id", id)).first(); 
        return d == null ? null : material(d); 
    }

    // ==================== PROCESO 2: USUARIOS ====================

    // Subproceso 2.3: Consultar todos los usuarios registrados
    public List<Usuario> usuarios() {
        List<Usuario> out = new ArrayList<>(); 
        for (Document d : usuarios.find()) out.add(usuario(d)); 
        return out;
    }

    // Subproceso 2.2: Buscar usuarios por código, documento o nombre
    public List<Usuario> buscarUsuarios(String criterio, String texto) {
        String q = texto == null ? "" : texto.trim().toLowerCase();
        List<Usuario> resultados = new ArrayList<>();
        for (Usuario u : usuarios()) {
            if (q.isEmpty()) {
                resultados.add(u);
                continue;
            }
            boolean coincide = switch (criterio) {
                case "Código" -> u.id().toLowerCase().contains(q);
                case "Documento" -> u.documento().toLowerCase().contains(q);
                case "Nombre" -> u.nombre().toLowerCase().contains(q);
                default -> u.id().toLowerCase().contains(q)
                        || u.documento().toLowerCase().contains(q)
                        || u.nombre().toLowerCase().contains(q);
            };
            if (coincide) resultados.add(u);
        }
        return resultados;
    }

    // Subproceso 2.1: Registrar usuario con validaciones completas
    public void guardarUsuario(Usuario u) {
        validarDatosUsuario(u);
        String codigoUnico = u.id().trim().toUpperCase();
        if (buscarUsuario(codigoUnico) != null) {
            throw new IllegalArgumentException("Código duplicado: ya existe un usuario con el código " + codigoUnico + ".");
        }
        if (existeEmail(u.email().trim().toLowerCase())) {
            throw new IllegalArgumentException("El correo electrónico ya está registrado.");
        }
        if (existeDocumento(u.documento().trim())) {
            throw new IllegalArgumentException("El número de documento ya está registrado en otro usuario.");
        }
        try {
            usuarios.insertOne(new Document("_id", codigoUnico)
                    .append("nombre", u.nombre().trim())
                    .append("documento", u.documento().trim())
                    .append("contacto", u.contacto().trim())
                    .append("email", u.email().trim().toLowerCase())
                    .append("tipo", u.tipo())
                    .append("passwordHash", u.passwordHash())
                    .append("activo", u.activo()));
        } catch (MongoException e) {
            throw new RuntimeException("Error de escritura en MongoDB al registrar el usuario: " + e.getMessage(), e);
        }
    }

    // Subproceso 2.4: Modificar datos permitidos de un usuario
    public void modificarUsuario(Usuario u) {
        validarDatosUsuario(u);
        Usuario actualDb = buscarUsuario(u.id());
        if (actualDb == null) {
            throw new IllegalArgumentException("No existe un usuario con el código " + u.id() + ".");
        }
        String nuevoEmail = u.email().trim().toLowerCase();
        if (!actualDb.email().equalsIgnoreCase(nuevoEmail) && existeEmail(nuevoEmail)) {
            throw new IllegalArgumentException("Ese correo electrónico ya pertenece a otro usuario.");
        }
        if (!actualDb.documento().equalsIgnoreCase(u.documento().trim()) && existeDocumento(u.documento().trim())) {
            throw new IllegalArgumentException("Ese documento ya pertenece a otro usuario.");
        }
        // Si se está inactivando desde edición, validar que no tenga préstamos activos
        if (!u.activo() && tienePrestamoActivo(u.id())) {
            throw new IllegalArgumentException("No se puede inactivar: el usuario tiene préstamos activos pendientes.");
        }
        try {
            usuarios.updateOne(eq("_id", u.id()), new Document("$set", new Document("nombre", u.nombre().trim())
                    .append("documento", u.documento().trim())
                    .append("contacto", u.contacto().trim())
                    .append("email", nuevoEmail)
                    .append("tipo", u.tipo())
                    .append("passwordHash", u.passwordHash())
                    .append("activo", u.activo())));
        } catch (MongoException e) {
            throw new RuntimeException("Error de escritura en MongoDB al modificar el usuario: " + e.getMessage(), e);
        }
    }

    // Subproceso 2.5: Activar o inactivar usuario comprobando préstamos activos
    public void cambiarEstadoUsuario(String id, boolean nuevoEstado) {
        Usuario u = buscarUsuario(id);
        if (u == null) {
            throw new IllegalArgumentException("No se encontró el usuario " + id + ".");
        }
        if (!nuevoEstado && tienePrestamoActivo(id)) {
            throw new IllegalArgumentException("No se puede inactivar: el usuario tiene préstamos activos.");
        }
        try {
            usuarios.updateOne(eq("_id", id), set("activo", nuevoEstado));
        } catch (MongoException e) {
            throw new RuntimeException("Error al actualizar el estado del usuario en MongoDB: " + e.getMessage(), e);
        }
    }

    // Subproceso 2.5: Eliminar usuario verificando préstamos activos
    public boolean eliminarUsuario(String id) {
        if (tienePrestamoActivo(id)) {
            throw new IllegalArgumentException("No se puede eliminar: el usuario tiene préstamos activos.");
        }
        return usuarios.deleteOne(eq("_id", id)).getDeletedCount() > 0; 
    }

    private void validarDatosUsuario(Usuario u) {
        if (u.id() == null || u.id().isBlank()) throw new IllegalArgumentException("El código único del usuario es obligatorio.");
        if (u.nombre() == null || u.nombre().isBlank()) throw new IllegalArgumentException("El nombre completo es obligatorio.");
        if (u.documento() == null || u.documento().isBlank()) throw new IllegalArgumentException("El documento de identidad/carnet es obligatorio.");
        if (u.contacto() == null || u.contacto().isBlank()) throw new IllegalArgumentException("Los datos de contacto son obligatorios.");
        if (u.email() == null || u.email().isBlank() || !u.email().contains("@")) {
            throw new IllegalArgumentException("Ingrese un correo electrónico válido.");
        }
    }

    public Usuario buscarUsuario(String id) { 
        Document d = usuarios.find(eq("_id", id)).first(); 
        return d == null ? null : usuario(d); 
    }

    public boolean existeEmail(String email) { 
        return usuarios.find(eq("email", email)).first() != null; 
    }

    public boolean existeDocumento(String documento) {
        return usuarios.find(eq("documento", documento)).first() != null;
    }

    public boolean tienePrestamoActivo(String uid) { 
        return prestamos.find(new Document("idUsuario", uid).append("activo", true)).first() != null; 
    }

    // ==================== PRÉSTAMOS Y DEVOLUCIONES ====================

    public void guardarPrestamo(Prestamo p) {
        prestamos.insertOne(new Document("_id", p.id()).append("idUsuario", p.idUsuario()).append("idMaterial", p.idMaterial()).append("fecha", p.fecha().toString()).append("vence", p.vence().toString()).append("activo", p.activo()));
        materiales.updateOne(eq("_id", p.idMaterial()), inc("cantidadDisponible", -1));
    }

    public long totalPrestamos() { 
        return prestamos.countDocuments(); 
    }

    public void devolver(String id) {
        Document p = prestamos.find(new Document("_id", id).append("activo", true)).first();
        if (p == null) throw new IllegalArgumentException("Préstamo no encontrado o ya devuelto.");
        
        prestamos.updateOne(eq("_id", id), set("activo", false)); 
        materiales.updateOne(eq("_id", p.getString("idMaterial")), inc("cantidadDisponible", 1));
    }

    public List<Document> prestamos() { 
        return prestamos.find().into(new ArrayList<>()); 
    }

    // ==================== MAPPERS BSON -> JAVA ====================

    private static Usuario usuario(Document d) { 
        return new Usuario(
            d.getString("_id"),
            d.getString("nombre"),
            d.getString("documento") != null ? d.getString("documento") : "N/A",
            d.getString("contacto") != null ? d.getString("contacto") : "N/A",
            d.getString("email"),
            d.getString("tipo"),
            d.getString("passwordHash"),
            d.getBoolean("activo", true)
        ); 
    }

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