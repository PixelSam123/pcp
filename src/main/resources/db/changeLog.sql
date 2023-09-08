-- liquibase formatted sql

-- changeset pixelsam123:1
-- comment: initial tables creation
CREATE TABLE user
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    name          VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    points        INT DEFAULT 0
);

CREATE TABLE challenge
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    timestamp       TIMESTAMP DEFAULT NOW(),
    name            VARCHAR(255) NOT NULL UNIQUE,
    description     VARCHAR(255) NOT NULL,
    initial_code    VARCHAR(255) NOT NULL,
    test_case       VARCHAR(255) NOT NULL,
    tier            INT          NOT NULL,
    completed_count INT       DEFAULT 0,
    user_id         BIGINT       NOT NULL,
    CONSTRAINT fk__challenge__user
        FOREIGN KEY (user_id) REFERENCES user (id)
);

CREATE TABLE challenge_comment
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    content      VARCHAR(255) NOT NULL,
    user_id      BIGINT       NOT NULL,
    challenge_id BIGINT       NOT NULL,
    CONSTRAINT fk__challenge_comment__user
        FOREIGN KEY (user_id) REFERENCES user (id),
    CONSTRAINT fk__challenge_comment__challenge
        FOREIGN KEY (challenge_id) REFERENCES challenge (id)
);

CREATE TABLE challenge_vote
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    is_upvote    BOOL   NOT NULL,
    user_id      BIGINT NOT NULL,
    challenge_id BIGINT NOT NULL,
    CONSTRAINT fk__challenge_vote__user
        FOREIGN KEY (user_id) REFERENCES user (id),
    CONSTRAINT fk__challenge_vote__challenge
        FOREIGN KEY (challenge_id) REFERENCES challenge (id)
);

CREATE TABLE challenge_submission
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    code         VARCHAR(255) NOT NULL,
    user_id      BIGINT       NOT NULL,
    challenge_id BIGINT       NOT NULL,
    CONSTRAINT fk__challenge_submission__user
        FOREIGN KEY (user_id) REFERENCES user (id),
    CONSTRAINT fk__challenge_submission__challenge
        FOREIGN KEY (challenge_id) REFERENCES challenge (id)
);

CREATE TABLE challenge_submission_comment
(
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    content                 VARCHAR(255) NOT NULL,
    user_id                 BIGINT       NOT NULL,
    challenge_submission_id BIGINT       NOT NULL,
    CONSTRAINT fk__challenge_submission_comment__user
        FOREIGN KEY (user_id) REFERENCES user (id),
    CONSTRAINT fk__challenge_submission_comment__challenge_submission
        FOREIGN KEY (challenge_submission_id) REFERENCES challenge_submission (id)
);

CREATE TABLE challenge_submission_vote
(
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    is_upvote               BOOL   NOT NULL,
    user_id                 BIGINT NOT NULL,
    challenge_submission_id BIGINT NOT NULL,
    CONSTRAINT fk__challenge_submission_vote__user
        FOREIGN KEY (user_id) REFERENCES user (id),
    CONSTRAINT fk__challenge_submission_vote__challenge_submission
        FOREIGN KEY (challenge_submission_id) REFERENCES challenge_submission (id)
);
