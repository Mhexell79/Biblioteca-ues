package com.biblioteca.config;
//configuracion para conectar mongo
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public final class MongoConfig implements AutoCloseable {
    private final MongoClient client;
    private final MongoDatabase database;

    public MongoConfig() {
        String uri = System.getenv().getOrDefault("MONGODB_URI", "mongodb://localhost:27017");
        String db = System.getenv().getOrDefault("MONGODB_DATABASE", "biblioteca_ues");
        client = MongoClients.create(uri);
        database = client.getDatabase(db);
        client.getDatabase("admin").runCommand(new org.bson.Document("ping", 1));
    }

    public MongoDatabase database() { return database; }
    @Override public void close() { client.close(); }
}
