package com.biblioteca.vista;

import com.biblioteca.modelo.Usuario;
import com.biblioteca.repositorio.BibliotecaRepository;
import com.biblioteca.servicio.PasswordUtil;
import javax.swing.*;
import java.awt.*;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

public class LoginFrame extends JFrame {
    public LoginFrame(BibliotecaRepository repo) {
        super("Biblioteca UES | Iniciar sesión"); setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(380, 220); setLocationRelativeTo(null);
        JTextField email = new JTextField("admin@ues.edu.sv", 22); JPasswordField pass = new JPasswordField(22);
        JButton entrar = new JButton("Ingresar");
        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8)); form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        form.add(new JLabel("Correo")); form.add(email); form.add(new JLabel("Contraseña")); form.add(pass); form.add(new JLabel()); form.add(entrar); add(form);
        entrar.addActionListener(e -> {
            try {
                Usuario u = repo.autenticar(email.getText().trim(), PasswordUtil.hash(new String(pass.getPassword())));
                if (u == null) { JOptionPane.showMessageDialog(this, "Correo o contraseña incorrectos."); return; }
                new BibliotecaFrame(repo, u).setVisible(true); dispose();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
        });
        getRootPane().setDefaultButton(entrar);
    }
}

