package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new HashMap<>();
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

    public List<Movie> getMoviesByYear(int currentYear) {
        return getMovies().values().stream()
                .filter(movie -> movie.getYear() == currentYear)
                .toList();
    }
}