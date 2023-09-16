-- liquibase formatted sql

-- changeset pixelsam123:1
-- comment: initial tables creation
CREATE TABLE user
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    name          VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    points        INT          NOT NULL DEFAULT 0
);

CREATE TABLE challenge
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    timestamp       TIMESTAMP    NOT NULL DEFAULT NOW(),
    name            VARCHAR(255) NOT NULL UNIQUE,
    description     VARCHAR(255) NOT NULL,
    initial_code    VARCHAR(255) NOT NULL,
    test_case       VARCHAR(255) NOT NULL,
    tier            INT          NOT NULL,
    completed_count INT          NOT NULL DEFAULT 0,
    user_id         BIGINT       NOT NULL,
    KEY (user_id)
);

CREATE TABLE challenge_comment
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    content      VARCHAR(255) NOT NULL,
    user_id      BIGINT       NOT NULL,
    challenge_id BIGINT       NOT NULL,
    KEY (user_id),
    KEY (challenge_id)
);

CREATE TABLE challenge_vote
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    is_upvote    BOOL   NOT NULL,
    user_id      BIGINT NOT NULL,
    challenge_id BIGINT NOT NULL,
    KEY (user_id),
    KEY (challenge_id)
);

CREATE TABLE challenge_submission
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    code         VARCHAR(255) NOT NULL,
    user_id      BIGINT       NOT NULL,
    challenge_id BIGINT       NOT NULL,
    KEY (user_id),
    KEY (challenge_id)
);

CREATE TABLE challenge_submission_comment
(
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    content                 VARCHAR(255) NOT NULL,
    user_id                 BIGINT       NOT NULL,
    challenge_submission_id BIGINT       NOT NULL,
    KEY (user_id),
    KEY (challenge_submission_id)
);

CREATE TABLE challenge_submission_vote
(
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    is_upvote               BOOL   NOT NULL,
    user_id                 BIGINT NOT NULL,
    challenge_submission_id BIGINT NOT NULL,
    KEY (user_id),
    KEY (challenge_submission_id)
);

-- changeset pixelsam123:2
-- comment: enlarge some VARCHAR(255) types to TEXT
ALTER TABLE challenge
    MODIFY COLUMN description TEXT NOT NULL,
    MODIFY COLUMN initial_code TEXT NOT NULL,
    MODIFY COLUMN test_case TEXT NOT NULL;

ALTER TABLE challenge_comment
    MODIFY COLUMN content TEXT NOT NULL;

ALTER TABLE challenge_submission
    MODIFY COLUMN code TEXT NOT NULL;

ALTER TABLE challenge_submission_comment
    MODIFY COLUMN content TEXT NOT NULL;
