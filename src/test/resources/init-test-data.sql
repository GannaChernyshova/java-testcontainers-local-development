CREATE TABLE IF NOT EXISTS products
(
    id          bigserial primary key,
    code        varchar not null unique,
    name        varchar not null,
    description varchar,
    image       varchar,
    price       numeric not null
);

insert into products(code, name, description, image, price) values
('P201','Product P201','Product P201 description', null, 14.0),
('P202','Product P202','Product P202 description', null, 19.0)
;