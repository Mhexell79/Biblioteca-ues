package com.biblioteca.vista;

// Importaciones para conectar con la base de datos, utilidades y la interfaz visual
import com.biblioteca.modelo.Usuario;
import com.biblioteca.repositorio.BibliotecaRepository;
import com.biblioteca.servicio.PasswordUtil;
import javax.swing.*;
import java.awt.*;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

// Hereda de JFrame para comportarse como una ventana tradicional del sistema operativo
public class LoginFrame extends JFrame {
    
    // Constructor: recibe la conexión a la base de datos para poder consultar los usuarios
    public LoginFrame(BibliotecaRepository repo) {
        super("Biblioteca UES | Iniciar sesión"); // Título de la ventana
        setDefaultCloseOperation(EXIT_ON_CLOSE);  // Cierra todo el programa si se cierra la ventana
        setSize(380, 220);                        // Ancho y alto de la ventana en píxeles
        setLocationRelativeTo(null);              // Centra la ventana en la pantalla
        
        // Campos de texto: el correo viene con un valor por defecto para facilitar las pruebas
        JTextField email = new JTextField("admin@ues.edu.sv", 22); 
        JPasswordField pass = new JPasswordField(22); // Oculta el texto tecleado por seguridad
        
        JButton entrar = new JButton("Ingresar");
        
        // Panel organizador usando una cuadrícula de 3 filas y 2 columnas con separación de 8px
        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8)); 
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Márgenes internos
        
        // Agrega las etiquetas y los campos a la cuadrícula en orden (izq-der, arriba-abajo)
        form.add(new JLabel("Correo")); 
        form.add(email); 
        form.add(new JLabel("Contraseña")); 
        form.add(pass); 
        form.add(new JLabel()); // Espacio en blanco en la cuadrícula
        form.add(entrar); 
        add(form);
        
        // Define qué sucede al presionar el botón "Ingresar"
        entrar.addActionListener(e -> {
            try {
                // Busca al usuario encriptando la contraseña tecleada para comparar con la BD
                Usuario u = repo.autenticar(email.getText().trim(), PasswordUtil.hash(new String(pass.getPassword())));
                
                // Si la consulta devuelve nulo (no existe o clave mala), muestra un error y detiene la acción
                if (u == null) { 
                    JOptionPane.showMessageDialog(this, "Correo o contraseña incorrectos."); 
                    return; 
                }
                if(!u.activo()){
                    JOptionPane.showMessageDialog(this, "Esta cuenta se encuentra inactiva ","Acceso Denegado", JOptionPane.WARNING_MESSAGE);
                    return;
                }
     
                
                // Si el acceso es válido, abre el menú principal pasándole los datos del usuario
                new BibliotecaFrame(repo, u).setVisible(true); 
                dispose(); // Cierra y destruye la ventana de inicio de sesión para liberar memoria
                
            } catch (Exception ex) { 
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); 
            }
        });
        
        // Excelente detalle: Permite activar el botón "Ingresar" presionando la tecla "Enter"
        getRootPane().setDefaultButton(entrar);
    }
}
