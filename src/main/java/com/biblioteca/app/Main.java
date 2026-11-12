package com.biblioteca.app;

import com.biblioteca.config.MongoConfig;
import com.biblioteca.repositorio.BibliotecaRepository;
import com.biblioteca.vista.LoginFrame;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Ejecuta la interfaz gráfica de forma segura en un hilo secundario
        SwingUtilities.invokeLater(() -> {
            try {
                // Aplica el estilo visual del sistema operativo a las ventanas
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                // Configura e inicia la conexión con la base de datos MongoDB
                MongoConfig mongo = new MongoConfig();
                // Crea el repositorio pasándole la base de datos conectada
                BibliotecaRepository repo = new BibliotecaRepository(mongo.database());
                // Asegura que la conexión a la base de datos se cierre al salir del programa
                Runtime.getRuntime().addShutdownHook(new Thread(mongo::close));
                // Crea y muestra la ventana de inicio de sesión
                new LoginFrame(repo).setVisible(true);
            } catch (Exception e) {
                // Muestra un mensaje de error en pantalla si falla la conexión
                JOptionPane.showMessageDialog(null, "No se pudo conectar con MongoDB. Verifica MONGODB_URI y que MongoDB esté activo.\n" + e.getMessage(), "Error de conexión", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}