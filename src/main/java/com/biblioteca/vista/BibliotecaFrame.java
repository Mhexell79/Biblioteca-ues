package com.biblioteca.vista;

import com.biblioteca.modelo.*;
import com.biblioteca.repositorio.BibliotecaRepository;
import com.biblioteca.servicio.PasswordUtil;
import org.bson.Document;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

public class BibliotecaFrame extends JFrame {

    private final BibliotecaRepository repo;
    private final Usuario actual;
    
    // Modelos de tabla actualizados (Proceso 1 y Proceso 2)
    private final DefaultTableModel mat = model("Código", "Tipo", "Título", "Año", "Editorial", "Disponibles", "Autor / Edición", "ISBN / Volumen");
    private final DefaultTableModel usr = model("Código", "Nombre", "Documento", "Contacto", "Correo", "Tipo", "Estado");
    private final DefaultTableModel pres = model("ID", "Usuario", "Material", "Fecha", "Vence", "Activo");

    public BibliotecaFrame(BibliotecaRepository repo, Usuario actual) {
        super("Biblioteca UES | " + actual.nombre());
        this.repo = repo;
        this.actual = actual;
        
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1020, 580);
        setLocationRelativeTo(null);
        
        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Materiales", panelMateriales());
        
        boolean esPersonal = actual.tipo().equalsIgnoreCase("Docente") || actual.tipo().equalsIgnoreCase("Bibliotecario");
        if (esPersonal) {
            tabs.add("Usuarios", panelUsuarios()); // Pestaña completa del Proceso 2
        }
        
        tabs.add("Préstamos", tablePanel(pres, "Registrar préstamo", this::prestar));
        
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton borrarUsuario = new JButton("Eliminar usuario");
        borrarUsuario.addActionListener(e -> eliminarUsuario());
        footer.add(borrarUsuario);
        borrarUsuario.setVisible(esPersonal);
        
        JButton borrarMaterial = new JButton("Eliminar material");
        borrarMaterial.addActionListener(e -> eliminarMaterial());
        footer.add(borrarMaterial);
        
        JButton devolver = new JButton("Registrar devolución");
        devolver.addActionListener(e -> devolver());
        footer.add(devolver);
        
        JButton salir = new JButton("Salir del sistema");
        salir.addActionListener(e -> salir());
        footer.add(salir);
        
        add(tabs, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
        
        refrescar();
    }

    // ==================== PANEL PROCESO 1: MATERIALES ====================

