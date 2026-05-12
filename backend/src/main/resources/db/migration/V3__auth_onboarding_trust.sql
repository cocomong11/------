alter table app_users
    alter column status set default 'PENDING_EMAIL_VERIFICATION',
    add column email_verified_at timestamptz,
    add column failed_login_count integer not null default 0,
    add column locked_until timestamptz,
    add column last_login_at timestamptz;

update app_users
set email_verified_at = coalesce(email_verified_at, now())
where status = 'ACTIVE';

alter table businesses
    add column representative_name varchar(100),
    add column taxation_type varchar(50),
    add column has_employees boolean not null default false,
    add column verification_status varchar(30) not null default 'NOT_STARTED';

create table agreements (
    id uuid primary key,
    user_id uuid not null references app_users(id),
    terms_version varchar(30) not null,
    privacy_version varchar(30) not null,
    business_info_consent_version varchar(30) not null,
    tax_data_consent_version varchar(30) not null,
    reference_notice_version varchar(30) not null,
    agreed_at timestamptz not null,
    ip_address varchar(64),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_agreements_user_id on agreements(user_id);

create table email_verification_codes (
    id uuid primary key,
    user_id uuid not null references app_users(id),
    code varchar(6) not null,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_email_verification_codes_user_id on email_verification_codes(user_id);

create table refresh_tokens (
    id uuid primary key,
    user_id uuid not null references app_users(id),
    token_hash varchar(64) not null unique,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    replaced_by_hash varchar(64),
    ip_address varchar(64),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_refresh_tokens_user_id on refresh_tokens(user_id);

create table password_reset_tokens (
    id uuid primary key,
    user_id uuid not null references app_users(id),
    token_hash varchar(64) not null unique,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_password_reset_tokens_user_id on password_reset_tokens(user_id);
