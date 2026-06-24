-- V2: Add 'active' boolean column to domains table
-- WHY: Domains can be soft-deleted (set active=false) without losing data.
-- The DomainRouter only searches active domains, so deactivated domains
-- are effectively invisible to the routing engine but preserved for auditing.

ALTER TABLE domains ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
