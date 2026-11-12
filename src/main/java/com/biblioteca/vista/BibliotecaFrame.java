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

// Clase principal de la interfaz gráfica, hereda de JFrame
public class BibliotecaFrame extends JFrame {

    private final BibliotecaRepository repo;
    private final Usuario actual;
    
    // Modelos de tabla: definen las columnas para los listados de materiales, usuarios y préstamos
    private final DefaultTableModel mat = model("Código", "Tipo", "Título", "Año", "Editorial", "Disponibles", "Autor / Edición", "ISBN / Volumen");
    private final DefaultTableModel usr = model("ID", "Nombre", "Correo", "Tipo");
    private final DefaultTableModel pres = model("ID", "Usuario", "Material", "Fecha", "Vence", "Activo");

    // Constructor: configura el tamaño, posición y los elementos visuales de la ventana
    public BibliotecaFrame(BibliotecaRepository repo, Usuario actual) {
        super("Biblioteca UES | " + actual.nombre()); // Título de la ventana
        this.repo = repo;
        this.actual = actual;
        
        setDefaultCloseOperation(EXIT_ON_CLOSE); // Cierra el programa al darle a la 'X'
        setSize(980, 560);
        setLocationRelativeTo(null); // Centra la ventana en la pantalla
        
        JTabbedPane tabs = new JTabbedPane(); // Crea un sistema de pestañas
        tabs.add("Materiales", panelMateriales()); // Pestaña completa del Proceso 1
        
        // Verifica si el usuario tiene permisos administrativos para mostrarle la pestaña de Usuarios
        boolean esPersonal = actual.tipo().equalsIgnoreCase("Docente") || actual.tipo().equalsIgnoreCase("Bibliotecario");
        if (esPersonal) {
            tabs.add("Usuarios", tablePanel(usr, "Registrar usuario", this::agregarUsuario));
        }
        
        tabs.add("Préstamos", tablePanel(pres, "Registrar préstamo", this::prestar));
        
        // Panel inferior (footer) para los botones de acción global
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton borrarUsuario = new JButton("Eliminar usuario");
        borrarUsuario.addActionListener(e -> eliminarUsuario());
        footer.add(borrarUsuario);
        borrarUsuario.setVisible(esPersonal); // Solo los administradores pueden ver este botón
        
        JButton borrarMaterial = new JButton("Eliminar material");
        borrarMaterial.addActionListener(e -> eliminarMaterial());
        footer.add(borrarMaterial);
        
        JButton devolver = new JButton("Registrar devolución");
        devolver.addActionListener(e -> devolver());
        footer.add(devolver);
        
        JButton salir = new JButton("Salir del sistema");
        salir.addActionListener(e -> salir());
        footer.add(salir);
        
        // Agrega las pestañas en el centro y los botones en la parte inferior
        add(tabs, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
        
        // Carga los datos de MongoDB en las tablas por primera vez
        refrescar();
    }

    // Panel especializado para el Proceso 1 (Registrar, Modificar, Buscar y Consultar catálogo)
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

        // Controles de búsqueda (Subproceso 1.4)
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

    // Formulario unificado para Registrar (1.1-1.3) y Modificar (1.6) materiales
    private void formularioMaterial(Material existente) {
        boolean esEdicion = existente != null;

        JTextField id = new JTextField(esEdicion ? existente.id() : "");
        id.setEnabled(!esEdicion); // El código único no se edita

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
            "Código único *:", id,
            "Tipo *:", tipo,
            "Título *:", titulo,
            "Año de publicación *:", anio,
            "Editorial *:", editorial,
            "Cantidad disponible *:", cantidad,
            "Autor (Libro):", autor,
            "ISBN (Libro):", isbn,
            "N° Edición (Revista):", edicion,
            "Volumen (Revista):", volumen
        };

        if (JOptionPane.showConfirmDialog(this, f, esEdicion ? "Modificar Material" : "Registrar Material", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            int anioNum;
            int cantNum;
            try {
                anioNum = Integer.parseInt(required(anio));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("El año de publicación debe ser un número entero válido.");
            }
            try {
                cantNum = Integer.parseInt(required(cantidad));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("La cantidad disponible debe ser un número entero válido.");
            }

            String tipoSel = (String) tipo.getSelectedItem();
            Material m = new Material(
                required(id).toUpperCase(),
                tipoSel,
                required(titulo),
                anioNum,
                required(editorial),
                cantNum,
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
        } catch (Exception ex) {
            error(ex);
        }
    }

    // Método auxiliar para crear un panel estándar con tabla y botones
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

    // Muestra un formulario emergente para registrar un nuevo usuario
    private void agregarUsuario() {
        JTextField id = new JTextField(), nombre = new JTextField(), email = new JTextField();
        JPasswordField password = new JPasswordField();
        JComboBox<String> tipo = new JComboBox<>(new String[]{"Estudiante", "Docente", "General"});
        Object[] f = {"ID", id, "Nombre", nombre, "Correo", email, "Tipo", tipo, "Contraseña", password};
        
        if (JOptionPane.showConfirmDialog(this, f, "Registrar usuario", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            String mail = required(email).toLowerCase();
            if (repo.existeEmail(mail)) {
                throw new IllegalArgumentException("Ese correo ya está registrado.");
            }
            repo.guardarUsuario(new Usuario(required(id), required(nombre), mail, (String) tipo.getSelectedItem(), PasswordUtil.hash(new String(password.getPassword()))));
            refrescar();
        } catch (Exception ex) {
            error(ex);
        }
    }

    // Gestiona el proceso de prestar un material a un usuario
    private void prestar() {
        JTextField uid = new JTextField(), mid = new JTextField();
        Object[] f = {"ID usuario", uid, "ID material", mid};
        
        if (JOptionPane.showConfirmDialog(this, f, "Registrar préstamo", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            String u = required(uid), m = required(mid).toUpperCase();
            Material material = repo.buscarMaterial(m);
            
            if (repo.buscarUsuario(u) == null || material == null) {
                throw new IllegalArgumentException("Usuario o material no existe.");
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

    // Solicita el ID de un préstamo para marcarlo como devuelto
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

    // Maneja la eliminación de un usuario con validaciones estrictas
    private void eliminarUsuario() {
        if (!(actual.tipo().equalsIgnoreCase("Docente") || actual.tipo().equalsIgnoreCase("Bibliotecario"))) {
            JOptionPane.showMessageDialog(this, "Solo el personal bibliotecario puede eliminar usuarios.", "Acceso denegado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String id = JOptionPane.showInputDialog(this, "ID del usuario que desea eliminar:");
        if (id == null || id.isBlank()) {
            return;
        }
        id = id.trim();
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
                    "¿Eliminar este usuario?\n\nID: " + usuario.id() + "\nNombre: " + usuario.nombre() + "\nCorreo: " + usuario.email(),
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

    // Maneja la eliminación de un material
    private void eliminarMaterial() {
        String id = JOptionPane.showInputDialog(this, "ID del material que desea eliminar:");
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
                    "¿Eliminar este material?\n\nID: " + material.id() + "\nTítulo: " + material.titulo(),
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

    // Cierra completamente el programa pidiendo confirmación primero
    private void salir() {
        int opcion = JOptionPane.showConfirmDialog(this, "¿Está seguro de que desea salir del sistema?", "Confirmar salida", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (opcion == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    // Carga la tabla de materiales y avisa si una búsqueda no tuvo resultados (Sección 4.5)
    private void cargarTablaMateriales(List<Material> lista, boolean esBusqueda) {
        mat.setRowCount(0);
        if (lista.isEmpty() && esBusqueda) {
            JOptionPane.showMessageDialog(this, "La consulta no arrojó resultados.", "Sin resultados", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        for (Material m : lista) {
            String det1 = "Libro".equals(m.tipo()) ? m.autor() : m.numeroEdicion();
            String det2 = "Libro".equals(m.tipo()) ? m.isbn() : m.volumen();
            mat.addRow(new Object[]{m.id(), m.tipo(), m.titulo(), m.anio(), m.editorial(), m.cantidadDisponible(), det1, det2});
        }
    }

    // Limpia las tablas visuales y vuelve a cargar toda la información desde la base de datos
    private void refrescar() {
        cargarTablaMateriales(repo.materiales(), false);
        
        usr.setRowCount(0);
        for (Usuario u : repo.usuarios()) {
            usr.addRow(new Object[]{u.id(), u.nombre(), u.email(), u.tipo()});
        }
        
        pres.setRowCount(0);
        for (Document p : repo.prestamos()) {
            pres.addRow(new Object[]{p.getString("_id"), p.getString("idUsuario"), p.getString("idMaterial"), p.getString("fecha"), p.getString("vence"), p.getBoolean("activo")});
        }
    }

    // Utilidad: extrae el texto de un campo y lanza un error si el usuario lo dejó vacío
    private static String required(JTextField f) {
        String s = f.getText().trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Completa todos los campos obligatorios (*).");
        }
        return s;
    }

    // Utilidad: crea un modelo de tabla cuyas celdas no se pueden editar con doble clic
    private static DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
    }

    // Utilidad: muestra una ventana de error estándar con el mensaje de la excepción capturada
    private void error(Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "No se pudo completar", JOptionPane.ERROR_MESSAGE);
    }
}