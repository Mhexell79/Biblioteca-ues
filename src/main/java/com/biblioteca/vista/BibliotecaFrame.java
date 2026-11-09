package com.biblioteca.vista;

import com.biblioteca.modelo.*;
import com.biblioteca.repositorio.BibliotecaRepository;
import com.biblioteca.servicio.PasswordUtil;
import org.bson.Document;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

public class BibliotecaFrame extends JFrame {
    private final BibliotecaRepository repo;
    private final Usuario actual;
    private final DefaultTableModel mat = model("ID", "Tipo", "Título", "Año", "Autor", "Código", "Disponible");
    private final DefaultTableModel usr = model("ID", "Nombre", "Correo", "Tipo");
    private final DefaultTableModel pres = model("ID", "Usuario", "Material", "Fecha", "Vence", "Activo");
    public BibliotecaFrame(BibliotecaRepository repo, Usuario actual) {
        super("Biblioteca UES | " + actual.nombre()); this.repo = repo; this.actual = actual; setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(900, 560); setLocationRelativeTo(null);
        JTabbedPane tabs = new JTabbedPane(); tabs.add("Materiales", tablePanel(mat, "Agregar material", this::agregarMaterial));
        tabs.add("Usuarios", tablePanel(usr, "Registrar usuario", this::agregarUsuario));
        if (actual.tipo().equalsIgnoreCase("Docente") || actual.tipo().equalsIgnoreCase("Bibliotecario")) tabs.add("Préstamos", tablePanel(pres, "Registrar préstamo", this::prestar));
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton borrarUsuario = new JButton("Eliminar usuario"); borrarUsuario.addActionListener(e -> eliminarUsuario()); footer.add(borrarUsuario);
        JButton devolver = new JButton("Registrar devolución"); devolver.addActionListener(e -> devolver()); footer.add(devolver);
        JButton salir = new JButton("Salir del sistema"); salir.addActionListener(e -> salir()); footer.add(salir);
        add(tabs, BorderLayout.CENTER); add(footer, BorderLayout.SOUTH); refrescar();
    }
    private JPanel tablePanel(DefaultTableModel model, String label, Runnable action) {
        JPanel p = new JPanel(new BorderLayout()); p.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER); JButton b = new JButton(label); b.addActionListener(e -> action.run()); JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT)); bar.add(b); JButton refresh = new JButton("Actualizar"); refresh.addActionListener(e -> refrescar()); bar.add(refresh); p.add(bar, BorderLayout.NORTH); return p;
    }
    private void agregarMaterial() {
        JTextField id = new JTextField(), titulo = new JTextField(), anio = new JTextField("2024"), autor = new JTextField(), codigo = new JTextField(); JComboBox<String> tipo = new JComboBox<>(new String[]{"Libro", "Revista"});
        Object[] f = {"ID", id, "Tipo", tipo, "Título", titulo, "Año", anio, "Autor", autor, "ISBN/ISSN", codigo};
        if (JOptionPane.showConfirmDialog(this, f, "Nuevo material", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try { repo.guardarMaterial(new Material(required(id), (String)tipo.getSelectedItem(), required(titulo), Integer.parseInt(required(anio)), required(autor), required(codigo), true)); refrescar(); }
        catch (Exception ex) { error(ex); }
    }
    private void agregarUsuario() {
        JTextField id = new JTextField(), nombre = new JTextField(), email = new JTextField(); JPasswordField password = new JPasswordField(); JComboBox<String> tipo = new JComboBox<>(new String[]{"Estudiante", "Docente", "General"});
        Object[] f = {"ID", id, "Nombre", nombre, "Correo", email, "Tipo", tipo, "Contraseña", password};
        if (JOptionPane.showConfirmDialog(this, f, "Registrar usuario", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try { String mail = required(email).toLowerCase(); if (repo.existeEmail(mail)) throw new IllegalArgumentException("Ese correo ya está registrado."); repo.guardarUsuario(new Usuario(required(id), required(nombre), mail, (String)tipo.getSelectedItem(), PasswordUtil.hash(new String(password.getPassword())))); refrescar(); }
        catch (Exception ex) { error(ex); }
    }
    private void prestar() {
        JTextField uid = new JTextField(), mid = new JTextField(); Object[] f = {"ID usuario", uid, "ID material", mid};
        if (JOptionPane.showConfirmDialog(this, f, "Registrar préstamo", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try { String u = required(uid), m = required(mid); Material material = repo.buscarMaterial(m); if (repo.buscarUsuario(u) == null || material == null) throw new IllegalArgumentException("Usuario o material no existe."); if (!material.disponible()) throw new IllegalArgumentException("El material no está disponible."); int dias = material.tipo().equals("Revista") ? 7 : 15; String id = "P" + String.format("%03d", repo.totalPrestamos() + 1); repo.guardarPrestamo(new Prestamo(id, u, m, LocalDate.now(), LocalDate.now().plusDays(dias), true)); refrescar(); }
        catch (Exception ex) { error(ex); }
    }
    private void devolver() {
        String id = JOptionPane.showInputDialog(this, "ID del préstamo:"); if (id == null || id.isBlank()) return;
        try { repo.devolver(id.trim()); refrescar(); } catch (Exception ex) { error(ex); }
    }
    private void eliminarUsuario() {
        if (!(actual.tipo().equalsIgnoreCase("Docente") || actual.tipo().equalsIgnoreCase("Bibliotecario"))) {
            JOptionPane.showMessageDialog(this, "Solo el personal bibliotecario puede eliminar usuarios.", "Acceso denegado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String id = JOptionPane.showInputDialog(this, "ID del usuario que desea eliminar:");
        if (id == null || id.isBlank()) return;
        id = id.trim();
        try {
            Usuario usuario = repo.buscarUsuario(id);
            if (usuario == null) throw new IllegalArgumentException("No se encontró el usuario " + id + ".");
            if (usuario.id().equals(actual.id())) throw new IllegalArgumentException("No puede eliminar la cuenta con la que inició sesión.");
            if (repo.tienePrestamoActivo(id)) throw new IllegalArgumentException("No se puede eliminar: el usuario tiene préstamos activos.");
            int confirmacion = JOptionPane.showConfirmDialog(this,
                    "¿Eliminar este usuario?\n\nID: " + usuario.id() + "\nNombre: " + usuario.nombre() + "\nCorreo: " + usuario.email(),
                    "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirmacion == JOptionPane.YES_OPTION) {
                if (repo.eliminarUsuario(id)) { refrescar(); JOptionPane.showMessageDialog(this, "Usuario eliminado."); }
                else throw new IllegalArgumentException("No se pudo eliminar el usuario.");
            }
        } catch (Exception ex) { error(ex); }
    }
    private void salir() {
        int opcion = JOptionPane.showConfirmDialog(this, "¿Está seguro de que desea salir del sistema?", "Confirmar salida", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (opcion == JOptionPane.YES_OPTION) System.exit(0);
    }
    private void refrescar() {
        mat.setRowCount(0); for (Material m : repo.materiales()) mat.addRow(new Object[]{m.id(),m.tipo(),m.titulo(),m.anio(),m.autor(),m.codigo(),m.disponible()});
        usr.setRowCount(0); for (Usuario u : repo.usuarios()) usr.addRow(new Object[]{u.id(),u.nombre(),u.email(),u.tipo()});
        pres.setRowCount(0); for (Document p : repo.prestamos()) pres.addRow(new Object[]{p.getString("_id"),p.getString("idUsuario"),p.getString("idMaterial"),p.getString("fecha"),p.getString("vence"),p.getBoolean("activo")});
    }
    private static String required(JTextField f) { String s=f.getText().trim(); if (s.isEmpty()) throw new IllegalArgumentException("Completa todos los campos."); return s; }
    private static DefaultTableModel model(String... cols) { return new DefaultTableModel(cols,0) { @Override public boolean isCellEditable(int r,int c) { return false; } }; }
    private void error(Exception e) { JOptionPane.showMessageDialog(this, e.getMessage(), "No se pudo completar", JOptionPane.ERROR_MESSAGE); }
}
