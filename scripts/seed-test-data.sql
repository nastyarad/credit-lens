-- Local development only. This script seeds fictional Credit Lens test data.

BEGIN;

-- Remove only rows owned by this seed, in foreign-key order.
DELETE FROM credit_extract
WHERE financing_request_id IN (
    '10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000004',
    '10000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000006',
    '10000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000008',
    '10000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000010',
    '10000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000012',
    '10000000-0000-0000-0000-000000000013', '10000000-0000-0000-0000-000000000014'
);

DELETE FROM financing_request
WHERE id IN (
    '10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000004',
    '10000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000006',
    '10000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000008',
    '10000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000010',
    '10000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000012',
    '10000000-0000-0000-0000-000000000013', '10000000-0000-0000-0000-000000000014'
);

DELETE FROM consumer
WHERE id IN (
    '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002',
    '20000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000004'
);

WITH clock AS (SELECT transaction_timestamp() AS ts)
INSERT INTO consumer (id, personal_identity_code, created_at)
SELECT v.id, v.personal_identity_code, clock.ts + v.created_offset
FROM clock
CROSS JOIN (VALUES
    ('20000000-0000-0000-0000-000000000001'::uuid, '010101-001A', '-90 days'::interval),
    ('20000000-0000-0000-0000-000000000002'::uuid, '020202+002B', '-80 days'::interval),
    ('20000000-0000-0000-0000-000000000003'::uuid, '030303-003C', '-70 days'::interval),
    ('20000000-0000-0000-0000-000000000004'::uuid, '040404-004D', '-60 days'::interval)
) AS v(id, personal_identity_code, created_offset);

WITH clock AS (SELECT transaction_timestamp() AS ts)
INSERT INTO financing_request
    (id, consumer_id, client_request_id, extract_purposes, requested_at, completed_at)
SELECT v.id, v.consumer_id, v.client_request_id, v.extract_purposes,
       clock.ts + v.requested_offset, clock.ts + v.completed_offset
