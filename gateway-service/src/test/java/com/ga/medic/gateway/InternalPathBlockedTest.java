package com.ga.medic.gateway;

import com.ga.medic.gateway.filter.BlockInternalPathFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternalPathBlockedTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void internalPathsReturn404AndAreNotProxied() throws Exception {
        mockMvc.perform(get("/internal/doctors/1"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/identity/internal/doctors/1"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/booking/internal/anything"))
                .andExpect(status().isNotFound());
    }

    @Test
    void recognizesInternalPaths() {
        assertThat(BlockInternalPathFilter.isInternalPath("/internal")).isTrue();
        assertThat(BlockInternalPathFilter.isInternalPath("/internal/doctors/1")).isTrue();
        assertThat(BlockInternalPathFilter.isInternalPath("/api/identity/internal")).isTrue();
        assertThat(BlockInternalPathFilter.isInternalPath("/api/identity/internal/doctors/1")).isTrue();
        assertThat(BlockInternalPathFilter.isInternalPath("/api/notification/internal")).isTrue();
        assertThat(BlockInternalPathFilter.isInternalPath("/api/identity/auth/users/login")).isFalse();
        assertThat(BlockInternalPathFilter.isInternalPath("/api/identity/uploads/x")).isFalse();
    }
}
