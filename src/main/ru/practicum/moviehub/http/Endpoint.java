package ru.practicum.moviehub.http;

public enum Endpoint {
    GET_MOVIES, GET_MOVIE_ID, POST_MOVIES, DELETE_MOVIE_ID, GET_MOVIES_YEAR, UNKNOWN;

    public static Endpoint getEndpoint(String requestPath, String requestMethod, String query) {
        String[] pathParts = requestPath.split("/");

        if (pathParts.length == 2 && pathParts[1].equals("movies")) {
            if (requestMethod.equals("GET")) {
                if (query != null && query.startsWith("year=")) {
                    return Endpoint.GET_MOVIES_YEAR;
                } else {
                    return Endpoint.GET_MOVIES;
                }
            }
                if (requestMethod.equals("POST")) {
                    return Endpoint.POST_MOVIES;
                }
            }
            if (pathParts.length == 3 && pathParts[1].equals("movies")) {
                if (requestMethod.equals("GET")) {
                    return Endpoint.GET_MOVIE_ID;
                }
                if (requestMethod.equals("DELETE")) {
                    return Endpoint.DELETE_MOVIE_ID;
                }
            }
            return Endpoint.UNKNOWN;
        }
    }