FROM clock
CROSS JOIN (VALUES
    ('10000000-0000-0000-0000-000000000001'::uuid, '20000000-0000-0000-0000-000000000001'::uuid, '30000000-0000-0000-0000-000000000001'::uuid, ARRAY['NewConsumerCredit']::varchar[], '-30 days'::interval, '-30 days'::interval + interval '2 seconds'),
    ('10000000-0000-0000-0000-000000000002'::uuid, '20000000-0000-0000-0000-000000000001'::uuid, '30000000-0000-0000-0000-000000000002'::uuid, ARRAY['NewLoan','GuaranteeOrThirdPartyPledge']::varchar[], '-20 days'::interval, '-20 days'::interval + interval '3 seconds'),
    ('10000000-0000-0000-0000-000000000003'::uuid, '20000000-0000-0000-0000-000000000001'::uuid, '30000000-0000-0000-0000-000000000003'::uuid, ARRAY['IncreaseOfConsumerCreditPrincipalOrCreditLimit']::varchar[], '-10 days'::interval, '-10 days'::interval + interval '4 seconds'),
    ('10000000-0000-0000-0000-000000000004'::uuid, '20000000-0000-0000-0000-000000000001'::uuid, '30000000-0000-0000-0000-000000000004'::uuid, ARRAY['ChangesToTermsOfConsumerCredit','NewConsumerCredit']::varchar[], '-5 days'::interval, '-5 days'::interval + interval '5 seconds'),
    ('10000000-0000-0000-0000-000000000005'::uuid, '20000000-0000-0000-0000-000000000001'::uuid, '30000000-0000-0000-0000-000000000005'::uuid, ARRAY['NewLoan']::varchar[], '-2 days'::interval, '-2 days'::interval + interval '6 seconds'),
    ('10000000-0000-0000-0000-000000000006'::uuid, '20000000-0000-0000-0000-000000000001'::uuid, '30000000-0000-0000-0000-000000000006'::uuid, ARRAY['ChangesToTerms']::varchar[], '-1 day'::interval - interval '1 second', '-1 day'::interval),
    ('10000000-0000-0000-0000-000000000007'::uuid, '20000000-0000-0000-0000-000000000001'::uuid, '30000000-0000-0000-0000-000000000007'::uuid, ARRAY['NewConsumerCredit']::varchar[], '-1 day'::interval, '-1 day'::interval + interval '1 second'),
    ('10000000-0000-0000-0000-000000000008'::uuid, '20000000-0000-0000-0000-000000000001'::uuid, '30000000-0000-0000-0000-000000000008'::uuid, ARRAY['IncreaseOfLoanPrincipalOrCreditLimit']::varchar[], '-12 hours'::interval, '-12 hours'::interval + interval '2 seconds'),
    ('10000000-0000-0000-0000-000000000009'::uuid, '20000000-0000-0000-0000-000000000002'::uuid, '30000000-0000-0000-0000-000000000009'::uuid, ARRAY['NewConsumerCredit']::varchar[], '-18 days'::interval, '-18 days'::interval + interval '2 seconds'),
    ('10000000-0000-0000-0000-000000000010'::uuid, '20000000-0000-0000-0000-000000000002'::uuid, '30000000-0000-0000-0000-000000000010'::uuid, ARRAY['GuaranteeOrThirdPartyPledgeForConsumerCredit']::varchar[], '-3 days'::interval, '-3 days'::interval + interval '2 seconds'),
    ('10000000-0000-0000-0000-000000000011'::uuid, '20000000-0000-0000-0000-000000000003'::uuid, '30000000-0000-0000-0000-000000000011'::uuid, ARRAY['NewLoan','ChangesToTerms']::varchar[], '-15 days'::interval, '-15 days'::interval + interval '2 seconds'),
    ('10000000-0000-0000-0000-000000000012'::uuid, '20000000-0000-0000-0000-000000000003'::uuid, '30000000-0000-0000-0000-000000000012'::uuid, ARRAY['NewConsumerCredit']::varchar[], '-7 days'::interval, '-7 days'::interval + interval '2 seconds'),
    ('10000000-0000-0000-0000-000000000013'::uuid, '20000000-0000-0000-0000-000000000004'::uuid, '30000000-0000-0000-0000-000000000013'::uuid, ARRAY['NewLoan']::varchar[], '-25 days'::interval, '-25 days'::interval + interval '2 seconds'),
    ('10000000-0000-0000-0000-000000000014'::uuid, '20000000-0000-0000-0000-000000000004'::uuid, '30000000-0000-0000-0000-000000000014'::uuid, ARRAY['ChangesToTerms']::varchar[], '-6 hours'::interval, '-6 hours'::interval + interval '2 seconds')
) AS v(id, consumer_id, client_request_id, extract_purposes, requested_offset, completed_offset);

WITH clock AS (SELECT transaction_timestamp() AS ts)
INSERT INTO credit_extract
    (id, financing_request_id, extract_reference, creation_time_utc,
     voluntary_ban_active, voluntary_ban_reason, lenders_count,
     loan_contracts_count, guaranteed_loan_contracts_count, repayment_amounts,
     leasing_instalment_amounts, loans, income_data, persisted_at)
SELECT v.id, v.request_id, v.extract_reference, clock.ts + v.completed_offset + interval '1 second',
       v.ban_active, v.ban_reason, v.lenders_count, v.loan_contracts_count,
       v.guaranteed_count, v.repayments::jsonb, v.leasing::jsonb, v.loans::jsonb,
       v.income::jsonb, clock.ts + v.completed_offset + interval '2 seconds'