    private JPanel panelMateriales() {
        JPanel p = new JPanel(new BorderLayout());
        JTable tablaMat = new JTable(mat);
        p.add(new JScrollPane(tablaMat), BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton btnAgregar = new JButton("Agregar material");
        btnAgregar.addActionListener(e -> formularioMaterial(null));

        JButton btnModificar = new JButton("Modificar material");
        btnModificar.addActionListener(e -> {
            int fila = tablaMat.getSelectedRow();
            if (fila < 0) {
                JOptionPane.showMessageDialog(this, "Selecciona un material de la tabla para modificarlo.");
                return;
            }
            String codigo = (String) mat.getValueAt(fila, 0);
            formularioMaterial(repo.buscarMaterial(codigo));
        });

        JComboBox<String> filtro = new JComboBox<>(new String[]{"Todos", "Código", "Título", "Autor", "Tipo"});
        JTextField txtBuscar = new JTextField(14);
        JButton btnBuscar = new JButton("Buscar");
        btnBuscar.addActionListener(e -> {
            List<Material> resultados = repo.buscarMateriales((String) filtro.getSelectedItem(), txtBuscar.getText());
            cargarTablaMateriales(resultados, true);
        });

        JButton btnMostrarTodos = new JButton("Mostrar todos");
        btnMostrarTodos.addActionListener(e -> {
            txtBuscar.setText("");
            cargarTablaMateriales(repo.materiales(), false);
        });

        bar.add(btnAgregar);
        bar.add(btnModificar);
        bar.add(new JLabel(" | Buscar por:"));
        bar.add(filtro);
        bar.add(txtBuscar);
        bar.add(btnBuscar);
        bar.add(btnMostrarTodos);

        p.add(bar, BorderLayout.NORTH);
        return p;
    }

    private void formularioMaterial(Material existente) {
        boolean esEdicion = existente != null;

        JTextField id = new JTextField(esEdicion ? existente.id() : "");
        id.setEnabled(!esEdicion);

        JComboBox<String> tipo = new JComboBox<>(new String[]{"Libro", "Revista"});
        if (esEdicion) tipo.setSelectedItem(existente.tipo());

        JTextField titulo = new JTextField(esEdicion ? existente.titulo() : "");
        JTextField anio = new JTextField(esEdicion ? String.valueOf(existente.anio()) : String.valueOf(LocalDate.now().getYear()));
        JTextField editorial = new JTextField(esEdicion ? existente.editorial() : "");
        JTextField cantidad = new JTextField(esEdicion ? String.valueOf(existente.cantidadDisponible()) : "1");

        JTextField autor = new JTextField(esEdicion ? existente.autor() : "");
        JTextField isbn = new JTextField(esEdicion ? existente.isbn() : "");
        JTextField edicion = new JTextField(esEdicion ? existente.numeroEdicion() : "");
        JTextField volumen = new JTextField(esEdicion ? existente.volumen() : "");

        Runnable actualizarCampos = () -> {
            boolean esLibro = "Libro".equals(tipo.getSelectedItem());
            autor.setEnabled(esLibro);
            isbn.setEnabled(esLibro);
            edicion.setEnabled(!esLibro);
            volumen.setEnabled(!esLibro);
        };
        actualizarCampos.run();
        tipo.addActionListener(e -> actualizarCampos.run());

        Object[] f = {
            "Código único *:", id, "Tipo *:", tipo, "Título *:", titulo,
            "Año de publicación *:", anio, "Editorial *:", editorial, "Cantidad disponible *:", cantidad,
            "Autor (Libro):", autor, "ISBN (Libro):", isbn,
            "N° Edición (Revista):", edicion, "Volumen (Revista):", volumen
        };

        if (JOptionPane.showConfirmDialog(this, f, esEdicion ? "Modificar Material" : "Registrar Material", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            int anioNum = Integer.parseInt(required(anio));
            int cantNum = Integer.parseInt(required(cantidad));
            String tipoSel = (String) tipo.getSelectedItem();

            Material m = new Material(
                required(id).toUpperCase(), tipoSel, required(titulo), anioNum, required(editorial), cantNum,
                "Libro".equals(tipoSel) ? autor.getText().trim() : "",
                "Libro".equals(tipoSel) ? isbn.getText().trim() : "",
                "Revista".equals(tipoSel) ? edicion.getText().trim() : "",
                "Revista".equals(tipoSel) ? volumen.getText().trim() : ""
            );

            if (esEdicion) {
                repo.modificarMaterial(m);
                JOptionPane.showMessageDialog(this, "Material actualizado correctamente.");
            } else {
                repo.guardarMaterial(m);
                JOptionPane.showMessageDialog(this, "Material registrado correctamente.");
            }
            refrescar();
        } catch (NumberFormatException ex) {
            error(new IllegalArgumentException("El año y la cantidad disponible deben ser números enteros válidos."));
        } catch (Exception ex) {
            error(ex);
        }
    }

    // ==================== PANEL PROCESO 2: USUARIOS ====================

    private JPanel panelUsuarios() {
        JPanel p = new JPanel(new BorderLayout());
        JTable tablaUsr = new JTable(usr);
        p.add(new JScrollPane(tablaUsr), BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Subproceso 2.1: Registrar usuario
        JButton btnRegistrar = new JButton("Registrar usuario");
        btnRegistrar.addActionListener(e -> formularioUsuario(null));

        // Subproceso 2.4: Modificar usuario
        JButton btnModificar = new JButton("Modificar usuario");
        btnModificar.addActionListener(e -> {
            int fila = tablaUsr.getSelectedRow();
            if (fila < 0) {
                JOptionPane.showMessageDialog(this, "Selecciona un usuario de la tabla para modificarlo.");
                return;
            }
            String codigo = (String) usr.getValueAt(fila, 0);
            formularioUsuario(repo.buscarUsuario(codigo));
        });

        // Subproceso 2.5: Inactivar o reactivar usuario
        JButton btnEstado = new JButton("Activar / Inactivar");
        btnEstado.addActionListener(e -> {
            int fila = tablaUsr.getSelectedRow();
            if (fila < 0) {
                JOptionPane.showMessageDialog(this, "Selecciona un usuario de la tabla para cambiar su estado.");
                return;
            }
            String codigo = (String) usr.getValueAt(fila, 0);
            if (codigo.equals(actual.id())) {
                JOptionPane.showMessageDialog(this, "No puedes inactivar tu propia cuenta en sesión.", "Acción no permitida", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                Usuario seleccionado = repo.buscarUsuario(codigo);
                boolean nuevoEstado = !seleccionado.activo();
                repo.cambiarEstadoUsuario(codigo, nuevoEstado);
                refrescar();
                JOptionPane.showMessageDialog(this, "Usuario " + (nuevoEstado ? "habilitado" : "inactivado") + " correctamente.");
            } catch (Exception ex) {
                error(ex);
            }
        });

        // Subproceso 2.2: Buscar usuario por código, documento o nombre
        JComboBox<String> filtro = new JComboBox<>(new String[]{"Todos", "Código", "Documento", "Nombre"});
        JTextField txtBuscar = new JTextField(13);
        JButton btnBuscar = new JButton("Buscar");
        btnBuscar.addActionListener(e -> {
            List<Usuario> resultados = repo.buscarUsuarios((String) filtro.getSelectedItem(), txtBuscar.getText());
            cargarTablaUsuarios(resultados, true);
        });

        JButton btnTodos = new JButton("Mostrar todos");
        btnTodos.addActionListener(e -> {
            txtBuscar.setText("");
            cargarTablaUsuarios(repo.usuarios(), false);
        });

        bar.add(btnRegistrar);
        bar.add(btnModificar);
        bar.add(btnEstado);
        bar.add(new JLabel(" | Buscar:"));
        bar.add(filtro);
        bar.add(txtBuscar);
        bar.add(btnBuscar);
        bar.add(btnTodos);

        p.add(bar, BorderLayout.NORTH);
        return p;
    }

    // Formulario unificado para Registrar (2.1) y Modificar (2.4) usuarios
    private void formularioUsuario(Usuario existente) {
        boolean esEdicion = existente != null;

        JTextField id = new JTextField(esEdicion ? existente.id() : "");
        id.setEnabled(!esEdicion);

        JTextField nombre = new JTextField(esEdicion ? existente.nombre() : "");
        JTextField documento = new JTextField(esEdicion ? existente.documento() : "");
        JTextField contacto = new JTextField(esEdicion ? existente.contacto() : "");
        JTextField email = new JTextField(esEdicion ? existente.email() : "");

        JComboBox<String> tipo = new JComboBox<>(new String[]{"Estudiante", "Docente", "Bibliotecario", "General"});
        if (esEdicion) tipo.setSelectedItem(existente.tipo());

        JPasswordField password = new JPasswordField();
        JCheckBox chkActivo = new JCheckBox("Usuario habilitado para préstamos", !esEdicion || existente.activo());
        if (esEdicion && existente.id().equals(actual.id())) {
            chkActivo.setEnabled(false); // Evita que el usuario logueado se auto-desactive
        }

        Object[] f = {
            "Código único *:", id,
            "Nombre completo *:", nombre,
            "Documento (DUI/Carnet) *:", documento,
            "Datos de contacto (Teléfono) *:", contacto,
            "Correo electrónico *:", email,
            "Tipo de usuario *:", tipo,
            esEdicion ? "Nueva contraseña (dejar vacío para mantener):" : "Contraseña *:", password,
            "Estado:", chkActivo
        };

        if (JOptionPane.showConfirmDialog(this, f, esEdicion ? "Modificar Usuario" : "Registrar Usuario", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            String passTexto = new String(password.getPassword());
            if (!esEdicion && passTexto.isBlank()) {
                throw new IllegalArgumentException("La contraseña es obligatoria para nuevos usuarios.");
            }
            String hashFinal = (esEdicion && passTexto.isBlank())
                    ? existente.passwordHash()
                    : PasswordUtil.hash(passTexto);

            Usuario u = new Usuario(
                required(id).toUpperCase(),
                required(nombre),
                required(documento),
                required(contacto),
                required(email).toLowerCase(),
                (String) tipo.getSelectedItem(),
                hashFinal,
                chkActivo.isSelected()
            );

            if (esEdicion) {
                repo.modificarUsuario(u);
                JOptionPane.showMessageDialog(this, "Usuario actualizado correctamente.");
            } else {
                repo.guardarUsuario(u);
                JOptionPane.showMessageDialog(this, "Usuario registrado correctamente.");
            }
            refrescar();
        } catch (Exception ex) {
            error(ex);
        }
    }

    // ==================== PRÉSTAMOS, DEVOLUCIONES Y ACCIONES GLOBALES ====================

    private JPanel tablePanel(DefaultTableModel model, String label, Runnable action) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
        
        JButton b = new JButton(label);
        b.addActionListener(e -> action.run());
        
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.add(b);
        
        JButton refresh = new JButton("Actualizar");
        refresh.addActionListener(e -> refrescar());
        bar.add(refresh);
        
        p.add(bar, BorderLayout.NORTH);
        return p;
    }

    private void prestar() {
        JTextField uid = new JTextField(), mid = new JTextField();
        Object[] f = {"Código usuario", uid, "Código material", mid};
        
        if (JOptionPane.showConfirmDialog(this, f, "Registrar préstamo", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            String u = required(uid).toUpperCase(), m = required(mid).toUpperCase();
            Usuario usuario = repo.buscarUsuario(u);
            Material material = repo.buscarMaterial(m);
            
            if (usuario == null || material == null) {
                throw new IllegalArgumentException("Usuario o material no existe.");
            }
            if (!usuario.activo()) {
                throw new IllegalArgumentException("El usuario está inactivo y no tiene permiso para solicitar préstamos.");
            }
            if (!material.estaDisponible()) {
                throw new IllegalArgumentException("El material no tiene ejemplares disponibles.");
            }
            
            int dias = material.tipo().equals("Revista") ? 7 : 15;
            String id = "P" + String.format("%03d", repo.totalPrestamos() + 1);
            
            repo.guardarPrestamo(new Prestamo(id, u, m, LocalDate.now(), LocalDate.now().plusDays(dias), true));
            refrescar();
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void devolver() {
        String id = JOptionPane.showInputDialog(this, "ID del préstamo:");
        if (id == null || id.isBlank()) {
            return;
        }
        try {
            repo.devolver(id.trim());
            refrescar();
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void eliminarUsuario() {
        if (!(actual.tipo().equalsIgnoreCase("Docente") || actual.tipo().equalsIgnoreCase("Bibliotecario"))) {
            JOptionPane.showMessageDialog(this, "Solo el personal bibliotecario puede eliminar usuarios.", "Acceso denegado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String id = JOptionPane.showInputDialog(this, "Código del usuario que desea eliminar:");
        if (id == null || id.isBlank()) {
            return;
        }
        id = id.trim().toUpperCase();
        try {
            Usuario usuario = repo.buscarUsuario(id);
            if (usuario == null) {
                throw new IllegalArgumentException("No se encontró el usuario " + id + ".");
            }
            if (usuario.id().equals(actual.id())) {
                throw new IllegalArgumentException("No puede eliminar la cuenta con la que inició sesión.");
            }
            if (repo.tienePrestamoActivo(id)) {
                throw new IllegalArgumentException("No se puede eliminar: el usuario tiene préstamos activos.");
            }
            
            int confirmacion = JOptionPane.showConfirmDialog(this,
                    "¿Eliminar este usuario?\n\nCódigo: " + usuario.id() + "\nNombre: " + usuario.nombre() + "\nDocumento: " + usuario.documento(),
                    "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirmacion == JOptionPane.YES_OPTION) {
                if (repo.eliminarUsuario(id)) {
                    refrescar();
                    JOptionPane.showMessageDialog(this, "Usuario eliminado.");
                } else {
                    throw new IllegalArgumentException("No se pudo eliminar el usuario.");
                }
            }
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void eliminarMaterial() {
        String id = JOptionPane.showInputDialog(this, "Código del material que desea eliminar:");
        if (id == null || id.isBlank()) {
            return;
        }
        id = id.trim().toUpperCase();
        try {
            Material material = repo.buscarMaterial(id);
            if (material == null) {
                throw new IllegalArgumentException("No se encontró el material " + id + ".");
            }
            
            int confirmacion = JOptionPane.showConfirmDialog(this,
                    "¿Eliminar este material?\n\nCódigo: " + material.id() + "\nTítulo: " + material.titulo(),
                    "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (confirmacion == JOptionPane.YES_OPTION) {
                if (repo.eliminarMaterial(id)) {
                    refrescar();
                    JOptionPane.showMessageDialog(this, "Material eliminado.");
                } else {
                    throw new IllegalArgumentException("No se pudo eliminar el material.");
                }
            }
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void salir() {
        int opcion = JOptionPane.showConfirmDialog(this, "¿Está seguro de que desea salir del sistema?", "Confirmar salida", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (opcion == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    private void cargarTablaMateriales(List<Material> lista, boolean esBusqueda) {
        mat.setRowCount(0);
        if (lista.isEmpty() && esBusqueda) {
            JOptionPane.showMessageDialog(this, "La consulta de materiales no arrojó resultados.", "Sin resultados", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        for (Material m : lista) {
            String det1 = "Libro".equals(m.tipo()) ? m.autor() : m.numeroEdicion();
            String det2 = "Libro".equals(m.tipo()) ? m.isbn() : m.volumen();
            mat.addRow(new Object[]{m.id(), m.tipo(), m.titulo(), m.anio(), m.editorial(), m.cantidadDisponible(), det1, det2});
        }
    }

    private void cargarTablaUsuarios(List<Usuario> lista, boolean esBusqueda) {
        usr.setRowCount(0);
        if (lista.isEmpty() && esBusqueda) {
            JOptionPane.showMessageDialog(this, "La consulta de usuarios no arrojó resultados.", "Sin resultados", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        for (Usuario u : lista) {
            usr.addRow(new Object[]{
                u.id(), u.nombre(), u.documento(), u.contacto(), u.email(), u.tipo(), u.activo() ? "Habilitado" : "Inactivo"
            });
        }
    }

    private void refrescar() {
        cargarTablaMateriales(repo.materiales(), false);
        cargarTablaUsuarios(repo.usuarios(), false);
        
        pres.setRowCount(0);
        for (Document p : repo.prestamos()) {
            pres.addRow(new Object[]{p.getString("_id"), p.getString("idUsuario"), p.getString("idMaterial"), p.getString("fecha"), p.getString("vence"), p.getBoolean("activo")});
        }
    }

    private static String required(JTextField f) {
        String s = f.getText().trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Completa todos los campos obligatorios (*).");
        }
        return s;
    }

    private static DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
    }

    private void error(Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "No se pudo completar", JOptionPane.ERROR_MESSAGE);
    }
}