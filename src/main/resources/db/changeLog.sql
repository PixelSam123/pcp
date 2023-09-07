-- liquibase formatted sql

-- changeset pixelsam123:1
-- comment: initial tables creation
CREATE TABLE user
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    name          VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    points        INT DEFAULT 0
);

CREATE TABLE challenge
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    timestamp       TIMESTAMP DEFAULT NOW(),
    name            VARCHAR(255) UNIQUE,
    description     VARCHAR(255) NOT NULL,
    initial_code    VARCHAR(255) NOT NULL,
    test_case       VARCHAR(255) NOT NULL,
    tier            INT          NOT NULL,
    completed_count INT       DEFAULT 0,
    user_id         BIGINT,
    CONSTRAINT fk_user_challenge
        FOREIGN KEY (user_id) REFERENCES user (id)
);

CREATE TABLE challenge_comment
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    content      VARCHAR(255) NOT NULL,
    user_id      BIGINT,
    challenge_id BIGINT,
    CONSTRAINT fk_user_challenge_comment
        FOREIGN KEY (user_id) REFERENCES user (id),
    CONSTRAINT fk_challenge_challenge_comment
        FOREIGN KEY (challenge_id) REFERENCES challenge (id)
);

CREATE TABLE challenge_vote
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    is_upvote    BOOL NOT NULL,
    user_id      BIGINT,
    challenge_id BIGINT,
    CONSTRAINT fk_user_challenge_vote
        FOREIGN KEY (user_id) REFERENCES user (id),
    CONSTRAINT fk_challenge_challenge_vote
        FOREIGN KEY (challenge_id) REFERENCES challenge (id)
);

CREATE TABLE challenge_submission
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    code         VARCHAR(255) NOT NULL,
    user_id      BIGINT,
    challenge_id BIGINT,
    CONSTRAINT fk_user_challenge_submission
        FOREIGN KEY (user_id) REFERENCES user (id),
    CONSTRAINT fk_challenge_challenge_submission
        FOREIGN KEY (challenge_id) REFERENCES challenge (id)
);

CREATE TABLE challenge_submission_comment
(
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    content                 VARCHAR(255) NOT NULL,
    user_id                 BIGINT,
    challenge_submission_id BIGINT,
    CONSTRAINT fk_user_challenge_submission_comment
        FOREIGN KEY (user_id) REFERENCES user (id),
    CONSTRAINT fk_challenge_submission_challenge_submission_comment
        FOREIGN KEY (challenge_submission_id) REFERENCES challenge_submission (id)
);

CREATE TABLE challenge_submission_vote
(
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    is_upvote               BOOL NOT NULL,
    user_id                 BIGINT,
    challenge_submission_id BIGINT,
    CONSTRAINT fk_user_challenge_submission_vote
        FOREIGN KEY (user_id) REFERENCES user (id),
    CONSTRAINT fk_challenge_submission_challenge_submission_vote
        FOREIGN KEY (challenge_submission_id) REFERENCES challenge_submission (id)
);