FROM clock
CROSS JOIN (VALUES
    ('40000000-0000-0000-0000-000000000001'::uuid, '10000000-0000-0000-0000-000000000001'::uuid, '50000000-0000-0000-0000-000000000001'::uuid, '-30 days'::interval, false, NULL::varchar, 0, 0, 0, '[]', '[]', '[]', '[]'),
    ('40000000-0000-0000-0000-000000000002'::uuid, '10000000-0000-0000-0000-000000000002'::uuid, '50000000-0000-0000-0000-000000000002'::uuid, '-20 days'::interval, true, 'RiskOfIdentityTheft', 2, 2, 0, '[{"currencyCode":"EUR","sum":820.50},{"currencyCode":"USD","sum":210.00}]', '[{"currencyCode":"EUR","sum":299.99}]', '[{"loanType":"LumpSumLoan","contractDate":"2024-02-15","isLoanWithCollateral":true,"collateralType":["ApartmentOrRealEstate"],"borrowersCount":2,"currencyCode":"EUR","paymentPlan":{"isInDebtArrangement":false,"isInBusinessRestructuringProgram":false},"accuracyIsDenied":false,"lumpSumLoan":{"amountIssued":12000.00,"amountPaid":3500.00,"balance":8500.00,"plannedFinalDueDate":"2029-02-15","amortizationFrequency":12},"runningAccountLoan":null,"leasingContract":null,"delayedAmount":[{"delayedInstalment":125.00,"originalDueDate":"2026-08-15","isForeclosed":false}]}]', '[{"year":2024,"months":[{"month":11,"wagesGrossAmount":3200.00,"wagesNetAmount":2450.00,"benefitsGrossAmount":0.00,"benefitsNetAmount":0.00}]},{"year":2025,"months":[{"month":6,"wagesGrossAmount":3350.00,"wagesNetAmount":2550.00,"benefitsGrossAmount":150.00,"benefitsNetAmount":120.00},{"month":12,"wagesGrossAmount":3500.00,"wagesNetAmount":2650.00,"benefitsGrossAmount":0.00,"benefitsNetAmount":0.00}]}]'),
    ('40000000-0000-0000-0000-000000000003'::uuid, '10000000-0000-0000-0000-000000000003'::uuid, '50000000-0000-0000-0000-000000000003'::uuid, '-10 days'::interval, true, 'ControlOfPersonalFinances', 1, 1, 1, '[{"currencyCode":"GBP","sum":640.75}]', '[{"currencyCode":"USD","sum":455.25},{"currencyCode":"SEK","sum":1800.00}]', '[{"loanType":"RunningAccountLoan","contractDate":"2025-04-01","isLoanWithCollateral":false,"collateralType":[],"borrowersCount":1,"currencyCode":"GBP","paymentPlan":{"isInDebtArrangement":true,"isInBusinessRestructuringProgram":false},"accuracyIsDenied":false,"lumpSumLoan":null,"runningAccountLoan":{"creditLimit":5000.00,"balance":1780.00,"balanceDate":"2026-08-31"},"leasingContract":null,"delayedAmount":[]}]', '[{"year":2025,"months":[{"month":1,"wagesGrossAmount":2800.00,"wagesNetAmount":2150.00,"benefitsGrossAmount":200.00,"benefitsNetAmount":160.00},{"month":7,"wagesGrossAmount":2900.00,"wagesNetAmount":2220.00,"benefitsGrossAmount":0.00,"benefitsNetAmount":0.00}]},{"year":2026,"months":[{"month":2,"wagesGrossAmount":3050.00,"wagesNetAmount":2320.00,"benefitsGrossAmount":0.00,"benefitsNetAmount":0.00}]}]'),
    ('40000000-0000-0000-0000-000000000004'::uuid, '10000000-0000-0000-0000-000000000004'::uuid, '50000000-0000-0000-0000-000000000004'::uuid, '-5 days'::interval, true, 'Other', 3, 3, 1, '[{"currencyCode":"EUR","sum":1200.00},{"currencyCode":"SEK","sum":5000.00}]', '[{"currencyCode":"EUR","sum":410.00}]', '[{"loanType":"Leasing","contractDate":"2023-09-10","isLoanWithCollateral":true,"collateralType":["InstalmentSaleItem","OtherMoveableProperty"],"borrowersCount":1,"currencyCode":"EUR","paymentPlan":{"isInDebtArrangement":false,"isInBusinessRestructuringProgram":true},"accuracyIsDenied":false,"lumpSumLoan":null,"runningAccountLoan":null,"leasingContract":{"contractPeriodStartDate":"2023-09-10","transactionPrice":24000.00},"delayedAmount":[{"delayedInstalment":350.00,"originalDueDate":"2026-07-10","isForeclosed":true}]},{"loanType":"GuaranteeReceivable","contractDate":"2026-01-20","isLoanWithCollateral":true,"collateralType":["PersonalGuarantee"],"borrowersCount":1,"currencyCode":"USD","paymentPlan":{"isInDebtArrangement":false,"isInBusinessRestructuringProgram":false},"accuracyIsDenied":true,"lumpSumLoan":null,"runningAccountLoan":null,"leasingContract":null,"delayedAmount":[]}]', '[{"year":2023,"months":[{"month":9,"wagesGrossAmount":4100.00,"wagesNetAmount":3100.00,"benefitsGrossAmount":0.00,"benefitsNetAmount":0.00},{"month":10,"wagesGrossAmount":4200.00,"wagesNetAmount":3180.00,"benefitsGrossAmount":0.00,"benefitsNetAmount":0.00}]},{"year":2026,"months":[{"month":3,"wagesGrossAmount":4500.00,"wagesNetAmount":3400.00,"benefitsGrossAmount":100.00,"benefitsNetAmount":80.00}]}]'),
    ('40000000-0000-0000-0000-000000000005'::uuid, '10000000-0000-0000-0000-000000000005'::uuid, '50000000-0000-0000-0000-000000000005'::uuid, '-2 days'::interval, false, NULL::varchar, 1, 1, 0, '[{"currencyCode":"USD","sum":95.00}]', '[]', '[{"loanType":"LumpSumLoan","contractDate":"2026-05-05","isLoanWithCollateral":false,"collateralType":[],"borrowersCount":1,"currencyCode":"USD","paymentPlan":{"isInDebtArrangement":false,"isInBusinessRestructuringProgram":false},"accuracyIsDenied":false,"lumpSumLoan":{"amountIssued":900.00,"amountPaid":300.00,"balance":600.00,"plannedFinalDueDate":"2027-05-05","amortizationFrequency":1},"runningAccountLoan":null,"leasingContract":null,"delayedAmount":[]}]', '[]'),
    ('40000000-0000-0000-0000-000000000006'::uuid, '10000000-0000-0000-0000-000000000006'::uuid, '50000000-0000-0000-0000-000000000006'::uuid, '-1 day'::interval, false, NULL::varchar, 0, 0, 0, '[]', '[]', '[]', '[]'),
    ('40000000-0000-0000-0000-000000000007'::uuid, '10000000-0000-0000-0000-000000000007'::uuid, '50000000-0000-0000-0000-000000000007'::uuid, '-1 day'::interval + interval '1 second', true, 'RiskOfIdentityTheft', 1, 1, 0, '[]', '[]', '[]', '[]'),
    ('40000000-0000-0000-0000-000000000008'::uuid, '10000000-0000-0000-0000-000000000008'::uuid, '50000000-0000-0000-0000-000000000008'::uuid, '-12 hours'::interval, false, NULL::varchar, 1, 1, 0, '[]', '[{"currencyCode":"EUR","sum":199.00}]', '[]', '[]'),
    ('40000000-0000-0000-0000-000000000009'::uuid, '10000000-0000-0000-0000-000000000009'::uuid, '50000000-0000-0000-0000-000000000009'::uuid, '-18 days'::interval, false, NULL::varchar, 1, 1, 0, '[{"currencyCode":"EUR","sum":510.00}]', '[]', '[]', '[{"year":2024,"months":[{"month":12,"wagesGrossAmount":2100.00,"wagesNetAmount":1650.00,"benefitsGrossAmount":0.00,"benefitsNetAmount":0.00}]}]'),
    ('40000000-0000-0000-0000-000000000010'::uuid, '10000000-0000-0000-0000-000000000010'::uuid, '50000000-0000-0000-0000-000000000010'::uuid, '-3 days'::interval, true, 'ControlOfPersonalFinances', 1, 1, 0, '[]', '[]', '[]', '[]'),
    ('40000000-0000-0000-0000-000000000011'::uuid, '10000000-0000-0000-0000-000000000011'::uuid, '50000000-0000-0000-0000-000000000011'::uuid, '-15 days'::interval, false, NULL::varchar, 2, 2, 1, '[{"currencyCode":"SEK","sum":900.00}]', '[]', '[]', '[]'),
    ('40000000-0000-0000-0000-000000000012'::uuid, '10000000-0000-0000-0000-000000000012'::uuid, '50000000-0000-0000-0000-000000000012'::uuid, '-7 days'::interval, false, NULL::varchar, 1, 1, 0, '[]', '[]', '[]', '[]'),
    ('40000000-0000-0000-0000-000000000013'::uuid, '10000000-0000-0000-0000-000000000013'::uuid, '50000000-0000-0000-0000-000000000013'::uuid, '-25 days'::interval, true, 'Other', 2, 2, 0, '[]', '[{"currencyCode":"USD","sum":275.00}]', '[]', '[]'),
    ('40000000-0000-0000-0000-000000000014'::uuid, '10000000-0000-0000-0000-000000000014'::uuid, '50000000-0000-0000-0000-000000000014'::uuid, '-6 hours'::interval, false, NULL::varchar, 0, 0, 0, '[]', '[]', '[]', '[]')
) AS v(id, request_id, extract_reference, completed_offset, ban_active, ban_reason,
       lenders_count, loan_contracts_count, guaranteed_count, repayments, leasing, loans, income);

