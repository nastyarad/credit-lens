package com.creditlens.backend.persistence.entity;

import com.creditlens.backend.domain.CreditExtract;
import com.creditlens.backend.domain.CreditExtractData;
import com.creditlens.backend.domain.CreditInformationSummary;
import com.creditlens.backend.domain.VoluntaryBanOnCredits;
import com.creditlens.backend.domain.VoluntaryCreditBanReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "credit_extract")
public class CreditExtractEntity {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "financing_request_id", nullable = false, unique = true)
    private FinancingRequestEntity financingRequest;

    @Column(name = "extract_reference", nullable = false, unique = true)
    private UUID extractReference;

    @Column(name = "creation_time_utc", nullable = false)
    private Instant creationTimeUtc;

    @Column(name = "voluntary_ban_active", nullable = false)
    private boolean voluntaryBanActive;

    @Column(name = "voluntary_ban_reason", length = 64)
    private String voluntaryBanReason;

    @Column(name = "lenders_count", nullable = false)
    private int lendersCount;

    @Column(name = "loan_contracts_count", nullable = false)
    private int loanContractsCount;

    @Column(name = "guaranteed_loan_contracts_count", nullable = false)
    private int guaranteedLoanContractsCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "repayment_amounts", nullable = false, columnDefinition = "jsonb")
    private List<CreditExtractData.CurrencyAmount> repaymentAmounts = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "leasing_instalment_amounts", nullable = false, columnDefinition = "jsonb")
    private List<CreditExtractData.CurrencyAmount> leasingInstalmentAmounts = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "loans", nullable = false, columnDefinition = "jsonb")
    private List<CreditExtractData.Loan> loans = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "income_data", nullable = false, columnDefinition = "jsonb")
    private List<CreditExtractData.IncomeData> incomeData = new ArrayList<>();

    @Column(name = "persisted_at", nullable = false)
    private Instant persistedAt;

    protected CreditExtractEntity() {
    }

    public CreditExtractEntity(CreditExtract extract, FinancingRequestEntity financingRequest) {
        id = extract.id();
        this.financingRequest = financingRequest;
        extractReference = extract.extractReference();
        creationTimeUtc = extract.creationTimeUtc();
        voluntaryBanActive = extract.voluntaryBanOnCredits().isInEffect();
        voluntaryBanReason = extract.voluntaryBanOnCredits().reason() == null
                ? null
                : extract.voluntaryBanOnCredits().reason().name();
        lendersCount = extract.creditInformationSummary().lendersCount();
        loanContractsCount = extract.creditInformationSummary().loanContractsCount();
        guaranteedLoanContractsCount = extract.creditInformationSummary().guaranteedLoanContractsCount();
        repaymentAmounts = new ArrayList<>(extract.repaymentsPaidLastAmount());
        leasingInstalmentAmounts = new ArrayList<>(extract.sumOfMonthlyLeasingInstalments());
        loans = new ArrayList<>(extract.loans());
        incomeData = new ArrayList<>(extract.incomeData());
        persistedAt = extract.persistedAt();
    }

    public CreditExtract toDomain() {
        VoluntaryCreditBanReason reason = voluntaryBanReason == null
                ? null
                : VoluntaryCreditBanReason.valueOf(voluntaryBanReason);
        return new CreditExtract(
                id,
                extractReference,
                creationTimeUtc,
                new VoluntaryBanOnCredits(voluntaryBanActive, reason),
                new CreditInformationSummary(
                        lendersCount,
                        loanContractsCount,
                        guaranteedLoanContractsCount
                ),
                repaymentAmounts,
                leasingInstalmentAmounts,
                loans,
                incomeData,
                persistedAt
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getFinancingRequestId() {
        return financingRequest.getId();
    }

    public UUID getExtractReference() {
        return extractReference;
    }

    public Instant getCreationTimeUtc() {
        return creationTimeUtc;
    }

    public boolean isVoluntaryBanActive() {
        return voluntaryBanActive;
    }

    public String getVoluntaryBanReason() {
        return voluntaryBanReason;
    }

    public int getLendersCount() {
        return lendersCount;
    }

    public int getLoanContractsCount() {
        return loanContractsCount;
    }

    public int getGuaranteedLoanContractsCount() {
        return guaranteedLoanContractsCount;
    }

    public List<CreditExtractData.CurrencyAmount> getRepaymentAmounts() {
        return List.copyOf(repaymentAmounts);
    }

    public List<CreditExtractData.CurrencyAmount> getLeasingInstalmentAmounts() {
        return List.copyOf(leasingInstalmentAmounts);
    }

    public List<CreditExtractData.Loan> getLoans() {
        return List.copyOf(loans);
    }

    public List<CreditExtractData.IncomeData> getIncomeData() {
        return List.copyOf(incomeData);
    }

    public Instant getPersistedAt() {
        return persistedAt;
    }
}
