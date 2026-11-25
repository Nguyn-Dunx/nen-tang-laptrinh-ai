CREATE TABLE account (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email varchar(254) NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL,
    role varchar(20) NOT NULL CHECK (role IN ('LEARNER', 'AUTHOR', 'ADMIN')),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Spring Session JDBC schema. The principal column accommodates the full account email.
CREATE TABLE spring_session (
    primary_id char(36) PRIMARY KEY,
    session_id char(36) NOT NULL UNIQUE,
    creation_time bigint NOT NULL,
    last_access_time bigint NOT NULL,
    max_inactive_interval integer NOT NULL,
    expiry_time bigint NOT NULL,
    principal_name varchar(254)
);

CREATE INDEX spring_session_expiry_time_idx ON spring_session (expiry_time);
CREATE INDEX spring_session_principal_name_idx ON spring_session (principal_name);

CREATE TABLE spring_session_attributes (
    session_primary_id char(36) NOT NULL REFERENCES spring_session (primary_id) ON DELETE CASCADE,
    attribute_name varchar(200) NOT NULL,
    attribute_bytes bytea NOT NULL,
    PRIMARY KEY (session_primary_id, attribute_name)
);
