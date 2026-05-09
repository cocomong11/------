create table app_users (
    id uuid primary key,
    email varchar(320) not null unique,
    password_hash varchar(255) not null,
    name varchar(100) not null,
    status varchar(30) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table businesses (
    id uuid primary key,
    owner_id uuid not null references app_users(id),
    name varchar(150) not null,
    business_registration_number varchar(30),
    industry_name varchar(150),
    industry_group varchar(30) not null,
    professional_business boolean not null default false,
    opened_on date,
    previous_year_revenue numeric(18, 2),
    bookkeeping_type varchar(30) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_businesses_owner_id on businesses(owner_id);

create table uploaded_files (
    id uuid primary key,
    business_id uuid not null references businesses(id),
    original_filename varchar(255) not null,
    storage_key varchar(500) not null,
    content_type varchar(120),
    file_size_bytes bigint not null,
    checksum_sha256 varchar(64),
    processing_status varchar(30) not null,
    error_message varchar(500),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_uploaded_files_business_id on uploaded_files(business_id);

create table category_rules (
    id uuid primary key,
    business_id uuid not null references businesses(id),
    keyword varchar(150) not null,
    match_type varchar(30) not null,
    category_name varchar(100) not null,
    transaction_type varchar(30),
    requires_review boolean not null default false,
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_category_rules_business_id on category_rules(business_id);
create index idx_category_rules_keyword on category_rules(keyword);

create table business_transactions (
    id uuid primary key,
    business_id uuid not null references businesses(id),
    uploaded_file_id uuid references uploaded_files(id),
    transaction_date date not null,
    merchant_name varchar(255),
    description varchar(500),
    amount numeric(18, 2) not null,
    transaction_type varchar(30) not null,
    category_name varchar(100),
    classification_status varchar(30) not null,
    evidence_status varchar(30) not null,
    source_row_number integer,
    raw_data text,
    user_memo varchar(500),
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_business_transactions_business_id on business_transactions(business_id);
create index idx_business_transactions_file_id on business_transactions(uploaded_file_id);
create index idx_business_transactions_date on business_transactions(transaction_date);
create index idx_business_transactions_classification on business_transactions(classification_status);

create table ledger_entries (
    id uuid primary key,
    business_id uuid not null references businesses(id),
    transaction_id uuid unique references business_transactions(id),
    entry_date date not null,
    account_title varchar(100) not null,
    summary varchar(500),
    revenue_amount numeric(18, 2) not null default 0,
    expense_amount numeric(18, 2) not null default 0,
    payment_method varchar(100),
    evidence_status varchar(30) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_ledger_entries_business_id on ledger_entries(business_id);
create index idx_ledger_entries_entry_date on ledger_entries(entry_date);

create table tax_reports (
    id uuid primary key,
    business_id uuid not null references businesses(id),
    period_type varchar(30) not null,
    period_start date not null,
    period_end date not null,
    total_revenue numeric(18, 2) not null default 0,
    total_expense numeric(18, 2) not null default 0,
    net_income numeric(18, 2) not null default 0,
    unclassified_transaction_count integer not null default 0,
    generated_at timestamptz not null default now(),
    notice varchar(255) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_tax_reports_business_period on tax_reports(business_id, period_type, period_start, period_end);

create table checklist_items (
    id uuid primary key,
    business_id uuid not null references businesses(id),
    transaction_id uuid references business_transactions(id),
    item_type varchar(40) not null,
    status varchar(30) not null,
    severity varchar(30) not null,
    title varchar(150) not null,
    description varchar(1000),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_checklist_items_business_id on checklist_items(business_id);
create index idx_checklist_items_status on checklist_items(status);

