package com.creditlens.backend.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PageDtoTest {
  @Test
  void createsPageWithItems() {
    PageDto<String> dto = new PageDto<>(List.of("item"), 0, 10, 1, 1);

    assertThat(dto.items()).containsExactly("item");
    assertThat(dto.totalItems()).isEqualTo(1);
  }
}
