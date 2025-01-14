package edu.pmdm.corrochano_josimdbapp.models;

import com.google.gson.annotations.SerializedName;

public class Genero {

    // Atributos
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String nombre;

    // Constructor
    public Genero(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

}
