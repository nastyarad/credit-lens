package com.creditlens.backend.integration.pcr;

import com.creditlens.backend.domain.CreditExtract;
import com.creditlens.backend.domain.CreditRegisterExtractPurpose;
import com.creditlens.backend.domain.PersonalIdentityCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.util.List;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class HttpPositiveCreditRegisterClient implements PositiveCreditRegisterClient {
  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final PcrProperties properties;

  public HttpPositiveCreditRegisterClient(RestClient.Builder builder, PcrProperties properties) {
    this.restClient =
        builder.baseUrl(properties.baseUrl()).requestFactory(requestFactory(properties)).build();
    this.objectMapper = JsonMapper.builder().build();
    this.properties = properties;
  }

  @Override
  public CreditExtract requestCreditExtract(
      PersonalIdentityCode personalIdentityCode, List<CreditRegisterExtractPurpose> purposes) {
    PcrTransport.Request request =
        new PcrTransport.Request(
            properties.targetEnvironment(),
            new PcrTransport.Owner(
                properties.owner().idCodeType(),
                properties.owner().idCode(),
                properties.owner().countryCode()),
            "PersonalIdentityCode",
            personalIdentityCode.value(),
            purposes.stream().map(Enum::name).toList());
    try {
      String responseBody =
          restClient
              .post()
              .uri("/GetCreditRegisterExtract")
              .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
              .accept(org.springframework.http.MediaType.APPLICATION_JSON)
              .body(request)
              .retrieve()
              .toEntity(String.class)
              .getBody();
      PcrTransport.Response response = readResponse(responseBody);
      return PcrCreditExtractMapper.map(response.creditRegisterExtract(), personalIdentityCode);
    } catch (org.springframework.web.client.HttpStatusCodeException exception) {
      throw new PositiveCreditRegisterException(kindFor(exception.getStatusCode()));
    } catch (ResourceAccessException exception) {
      throw new PositiveCreditRegisterException(
          isTimeout(exception)
              ? PositiveCreditRegisterException.Kind.TIMEOUT
              : PositiveCreditRegisterException.Kind.UNAVAILABLE);
    } catch (JsonProcessingException | IllegalArgumentException exception) {
      throw new PositiveCreditRegisterException(
          PositiveCreditRegisterException.Kind.INVALID_RESPONSE);
    }
  }

  private PcrTransport.Response readResponse(String body) throws JsonProcessingException {
    if (body == null || body.isBlank()) throw new JsonProcessingException("empty response") {};
    return objectMapper.readValue(body, PcrTransport.Response.class);
  }

  private static PositiveCreditRegisterException.Kind kindFor(HttpStatusCode status) {
    if (status.value() == 429 || status.is5xxServerError())
      return PositiveCreditRegisterException.Kind.UNAVAILABLE;
    if (status.is4xxClientError()) return PositiveCreditRegisterException.Kind.REJECTED;
    return PositiveCreditRegisterException.Kind.INVALID_RESPONSE;
  }

  private static boolean isTimeout(Throwable exception) {
    for (Throwable current = exception; current != null; current = current.getCause()) {
      if (current instanceof SocketTimeoutException
          || current instanceof java.util.concurrent.TimeoutException) return true;
    }
    return false;
  }

  private static JdkClientHttpRequestFactory requestFactory(PcrProperties properties) {
    HttpClient httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(properties.connectTimeout())
            .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(properties.readTimeout());
    return factory;
  }
}
