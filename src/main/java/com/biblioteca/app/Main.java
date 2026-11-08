package com.biblioteca.app;

import com.biblioteca.config.MongoConfig;
import com.biblioteca.repositorio.BibliotecaRepository;
import com.biblioteca.vista.LoginFrame;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                MongoConfig mongo = new MongoConfig();
                BibliotecaRepository repo = new BibliotecaRepository(mongo.database());
                Runtime.getRuntime().addShutdownHook(new Thread(mongo::close));
                new LoginFrame(repo).setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "No se pudo conectar con MongoDB. Verifica MONGODB_URI y que MongoDB esté activo.\n" + e.getMessage(), "Error de conexión", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
