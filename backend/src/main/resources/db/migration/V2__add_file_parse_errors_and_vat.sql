alter table business_transactions
    add column vat_amount numeric(18, 2) not null default 0;

create table uploaded_file_parse_errors (
    id uuid primary key,
    uploaded_file_id uuid not null references uploaded_files(id),
    row_number integer not null,
    message varchar(500) not null,
    raw_data text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_uploaded_file_parse_errors_file_id on uploaded_file_parse_errors(uploaded_file_id);

