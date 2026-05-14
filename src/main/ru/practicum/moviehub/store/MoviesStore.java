package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.HashMap;
import java.util.Map;

public class MoviesStore {
    private Map<Integer, Movie> movies = new HashMap<>();
    private int nextId = 1;

    public Map<Integer, Movie> getMovies() {
        return movies;
    }

    public void addMovie(Movie movie) {
        movie.setId(nextId++);
        movies.put(movie.getId(), movie);
    }

    public void cleanStore() {
        movies.clear();
        nextId = 1;
    }
}