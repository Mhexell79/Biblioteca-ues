/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.biblioteca.repositorio;

import com.biblioteca.modelo.Material;
import com.biblioteca.modelo.Prestamo;
import com.biblioteca.modelo.Usuario;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import com.biblioteca.servicio.PasswordUtil;

public class BibliotecaRepository {
    private final MongoCollection<Document> usuarios, materiales, prestamos;
    public BibliotecaRepository(MongoDatabase db) {
        usuarios = db.getCollection("usuarios"); materiales = db.getCollection("materiales"); prestamos = db.getCollection("prestamos");
        usuarios.createIndex(new Document("email", 1));
        inicializarSiVacio();
    }
    private void inicializarSiVacio() {
        if (usuarios.countDocuments() == 0) {
            String password = "MiClaveSegura123";
            if (password != null && !password.isBlank()) {
                guardarUsuario(new Usuario("USR001", "Bibliotecario Admin", "admin@ues.edu.sv", "Docente", PasswordUtil.hash(password)));
            }
        }
        if (materiales.countDocuments() == 0) {
            guardarMaterial(new Material("MAT001", "Libro", "Cien Años de Soledad", 1967, "Gabriel García Márquez", "978-8424115531", true));
            guardarMaterial(new Material("MAT002", "Libro", "Clean Code", 2008, "Robert C. Martin", "978-0132350884", true));
            guardarMaterial(new Material("MAT003", "Revista", "National Geographic", 2024, "National Geographic Society", "ISSN-0027-9358", true));
        }
    }
    public Usuario autenticar(String email, String hash) {
        Document d = usuarios.find(new Document("email", email).append("passwordHash", hash)).first();
        return d == null ? null : usuario(d);
    }
    public List<Material> materiales() {
        List<Material> out = new ArrayList<>(); for (Document d : materiales.find()) out.add(material(d)); return out;
    }
    public List<Usuario> usuarios() {
        List<Usuario> out = new ArrayList<>(); for (Document d : usuarios.find()) out.add(usuario(d)); return out;
    }
    public void guardarMaterial(Material m) {
        materiales.insertOne(new Document("_id", m.id()).append("tipo", m.tipo()).append("titulo", m.titulo()).append("anio", m.anio()).append("autor", m.autor()).append("codigo", m.codigo()).append("disponible", m.disponible()));
    }
    public void guardarUsuario(Usuario u) {
        usuarios.insertOne(new Document("_id", u.id()).append("nombre", u.nombre()).append("email", u.email()).append("tipo", u.tipo()).append("passwordHash", u.passwordHash()));
    }
    public void guardarPrestamo(Prestamo p) {
        prestamos.insertOne(new Document("_id", p.id()).append("idUsuario", p.idUsuario()).append("idMaterial", p.idMaterial()).append("fecha", p.fecha().toString()).append("vence", p.vence().toString()).append("activo", p.activo()));
        materiales.updateOne(eq("_id", p.idMaterial()), set("disponible", false));
    }
    public boolean eliminarUsuario(String id) { return usuarios.deleteOne(eq("_id", id)).getDeletedCount() > 0; }
    public boolean eliminarMaterial(String id) {
        if (prestamos.find(new Document("idMaterial", id).append("activo", true)).first() != null) {
            throw new IllegalArgumentException("No se puede eliminar: el material tiene un préstamo activo.");
        }
        return materiales.deleteOne(eq("_id", id)).getDeletedCount() > 0;
    }
    public Material buscarMaterial(String id) { Document d = materiales.find(eq("_id", id)).first(); return d == null ? null : material(d); }
    public Usuario buscarUsuario(String id) { Document d = usuarios.find(eq("_id", id)).first(); return d == null ? null : usuario(d); }
    public boolean existeEmail(String email) { return usuarios.find(eq("email", email)).first() != null; }
    public boolean tienePrestamoActivo(String uid) { return prestamos.find(new Document("idUsuario", uid).append("activo", true)).first() != null; }
    public long totalPrestamos() { return prestamos.countDocuments(); }
    public void devolver(String id) {
        Document p = prestamos.find(new Document("_id", id).append("activo", true)).first();
        if (p == null) throw new IllegalArgumentException("Préstamo no encontrado o ya devuelto.");
        prestamos.updateOne(eq("_id", id), set("activo", false)); materiales.updateOne(eq("_id", p.getString("idMaterial")), set("disponible", true));
    }
    public List<Document> prestamos() { return prestamos.find().into(new ArrayList<>()); }
    private static Usuario usuario(Document d) { return new Usuario(d.getString("_id"), d.getString("nombre"), d.getString("email"), d.getString("tipo"), d.getString("passwordHash")); }
    private static Material material(Document d) { return new Material(d.getString("_id"), d.getString("tipo"), d.getString("titulo"), d.getInteger("anio", 0), d.getString("autor"), d.getString("codigo"), d.getBoolean("disponible", true)); }
}

