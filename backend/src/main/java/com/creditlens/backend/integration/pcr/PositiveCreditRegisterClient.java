package com.creditlens.backend.integration.pcr;

import com.creditlens.backend.domain.CreditExtract;
import com.creditlens.backend.domain.CreditRegisterExtractPurpose;
import com.creditlens.backend.domain.PersonalIdentityCode;
import java.util.List;

public interface PositiveCreditRegisterClient {

  CreditExtract requestCreditExtract(
      PersonalIdentityCode personalIdentityCode, List<CreditRegisterExtractPurpose> purposes);
}
