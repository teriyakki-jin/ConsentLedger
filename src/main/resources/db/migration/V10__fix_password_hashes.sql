-- Fix password hashes (previous hashes were incorrect)
-- admin1234 -> BCrypt hash
UPDATE users
SET password_hash = '$2a$10$WWj9gVRdcDb/sVH9Bmwp5eKRBz2CrM9THomHcoNEY4D31WucFI4pi'
WHERE email = 'admin@consentledger.com';

-- user1234 -> BCrypt hash
UPDATE users
SET password_hash = '$2a$10$Yx7jIFQdOjNOHm3ANUbKJ.rYR4TGX8PkV0PT5SjuU1J0O9ACXzuYi'
WHERE email = 'user@consentledger.com';
