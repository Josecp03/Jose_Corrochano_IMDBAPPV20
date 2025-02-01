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
import edu.pmdm.corrochano_josimdbapp.database.FavoriteDatabaseHelper;
import edu.pmdm.corrochano_josimdbapp.models.Favorite;
import edu.pmdm.corrochano_josimdbapp.models.Movie;
import edu.pmdm.corrochano_josimdbapp.sync.FavoritesSync;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    // Atributos
    private final Context context;
    private final List<Movie> movieList;
    private final String idUsuario;
    private final FavoriteDatabaseHelper databaseHelper;
    private final boolean favoritos;

    // Constructor
    public MovieAdapter(Context context, List<Movie> movieList, String idUsuario, FavoriteDatabaseHelper databaseHelper, boolean favoritos) {
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

        Glide.with(context)
                .load(movie.getPosterPath())
                .error(R.mipmap.placeholderportada)
                .into(holder.posterImageView);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MovieDetailsActivity.class);
            intent.putExtra("pelicula", movie);
            context.startActivity(intent);
        });

        holder.itemView.setOnLongClickListener(v -> {
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
            posterImageView = itemView.findViewById(R.id.ImageViewPelicula);
        }
    }

    // Método para agregar a favoritos una película
    private void agregarFavorito(Movie movie, int position) {
        SQLiteDatabase dbWrite = databaseHelper.getWritableDatabase();

        long result = databaseHelper.insertarFavorito(
                dbWrite,
                idUsuario,
                movie.getId(),
                movie.getTitle(),
                movie.getPosterPath()
        );

        dbWrite.close();

        if (result != -1) {
            Toast.makeText(context, "Agregada a favoritos: " + movie.getTitle(), Toast.LENGTH_SHORT).show();
            // Sincronizamos añadiendo el favorito a Firestore
            Favorite favorite = new Favorite(movie.getId(), idUsuario, movie.getTitle(), movie.getPosterPath());
            FavoritesSync.addFavorite(context, favorite);
        } else {
            Toast.makeText(context, "Error al agregar a favoritos.", Toast.LENGTH_SHORT).show();
        }
    }

    // Método para eliminar de favoritos una película
    private void eliminarFavorito(Movie movie, int position) {
        SQLiteDatabase dbWrite = databaseHelper.getWritableDatabase();

        int rowsDeleted = dbWrite.delete(
                FavoriteDatabaseHelper.TABLE_FAVORITOS,
                "idUsuario=? AND idPelicula=?",
                new String[]{idUsuario, movie.getId()}
        );

        dbWrite.close();

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
