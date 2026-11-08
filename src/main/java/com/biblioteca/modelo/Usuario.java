package com.biblioteca.modelo;

public record Usuario(String id, String nombre, String email, String tipo, String passwordHash) { }
