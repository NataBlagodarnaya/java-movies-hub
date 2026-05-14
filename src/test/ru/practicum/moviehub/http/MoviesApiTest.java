package ru.practicum.moviehub.http;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore moviesStore = new MoviesStore();
    private final Gson gson = new Gson();

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer(moviesStore, 8080);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

    }

    @BeforeEach
    void beforeEach() {
        moviesStore.cleanStore();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    private void assertJsonContentType(HttpResponse resp) {
        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
    }

    private JsonObject getErrorResponse(HttpResponse<String> resp) {
        String body = resp.body().trim();
        JsonElement jsonElement = JsonParser.parseString(body);
        assertTrue(jsonElement.isJsonObject(), "Ожидается Json объект");
        return jsonElement.getAsJsonObject();
    }

    //возвращает пустой список, если нет фильмов
    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        assertJsonContentType(resp);

        String body = resp.body().trim();
        assertEquals("[]", body, "Ожидается JSON-массив");
    }

    //возвращает список с ранее добавленными фильмами
    @Test
    void getMovies_returnsArrayOfMovies() throws Exception {
        Movie movie = new Movie("Пираты", 1891);
        moviesStore.addMovie(movie);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        assertJsonContentType(resp);

        String body = resp.body().trim();
        //проверим что в ответе вернулся именно список с объектом Movie
        List<Movie> currentMovies = gson.fromJson(body, new ListOfMoviesTypeToken().getType());
        assertFalse(currentMovies.isEmpty(), "Ожидается что список не пустой");
        assertTrue(currentMovies.size() == 1, "Ожидается 1 элемент в списке");
        assertEquals("Пираты", currentMovies.get(0).getTitle(), "Название");
        assertEquals(1891, currentMovies.get(0).getYear(), "Год");
        assertEquals(1, currentMovies.get(0).getId(), "Id фильма");
    }

    //добавляет фильм при корректных данных
    @Test
    void postMovie_ifCorrect() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(new Movie("Золушка", 1991))))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");

        assertJsonContentType(resp);

        //проверим что фильм добавился в хранилище на сервере
        assertTrue(moviesStore.getMovies().size() == 1, "Ожидается 1 элемент в списке");
        assertEquals("Золушка", moviesStore.getMovies().get(1).getTitle(), "Название");
        assertEquals(1991, moviesStore.getMovies().get(1).getYear(), "Год");
        assertEquals(1, moviesStore.getMovies().get(1).getId(), "Id фильма");
    }

    // возвращает ошибку при пустом title
    @Test
    void returnError_ifEmptyTitle() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(new Movie(null, 2000))))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        JsonObject jsonObject = getErrorResponse(resp);
        assertEquals("Ошибка валидации.", jsonObject.get("error").getAsString(), "Ошибка валидации");
        assertEquals("Название не должно быть пустым или содержать более 100 символов," +
                        " год должен быть между 1888 и 2026",
                jsonObject.get("details").getAsString(), "Ошибка валидации");
    }

    // возвращает ошибку при слишком длинном title (> 100 символов);
    @Test
    void returnError_ifBigTitle() throws Exception {
        String title = "t".repeat(101);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(new Movie(title, 2005))))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        JsonObject jsonObject = getErrorResponse(resp);
        assertEquals("Ошибка валидации.", jsonObject.get("error").getAsString(), "Ошибка валидации");
        assertEquals("Название не должно быть пустым или содержать более 100 символов," +
                        " год должен быть между 1888 и 2026",
                jsonObject.get("details").getAsString(), "Ошибка валидации");
    }

    // возвращает ошибку при неверном year (меньше 1888 или больше текущего года + 1)
    @Test
    void returnError_ifWrongYear() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(new Movie("Эффект бабочки", 1750))))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        JsonObject jsonObject = getErrorResponse(resp);
        assertEquals("Ошибка валидации.", jsonObject.get("error").getAsString(), "Ошибка валидации");
        assertEquals("Название не должно быть пустым или содержать более 100 символов," +
                        " год должен быть между 1888 и 2026",
                jsonObject.get("details").getAsString(), "Ошибка валидации");
    }

    // возвращает ошибку при неправильном Content-Type
    @Test
    void returnError_ifWrongContentType() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/html; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(new Movie("Эффект бабочки", 2000))))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, resp.statusCode(), "POST /movies должен вернуть 415");

        JsonObject jsonObject = getErrorResponse(resp);
        assertEquals("Ошибка Content-Type запроса.", jsonObject.get("error").getAsString(),
                "Ошибка Content-Type запроса.");
        assertEquals("Ожидается application/json; charset=UTF-8, а в запросе - text/html; charset=UTF-8",
                jsonObject.get("details").getAsString(), "Ошибка Content-Type запроса.");
    }

    // возвращает ошибку при некорректном JSON.
    @Test
    void returnError_ifWrongJSON() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString("{ movieName: Золушка, year: 1982 "))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "POST /movies должен вернуть 400");

        JsonObject jsonObject = getErrorResponse(resp);
        assertEquals("Ошибка формата.", jsonObject.get("error").getAsString(), "Ошибка формата.");
        assertEquals("Формат запроса не соответствует ожидаемому (JSON).",
                jsonObject.get("details").getAsString(), "Ошибка формата.");
    }

    //возвращает фильм по существующему id;
    @Test
    void getMovieId_returnsMovieById() throws Exception {
        Movie movie = new Movie("Пираты", 1891);
        moviesStore.addMovie(movie);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies/{id} должен вернуть 200");

        assertJsonContentType(resp);

        String body = resp.body().trim();
        //проверим что в ответе вернулся именно объект Movie
        Movie respMovie = gson.fromJson(body, Movie.class);
        assertEquals("Пираты", respMovie.getTitle(), "Название");
        assertEquals(1891, respMovie.getYear(), "Год");
        assertEquals(1, respMovie.getId(), "Id фильма");
    }

    //возвращает ошибку, если фильм не найден;
    @Test
    void getMovieID_returnsError_ifNoMovieId() throws Exception {
        Movie movie = new Movie("Пираты", 1891);
        moviesStore.addMovie(movie);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/2"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode(), "GET /movies/{id} должен вернуть 404");

        JsonObject jsonObject = getErrorResponse(resp);
        assertEquals("Неверный Id.", jsonObject.get("error").getAsString(), "Неверный Id.");
        assertEquals("Фильма с Id 2 нет в списке доступных.",
                jsonObject.get("details").getAsString(), "Нет фильма с таким Id");
    }

    //возвращает ошибку, если id не число.
    @Test
    void getMovieID_returnsError_ifIncorrectIdFormat() throws Exception {
        Movie movie = new Movie("Пираты", 1891);
        moviesStore.addMovie(movie);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/b"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "GET /movies/{id} должен вернуть 400");

        JsonObject jsonObject = getErrorResponse(resp);
        assertEquals("Ошибка Id.", jsonObject.get("error").getAsString(), "Ошибка Id.");
        assertEquals("Формат запроса не соответствует ожидаемому - Id фильма не является целым числом.",
                jsonObject.get("details").getAsString(), "Ошибка формата Id");
    }

    //удаляет фильм по существующему id
    @Test
    void deleteMovieById() throws Exception {
        Movie movie = new Movie("Пираты", 1891);
        moviesStore.addMovie(movie);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, resp.statusCode(), "DELETE/movies/{id} должен вернуть 204");

        assertJsonContentType(resp);

        String body = resp.body().trim();
        //проверим что фильм удалился
        assertTrue(body.isEmpty(), "Ожидается пустое тело ответа.");
        assertFalse(moviesStore.getMovies().containsKey(1));
    }

    //возвращает ошибку, если фильм не найден
    @Test
    void deleteMovieID_returnsError_ifNoMovieId() throws Exception {
        Movie movie = new Movie("Пираты", 1891);
        moviesStore.addMovie(movie);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/2"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode(), "DELETE/movies/{id} должен вернуть 404");

        JsonObject jsonObject = getErrorResponse(resp);
        assertEquals("Неверный Id.", jsonObject.get("error").getAsString(), "Неверный Id.");
        assertEquals("Фильма с Id 2 нет в списке доступных.",
                jsonObject.get("details").getAsString(), "Нет фильма с таким Id");
    }

    //возвращает ошибку, если id не число.
    @Test
    void deleteMovieID_returnsError_ifIncorrectIdFormat() throws Exception {
        Movie movie = new Movie("Пираты", 1891);
        moviesStore.addMovie(movie);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/b"))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "DELETE/movies/{id} должен вернуть 400");

        JsonObject jsonObject = getErrorResponse(resp);
        assertEquals("Ошибка Id.", jsonObject.get("error").getAsString(), "Ошибка Id.");
        assertEquals("Формат запроса не соответствует ожидаемому - Id фильма не является целым числом.",
                jsonObject.get("details").getAsString(), "Ошибка формата Id");
    }

    //возвращает фильмы указанного года
    @Test
    void getMoviesByYear_returnsArrayOfMovies() throws Exception {
        Movie movie = new Movie("Пираты", 1891);
        moviesStore.addMovie(movie);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1891"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET/movies/?year=YYYY должен вернуть 200");

        assertJsonContentType(resp);

        String body = resp.body().trim();
        //проверим что в ответе вернулся именно список с объектом Movie
        List<Movie> currentMovies = gson.fromJson(body, new ListOfMoviesTypeToken().getType());
        assertFalse(currentMovies.isEmpty(), "Ожидается что список не пустой");
        assertTrue(currentMovies.size() == 1, "Ожидается 1 элемент в списке");
        assertEquals("Пираты", currentMovies.get(0).getTitle(), "Название");
        assertEquals(1891, currentMovies.get(0).getYear(), "Год");
        assertEquals(1, currentMovies.get(0).getId(), "Id фильма");
    }

    //возвращает пустой список, если фильмов с таким годом нет
    @Test
    void getMoviesByYear_whenNoSuch_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1891"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET/movies/?year=YYYY должен вернуть 200");

        assertJsonContentType(resp);

        String body = resp.body().trim();
        assertEquals("[]", body, "Ожидается JSON-массив");
    }

    //возвращает ошибку, если параметр year не число
    @Test
    void getMoviesByYear_returnsError_ifIncorrectYearFormat() throws Exception {
        Movie movie = new Movie("Пираты", 1891);
        moviesStore.addMovie(movie);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=uhyg"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "GET/movies/?year=YYYY должен вернуть 400");

        JsonObject jsonObject = getErrorResponse(resp);
        assertEquals("Ошибка в годе.", jsonObject.get("error").getAsString(), "Ошибка в годе.");
        assertEquals("Некорректный параметр запроса — 'year'. Год должен быть целым числом между 1888 и 2026.",
                jsonObject.get("details").getAsString(), "Ошибка в формате года.");
    }

    @Test
    void returnsError_ifIncorrectMethod() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(new Movie("Золушка", 1991))))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(405, resp.statusCode(), "PUT/movies должен вернуть 405");

        JsonObject jsonObject = getErrorResponse(resp);
        assertEquals("Неверный запрос.", jsonObject.get("error").getAsString(), "Неверный запрос.");
        assertEquals("Такой метод не поддерживается.",
                jsonObject.get("details").getAsString(), "Такой метод не поддерживается.");
    }
}