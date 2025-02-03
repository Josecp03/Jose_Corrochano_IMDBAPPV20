package edu.pmdm.corrochano_josimdbapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import edu.pmdm.corrochano_josimdbapp.MovieDetailsActivity;
import edu.pmdm.corrochano_josimdbapp.R;
import edu.pmdm.corrochano_josimdbapp.database.DatabaseHelper;
import edu.pmdm.corrochano_josimdbapp.models.Favorite;
import edu.pmdm.corrochano_josimdbapp.models.Movie;
import edu.pmdm.corrochano_josimdbapp.sync.FavoritesSync;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    // Atributos
    private final Context context;
    private final List<Movie> movieList;
    private final String idUsuario;
    private final DatabaseHelper databaseHelper;
    private final boolean favoritos;

    // Constructor
    public MovieAdapter(Context context, List<Movie> movieList, String idUsuario, DatabaseHelper databaseHelper, boolean favoritos) {
        this.context = context;
        this.movieList = movieList;
        this.idUsuario = idUsuario;
        this.databaseHelper = databaseHelper;
        this.favoritos = favoritos;
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_movie, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {

        Movie movie = movieList.get(position);

        // Usar Glide para cargar la imagen con una predeterminada en caso de error
        Glide.with(context)
                .load(movie.getPosterPath()) // Cargar la portada de la película
                .error(R.mipmap.placeholderportada) // Imagen predeterminada en caso de error
                .into(holder.posterImageView);

        // Listener para cuando hago Click sobre una película
        holder.itemView.setOnClickListener(v -> {

            // Crear el intent que nos dirige a la actividad donde se muetsran los detalles
            Intent intent = new Intent(context, MovieDetailsActivity.class);

            // Pasarle el objeto película con todos sus detalles
            intent.putExtra("pelicula", movie);

            // Lanzar el intent
            context.startActivity(intent);

        });

        // Listener para cuando hago longClick sobre una película
        holder.itemView.setOnLongClickListener(v -> {

            // Comprobar la variable booleana para saber si la película sobre la que estoy pinchando está ya en favoritos o no
            if (!favoritos) {
                agregarFavorito(movie, holder.getAdapterPosition());
            } else {
                eliminarFavorito(movie, holder.getAdapterPosition());
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return movieList.size();
    }

    public static class MovieViewHolder extends RecyclerView.ViewHolder {
        ImageView posterImageView;
        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);

            // Inicialización de la ImageView utilizando findViewById para enlazarla con el ID definido en el layout XML
            posterImageView = itemView.findViewById(R.id.ImageViewPelicula);

        }
    }

    // Método para agregar a favoritos una película
    private void agregarFavorito(Movie movie, int position) {

        // Obtener una instancia de la base de datos en modo escritura
        SQLiteDatabase dbWrite = databaseHelper.getWritableDatabase();

        // Insertar la película en la tabla de favoritos mediante el método insertarFavorito del DatabaseHelper
        long result = databaseHelper.insertarFavorito(
                dbWrite,
                idUsuario,
                movie.getId(),
                movie.getTitle(),
                movie.getPosterPath()
        );

        // Cerrar la conexión a la base de datos
        dbWrite.close();

        // Comprobar si se ha realizado la operación para informar al usuario
        if (result != -1) {
            Toast.makeText(context, "Agregada a favoritos: " + movie.getTitle(), Toast.LENGTH_SHORT).show();

            // Sincronizamos añadiendo el favorito a Firestore
            Favorite favorite = new Favorite(movie.getId(), idUsuario, movie.getTitle(), movie.getPosterPath());
            FavoritesSync.addFavorite(context, favorite);

        } else {
            Toast.makeText(context, "La película ya está en favoritos", Toast.LENGTH_SHORT).show();
        }
    }

    // Método para eliminar de favoritos una película
    private void eliminarFavorito(Movie movie, int position) {

        // Obtener una instancia de la base de datos en modo escritura
        SQLiteDatabase dbWrite = databaseHelper.getWritableDatabase();

        // Eliminar la película de la tabla de favoritos
        int rowsDeleted = dbWrite.delete(
                DatabaseHelper.TABLE_FAVORITOS,
                "idUsuario=? AND idPelicula=?",
                new String[]{idUsuario, movie.getId()}
        );

        // Cerrar la conexión a la base de datos
        dbWrite.close();

        // Verificar si se ha realizado la operación para informar al usuario y realizar las operaciones necesarias
        if (rowsDeleted > 0) {

            Toast.makeText(context, movie.getTitle() + " eliminado de favoritos", Toast.LENGTH_SHORT).show();
            movieList.remove(position);
            notifyItemRemoved(position);

            // Sincronizamos eliminando el favorito de Firestore
            FavoritesSync.removeFavorite(context, idUsuario, movie.getId());

        } else {
            Toast.makeText(context, "Error al eliminar de favoritos.", Toast.LENGTH_SHORT).show();
        }
    }

}
