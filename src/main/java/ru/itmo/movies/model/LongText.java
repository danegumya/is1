package ru.itmo.movies.model;

import jakarta.json.bind.adapter.JsonbAdapter;

public class LongText implements JsonbAdapter<Long, String> {

    public String adaptToJson(Long value) {
        return value == null ? null : value.toString();
    }

    public Long adaptFromJson(String value) {
        return value == null ? null : Long.valueOf(value);
    }
}