-- Compact checks; identity codes are intentionally shown only in masked form.
SELECT 'seed_consumers' AS check_name, count(*) AS value
FROM consumer WHERE id IN (
    '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002',
    '20000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000004');

SELECT 'seed_requests' AS check_name, count(*) AS value
FROM financing_request WHERE id::text LIKE '10000000-0000-0000-0000-0000000000%';

SELECT 'seed_extracts' AS check_name, count(*) AS value
FROM credit_extract WHERE id::text LIKE '40000000-0000-0000-0000-0000000000%';

SELECT 'seed_one_to_one' AS check_name,
       count(*) AS seed_requests,
       count(*) FILTER (WHERE extract_count = 1) AS requests_with_exactly_one_extract,
       count(*) FILTER (WHERE extract_count <> 1) AS requests_without_exactly_one_extract
FROM (
    SELECT r.id, count(e.id) AS extract_count
    FROM financing_request r
    JOIN consumer c ON c.id = r.consumer_id
    LEFT JOIN credit_extract e ON e.financing_request_id = r.id
    WHERE r.id::text LIKE '10000000-0000-0000-0000-0000000000%'
    GROUP BY r.id
) AS relationship_check;

SELECT 'seed_active_bans' AS check_name, count(*) AS value,
       array_agg(DISTINCT voluntary_ban_reason ORDER BY voluntary_ban_reason) AS reasons
FROM credit_extract WHERE id::text LIKE '40000000-0000-0000-0000-0000000000%'
  AND voluntary_ban_active;

SELECT 'seed_masked_identity_codes' AS check_name,
       array_agg('******' || right(personal_identity_code, 5) ORDER BY id) AS masked_values
FROM consumer WHERE id IN (
    '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002',
    '20000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000004');

COMMIT;
