create table notification_instance
(
    uuid uuid not null,
    name varchar(255) not null,
    -- email address from which to send notifications
    email_from varchar(255),
    -- subject of notification email
    subject varchar(255),
    -- base64 encoded content template
    content_template text not null,
    metadata text,
    attributes text,
    primary key (uuid)
);
