BEGIN;
SET search_path TO s468127;
CREATE TABLE IF NOT EXISTS ml_location (
    id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY CHECK (id > 0),
    x real NOT NULL CHECK (x > '-Infinity'::real AND x < 'Infinity'::real),
    y real NOT NULL CHECK (y > '-Infinity'::real AND y < 'Infinity'::real),
    z double precision NOT NULL CHECK (z > '-Infinity'::float8 AND z < 'Infinity'::float8)
);
CREATE TABLE IF NOT EXISTS ml_coordinates (
    id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY CHECK (id > 0),
    x integer NOT NULL CHECK (x > -299),
    y bigint NOT NULL CHECK (y <= 40)
);
CREATE TABLE IF NOT EXISTS ml_person (
    id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY CHECK (id > 0),
    name text NOT NULL CHECK (length(btrim(name)) > 0),
    eye_color varchar(20) NOT NULL CHECK (eye_color IN ('RED', 'BLACK', 'BROWN')),
    hair_color varchar(20) NOT NULL CHECK (hair_color IN ('RED', 'BLACK', 'BROWN')),
    location_id integer REFERENCES ml_location(id),
    height double precision CHECK (height > 0 AND height < 'Infinity'::float8),
    nationality varchar(30) CHECK (nationality IN ('UNITED_KINGDOM', 'GERMANY', 'FRANCE', 'THAILAND'))
);
CREATE TABLE IF NOT EXISTS ml_movie (
    id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY CHECK (id > 0),
    version bigint NOT NULL DEFAULT 0,
    name text NOT NULL CHECK (length(btrim(name)) > 0),
    coordinates_id integer NOT NULL REFERENCES ml_coordinates(id),
    creation_date timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    oscars_count bigint CHECK (oscars_count > 0),
    budget integer NOT NULL CHECK (budget > 0),
    total_box_office integer NOT NULL CHECK (total_box_office > 0),
    mpaa_rating varchar(20) NOT NULL CHECK (mpaa_rating IN ('PG', 'R', 'NC_17')),
    director_id integer REFERENCES ml_person(id),
    screenwriter_id integer REFERENCES ml_person(id),
    operator_id integer NOT NULL REFERENCES ml_person(id),
    length bigint NOT NULL CHECK (length > 0),
    golden_palm_count bigint CHECK (golden_palm_count > 0),
    genre varchar(20) NOT NULL CHECK (genre IN ('ACTION', 'WESTERN', 'TRAGEDY', 'THRILLER'))
);
CREATE INDEX IF NOT EXISTS movie_coordinates ON ml_movie(coordinates_id);
CREATE INDEX IF NOT EXISTS movie_director ON ml_movie(director_id);
CREATE INDEX IF NOT EXISTS movie_screenwriter ON ml_movie(screenwriter_id);
CREATE INDEX IF NOT EXISTS movie_operator ON ml_movie(operator_id);
CREATE INDEX IF NOT EXISTS person_location ON ml_person(location_id);
CREATE TABLE IF NOT EXISTS ml_app_user (
    id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY CHECK (id > 0),
    name varchar(40) NOT NULL UNIQUE CHECK (name ~ '^[a-zA-Z0-9_]{3,40}$'),
    salt varchar(100) NOT NULL,
    password_hash varchar(100) NOT NULL
);
CREATE OR REPLACE FUNCTION s468127.ml_average_palms() RETURNS numeric
LANGUAGE sql STABLE AS $$
    SELECT avg(golden_palm_count)
    FROM s468127.ml_movie
$$;
CREATE OR REPLACE FUNCTION s468127.ml_minimum_movie() RETURNS SETOF s468127.ml_movie
LANGUAGE sql STABLE AS $$
    SELECT * FROM s468127.ml_movie
    ORDER BY id
    LIMIT 1
$$;
CREATE OR REPLACE FUNCTION s468127.ml_genres_before(value text) RETURNS SETOF s468127.ml_movie
LANGUAGE plpgsql STABLE AS $$
BEGIN
    IF value IS NULL OR value NOT IN ('ACTION', 'WESTERN', 'TRAGEDY', 'THRILLER') THEN
        RAISE EXCEPTION 'Unknown genre' USING ERRCODE = '22023';
    END IF;
    RETURN QUERY
    SELECT * FROM s468127.ml_movie
    WHERE array_position(ARRAY['ACTION','WESTERN','TRAGEDY','THRILLER'], genre)
        < array_position(ARRAY['ACTION','WESTERN','TRAGEDY','THRILLER'], value)
    ORDER BY id;
END $$;
CREATE OR REPLACE FUNCTION s468127.ml_without_oscars() RETURNS SETOF s468127.ml_movie
LANGUAGE sql STABLE AS $$
    SELECT * FROM s468127.ml_movie
    WHERE oscars_count IS NULL
    ORDER BY id
$$;
CREATE OR REPLACE FUNCTION s468127.ml_award_oscars(min_length bigint, amount bigint) RETURNS integer
LANGUAGE plpgsql AS $$
DECLARE affected integer;
BEGIN
    IF min_length IS NULL OR min_length < 0 OR amount IS NULL OR amount <= 0 THEN
        RAISE EXCEPTION 'Invalid award parameters' USING ERRCODE = '22023';
    END IF;
    UPDATE s468127.ml_movie
    SET oscars_count = coalesce(oscars_count, 0) + amount,
        version = version + 1
    WHERE length > min_length;
    GET DIAGNOSTICS affected = ROW_COUNT;
    RETURN affected;
END $$;
COMMIT;
