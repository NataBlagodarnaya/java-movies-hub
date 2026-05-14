package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;


public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore moviesStore;
    private final Gson gson = new Gson();

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        Endpoint endpoint = Endpoint.getEndpoint(ex.getRequestURI().getPath(), ex.getRequestMethod(),
                ex.getRequestURI().getQuery());

        switch (endpoint) {
            case GET_MOVIES: {
                handleGetMovies(ex);
                break;
            }
            case GET_MOVIE_ID: {
                handleGetMovieById(ex);
                break;
            }
            case POST_MOVIES: {
                handlePostMovies(ex);
                break;
            }
            case DELETE_MOVIE_ID: {
                handleDeleteMovieById(ex);
                break;
            }
            case GET_MOVIES_YEAR: {
                handleGetMoviesByYear(ex);
                break;
            }
            default:
                sendJson(ex, 405, gson.toJson(new ErrorResponse("Неверный запрос.",
                        "Такой метод не поддерживается.")));
        }
    }


    private void handleGetMovies(HttpExchange ex) throws IOException {
        String json = gson.toJson(moviesStore.getMovies().values());
        sendJson(ex, 200, json);
    }

    private void handleGetMovieById(HttpExchange ex) throws IOException {
        String[] pathParts = ex.getRequestURI().getPath().split("/");
        int currentId;
        try {
            currentId = Integer.parseInt(pathParts[2]);
        } catch (NumberFormatException | NullPointerException e) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse("Ошибка Id.",
                    "Формат запроса не соответствует ожидаемому - Id фильма не является целым числом.")));
            return;
        }
        if (moviesStore.getMovies().containsKey(currentId)) {
            String json = gson.toJson(moviesStore.getMovies().get(currentId));
            sendJson(ex, 200, json);
        } else {
            sendJson(ex, 404, gson.toJson(new ErrorResponse("Неверный Id.",
                    "Фильма с Id " + currentId + " нет в списке доступных.")));
        }
    }

    private void handlePostMovies(HttpExchange ex) throws IOException {
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");
        if (!contentType.equalsIgnoreCase("application/json; charset=UTF-8")) {
            ErrorResponse errorResponse = new ErrorResponse(
                    "Ошибка Content-Type запроса.",
                    "Ожидается application/json; charset=UTF-8, а в запросе - " + contentType);
            String jsonError = gson.toJson(errorResponse);
            sendJson(ex, 415, jsonError);
            return;
        }
        InputStream is = ex.getRequestBody();
        try {
            String reqBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Movie movie = gson.fromJson(reqBody, Movie.class);

            if (movie.getTitle() == null ||
                    movie.getTitle().length() > 100 ||
                    movie.getYear() < 1888 ||
                    movie.getYear() > 2026) {
                ErrorResponse errorResponse = new ErrorResponse(
                        "Ошибка валидации.",
                        "Название не должно быть пустым или содержать более 100 символов," +
                                " год должен быть между 1888 и 2026");
                String jsonError = gson.toJson(errorResponse);
                sendJson(ex, 422, jsonError);
                return;
            }
            moviesStore.addMovie(movie);
            String json = gson.toJson(moviesStore.getMovies().values());
            sendJson(ex, 201, json);
        } catch (JsonSyntaxException e) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse("Ошибка формата.",
                    "Формат запроса не соответствует ожидаемому (JSON).")));
        }
    }

    private void handleDeleteMovieById(HttpExchange ex) throws IOException {
        String[] pathParts = ex.getRequestURI().getPath().split("/");
        int currentId;
        try {
            currentId = Integer.parseInt(pathParts[2]);
        } catch (NumberFormatException | NullPointerException e) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse("Ошибка Id.",
                    "Формат запроса не соответствует ожидаемому - Id фильма не является целым числом.")));
            return;
        }
        if (moviesStore.getMovies().containsKey(currentId)) {
            moviesStore.getMovies().remove(currentId);
            sendNoContent(ex);
        } else {
            sendJson(ex, 404, gson.toJson(new ErrorResponse("Неверный Id.",
                    "Фильм с Id " + currentId + " не найден, возможно был удален ранее.")));
        }
    }

    private void handleGetMoviesByYear(HttpExchange ex) throws IOException {
        String query = ex.getRequestURI().getQuery();
        if ( query == null || query.isEmpty()) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse("Ошибка запроса.",
                    "Формат запроса не соответствует ожидаемому GET/movies/?year=YYYY")));
         return;
        }
        String stringCurrentYear = query.replaceFirst("year=", "").trim();
        if (stringCurrentYear.isEmpty()) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse("Ошибка запроса.",
                    "Формат запроса не соответствует ожидаемому GET/movies/?year=YYYY")));
            return;
        }
        int currentYear;
        try {
            currentYear = Integer.parseInt(stringCurrentYear);
        } catch (NullPointerException | NumberFormatException e) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse("Ошибка в годе.",
                    "Некорректный параметр запроса — 'year'. Год должен быть целым числом между 1888 и 2026.")));
            return;
        }
        if (currentYear < 1888 || currentYear > 2026 ) {
            ErrorResponse errorResponse = new ErrorResponse(
                    "Ошибка в годе.",
                    "Некорректный параметр запроса — 'year'. Год должен быть целым числом между 1888 и 2026.");
            String jsonError = gson.toJson(errorResponse);
            sendJson(ex, 400, jsonError);
            return;
        }
        List<Movie> moviesByYear = moviesStore.getMovies().values().stream()
                .filter(movie -> movie.getYear() == currentYear)
                .toList();
        String json = gson.toJson(moviesByYear);
        sendJson(ex, 200, json);
    }
}