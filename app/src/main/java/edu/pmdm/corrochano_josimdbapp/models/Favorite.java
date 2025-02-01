package edu.pmdm.corrochano_josimdbapp.models;

public class Favorite {
    private String idPelicula;
    private String idUsuario;
    private String nombrePelicula;
    private String portadaURL;

    // Constructor vacío necesario para Firestore
    public Favorite() {
    }

    public Favorite(String idPelicula, String idUsuario, String nombrePelicula, String portadaURL) {
        this.idPelicula = idPelicula;
        this.idUsuario = idUsuario;
        this.nombrePelicula = nombrePelicula;
        this.portadaURL = portadaURL;
    }

    // Getters y Setters
    public String getIdPelicula() {
        return idPelicula;
    }

    public void setIdPelicula(String idPelicula) {
        this.idPelicula = idPelicula;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombrePelicula() {
        return nombrePelicula;
    }

    public void setNombrePelicula(String nombrePelicula) {
        this.nombrePelicula = nombrePelicula;
    }

    public String getPortadaURL() {
        return portadaURL;
    }

    public void setPortadaURL(String portadaURL) {
        this.portadaURL = portadaURL;
    }
}
