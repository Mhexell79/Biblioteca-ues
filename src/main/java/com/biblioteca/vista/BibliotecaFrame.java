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

// Clase principal de la interfaz gráfica, hereda de JFrame (una ventana de Windows/Mac/Linux)
public class BibliotecaFrame extends JFrame {

    private final BibliotecaRepository repo;
    private final Usuario actual;
    
    // Modelos de tabla: definen las columnas para los listados de materiales, usuarios y préstamos
    private final DefaultTableModel mat = model("ID", "Tipo", "Título", "Año", "Autor", "Código", "Disponible");
    private final DefaultTableModel usr = model("ID", "Nombre", "Correo", "Tipo");
    private final DefaultTableModel pres = model("ID", "Usuario", "Material", "Fecha", "Vence", "Activo");

    // Constructor: configura el tamaño, posición y los elementos visuales de la ventana
    public BibliotecaFrame(BibliotecaRepository repo, Usuario actual) {
        super("Biblioteca UES | " + actual.nombre()); // Título de la ventana
        this.repo = repo;
        this.actual = actual;
        
        setDefaultCloseOperation(EXIT_ON_CLOSE); // Cierra el programa al darle a la 'X'
        setSize(900, 560);
        setLocationRelativeTo(null); // Centra la ventana en la pantalla
        
        JTabbedPane tabs = new JTabbedPane(); // Crea un sistema de pestañas
        tabs.add("Materiales", tablePanel(mat, "Agregar material", this::agregarMaterial));
        
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

    // Método auxiliar para crear un panel que contiene una tabla y botones en la parte superior
    private JPanel tablePanel(DefaultTableModel model, String label, Runnable action) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER); // Agrega barra de desplazamiento a la tabla
        
        JButton b = new JButton(label);
        b.addActionListener(e -> action.run()); // Ejecuta la acción pasada como parámetro al hacer clic
        
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.add(b);
        
        JButton refresh = new JButton("Actualizar");
        refresh.addActionListener(e -> refrescar());
        bar.add(refresh);
        
        p.add(bar, BorderLayout.NORTH);
        return p;
    }

    // Muestra un formulario emergente para registrar un nuevo material
    private void agregarMaterial() {
        JTextField id = new JTextField(), titulo = new JTextField(), anio = new JTextField("2024"), autor = new JTextField(), codigo = new JTextField();
        JComboBox<String> tipo = new JComboBox<>(new String[]{"Libro", "Revista"});
        Object[] f = {"ID", id, "Tipo", tipo, "Título", titulo, "Año", anio, "Autor", autor, "ISBN/ISSN", codigo};
        
        // Si el usuario presiona "Cancelar", se detiene la ejecución
        if (JOptionPane.showConfirmDialog(this, f, "Nuevo material", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            // Guarda el material en la base de datos y actualiza las tablas visuales
            repo.guardarMaterial(new Material(required(id), (String) tipo.getSelectedItem(), required(titulo), Integer.parseInt(required(anio)), required(autor), required(codigo), true));
            refrescar();
        } catch (Exception ex) {
            error(ex);
        }
    }

    // Muestra un formulario emergente para registrar un nuevo usuario
    private void agregarUsuario() {
        JTextField id = new JTextField(), nombre = new JTextField(), email = new JTextField();
        JPasswordField password = new JPasswordField(); // Oculta el texto que se escribe
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
            // Encripta la contraseña antes de guardar al usuario
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
            String u = required(uid), m = required(mid);
            Material material = repo.buscarMaterial(m);
            
            // Validaciones de negocio: existencia y disponibilidad
            if (repo.buscarUsuario(u) == null || material == null) {
                throw new IllegalArgumentException("Usuario o material no existe.");
            }
            if (!material.disponible()) {
                throw new IllegalArgumentException("El material no está disponible.");
            }
            
            // Las revistas se prestan por 7 días, los libros por 15
            int dias = material.tipo().equals("Revista") ? 7 : 15;
            String id = "P" + String.format("%03d", repo.totalPrestamos() + 1); // Genera un ID automático
            
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
            return; // Si el usuario cancela o no escribe nada, sale del método
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
        // Doble verificación de seguridad por si acaso el botón se llega a mostrar por error
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
            
            // Pide confirmación final antes de borrar de la base de datos
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
        id = id.trim();
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
            error(ex); // El repositorio lanza error automáticamente si tiene préstamos activos
        }
    }

    // Cierra completamente el programa pidiendo confirmación primero
    private void salir() {
        int opcion = JOptionPane.showConfirmDialog(this, "¿Está seguro de que desea salir del sistema?", "Confirmar salida", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (opcion == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    // Limpia las tablas visuales y vuelve a cargar toda la información desde la base de datos
    private void refrescar() {
        mat.setRowCount(0); // Borra todas las filas actuales
        for (Material m : repo.materiales()) {
            mat.addRow(new Object[]{m.id(), m.tipo(), m.titulo(), m.anio(), m.autor(), m.codigo(), m.disponible()});
        }
        
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
            throw new IllegalArgumentException("Completa todos los campos.");
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
