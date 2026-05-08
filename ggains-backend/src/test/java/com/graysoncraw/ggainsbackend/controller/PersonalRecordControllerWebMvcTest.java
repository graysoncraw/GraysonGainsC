package com.graysoncraw.ggainsbackend.controller;

import com.graysoncraw.ggainsbackend.dto.personalrecord.PersonalRecordResponseDTO;
import com.graysoncraw.ggainsbackend.exception.GlobalExceptionHandler;
import com.graysoncraw.ggainsbackend.mapper.PersonalRecordMapper;
import com.graysoncraw.ggainsbackend.model.PersonalRecord;
import com.graysoncraw.ggainsbackend.security.AuthenticatedUserGuard;
import com.graysoncraw.ggainsbackend.service.PersonalRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PersonalRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration")
class PersonalRecordControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PersonalRecordService personalRecordService;
    @MockitoBean
    private PersonalRecordMapper personalRecordMapper;
    @MockitoBean
    private AuthenticatedUserGuard authenticatedUserGuard;

    @Test
    void createPersonalRecordReturnsCreatedWhenValid() throws Exception {
        PersonalRecord mapped = PersonalRecord.builder().build();
        PersonalRecord created = PersonalRecord.builder().id(1L).build();
        PersonalRecordResponseDTO response = new PersonalRecordResponseDTO();
        response.setId(1L);

        doNothing().when(authenticatedUserGuard).requireUidMatches("uid-123");
        when(personalRecordMapper.toEntity(any())).thenReturn(mapped);
        when(personalRecordService.createPersonalRecord("uid-123", mapped)).thenReturn(created);
        when(personalRecordMapper.toResponse(created)).thenReturn(response);

        mockMvc.perform(post("/api/users/uid-123/personal-record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "benchPressPR":225.0,
                                  "squatPR":315.0,
                                  "deadliftPR":405.0,
                                  "shoulderPressPR":135.0
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createPersonalRecordReturnsBadRequestWhenValidationFails() throws Exception {
        doNothing().when(authenticatedUserGuard).requireUidMatches("uid-123");

        mockMvc.perform(post("/api/users/uid-123/personal-record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "squatPR":315.0,
                                  "deadliftPR":405.0,
                                  "shoulderPressPR":135.0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getPersonalRecordReturnsForbiddenWhenUidMismatch() throws Exception {
        doThrow(new AccessDeniedException("You are not allowed to access this user's data"))
                .when(authenticatedUserGuard)
                .requireUidMatches("uid-other");

        mockMvc.perform(get("/api/users/uid-other/personal-record"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void updateSpecificPrReturnsMappedRecord() throws Exception {
        PersonalRecord updated = PersonalRecord.builder().id(11L).build();
        PersonalRecordResponseDTO response = new PersonalRecordResponseDTO();
        response.setId(11L);
        response.setBenchPressPR(230.0);

        doNothing().when(authenticatedUserGuard).requireUidMatches("uid-123");
        when(personalRecordService.updateSpecificPR("uid-123", "BENCH", 230.0)).thenReturn(updated);
        when(personalRecordMapper.toResponse(updated)).thenReturn(response);

        mockMvc.perform(patch("/api/users/uid-123/personal-record/BENCH")
                        .param("newPR", "230.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11));
    }
}
