CREATE TABLE course (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug varchar(100) NOT NULL UNIQUE,
    title varchar(200) NOT NULL,
    summary varchar(500) NOT NULL,
    published_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lesson (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    course_id bigint NOT NULL REFERENCES course (id),
    slug varchar(100) NOT NULL,
    title varchar(200) NOT NULL,
    position integer NOT NULL CHECK (position > 0),
    body_markdown text NOT NULL,
    published_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (course_id, slug),
    UNIQUE (course_id, position)
);
