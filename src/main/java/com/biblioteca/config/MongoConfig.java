package com.biblioteca.config;
// Importaciones necesarias para manejar la conexión con MongoDB
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

// La clase implementa AutoCloseable para permitir el cierre seguro de los recursos
public final class MongoConfig implements AutoCloseable {
    // Cliente que mantiene la conexión física con el servidor de MongoDB
    private final MongoClient client;
    // Representa la base de datos específica con la que vamos a interactuar
    private final MongoDatabase database;
    
// Constructor: se ejecuta automáticamente al crear un objeto MongoConfig
    public MongoConfig() {
        // Obtiene la ruta de conexión del sistema, o usa localhost por defecto
        String uri = System.getenv().getOrDefault("MONGODB_URI", "mongodb://localhost:27017");
        // Obtiene el nombre de la base de datos del sistema, o usa "biblioteca_ues" por defecto
        String db = System.getenv().getOrDefault("MONGODB_DATABASE", "biblioteca_ues");
        // Crea la conexión inicial con el servidor usando la ruta (URI)
        client = MongoClients.create(uri);
        // Selecciona la base de datos específica para trabajar con ella
        database = client.getDatabase(db);
        // Ejecuta un "ping" rápido a la base de datos para confirmar que la conexión fue exitosa
        client.getDatabase("admin").runCommand(new org.bson.Document("ping", 1));
    }

    // Método para que otras partes del programa puedan acceder a la base de datos
    public MongoDatabase database() { return database; }
    // Método que cierra la conexión de forma segura cuando el programa termina
    @Override
    public void close() { client.close(); }
}
