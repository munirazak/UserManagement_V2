package com.kopibru.usermanagement.controller;

import com.kopibru.usermanagement.dto.BorrowerRegistrationRequest;
import com.kopibru.usermanagement.exception.DuplicateBorrowerException;
import com.kopibru.usermanagement.exception.ForbiddenRoleException;
import com.kopibru.usermanagement.exception.GlobalExceptionHandler;
import com.kopibru.usermanagement.exception.ResourceNotFoundException;
import com.kopibru.usermanagement.model.Borrower;
import com.kopibru.usermanagement.model.BorrowerRole;
import com.kopibru.usermanagement.security.JwtService;
import com.kopibru.usermanagement.service.BorrowerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BorrowerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BorrowerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BorrowerService borrowerService;

    @MockBean
    private JwtService jwtService;

    @Test
    void registerBorrower_returnsCreated() throws Exception {
        BorrowerRegistrationRequest request = new BorrowerRegistrationRequest(
                "LIB-001", "Alice", "alice@example.com", "alice", "password123");
        Borrower borrower = new Borrower("LIB-001", "Alice", "alice@example.com");
        borrower.setId(1L);
        when(borrowerService.register(any(BorrowerRegistrationRequest.class))).thenReturn(borrower);

        mockMvc.perform(post("/api/borrowers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.libraryId").value("LIB-001"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.role").value("BORROWER"));
    }

    @Test
    void registerBorrower_acceptsExplicitRole() throws Exception {
        BorrowerRegistrationRequest request = new BorrowerRegistrationRequest(
                "LIB-002", "Bob", "bob@example.com", "bob", "password123", BorrowerRole.ADMIN);
        Borrower borrower = new Borrower("LIB-002", "Bob", "bob@example.com", BorrowerRole.ADMIN);
        borrower.setId(2L);
        when(borrowerService.register(any(BorrowerRegistrationRequest.class))).thenReturn(borrower);

        mockMvc.perform(post("/api/borrowers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void registerBorrower_returnsBadRequestWhenRoleInvalid() throws Exception {
        String body = """
                {"libraryId":"LIB-003","name":"Carol","email":"carol@example.com",
                 "username":"carol","password":"password123","role":"ADMI"}
                """;

        mockMvc.perform(post("/api/borrowers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerBorrower_returnsBadRequestWhenSuperAdminRequested() throws Exception {
        BorrowerRegistrationRequest request = new BorrowerRegistrationRequest(
                "LIB-SA", "Admin", "admin@example.com", "admin", "password123", BorrowerRole.SUPER_ADMIN);
        when(borrowerService.register(any(BorrowerRegistrationRequest.class)))
                .thenThrow(new ForbiddenRoleException(
                        "SUPER_ADMIN can only be created by one-time bootstrap, not via registration"));

        mockMvc.perform(post("/api/borrowers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("one-time bootstrap")));
    }

    @Test
    void registerBorrower_returnsBadRequestWhenInvalid() throws Exception {
        String body = """
                {"libraryId":"","name":"Alice","email":"bad-email","username":"ab","password":"short"}
                """;

        mockMvc.perform(post("/api/borrowers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    void registerBorrower_returnsConflictWhenDuplicate() throws Exception {
        BorrowerRegistrationRequest request = new BorrowerRegistrationRequest(
                "LIB-001", "Alice", "alice@example.com", "alice", "password123");
        when(borrowerService.register(any(BorrowerRegistrationRequest.class)))
                .thenThrow(new DuplicateBorrowerException("already exists"));

        mockMvc.perform(post("/api/borrowers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void getAllBorrowers_returnsOk() throws Exception {
        when(borrowerService.getAllBorrowers()).thenReturn(List.of(
                new Borrower("LIB-001", "Alice", "alice@example.com")));

        mockMvc.perform(get("/api/borrowers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].libraryId").value("LIB-001"));
    }

    @Test
    void getBorrower_returnsOk() throws Exception {
        Borrower borrower = new Borrower("LIB-001", "Alice", "alice@example.com");
        borrower.setId(1L);
        when(borrowerService.getByLibraryId("LIB-001")).thenReturn(borrower);

        mockMvc.perform(get("/api/borrowers/LIB-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libraryId").value("LIB-001"));
    }

    @Test
    void getBorrower_returnsNotFound() throws Exception {
        when(borrowerService.getByLibraryId("LIB-999"))
                .thenThrow(new ResourceNotFoundException("not found"));

        mockMvc.perform(get("/api/borrowers/LIB-999"))
                .andExpect(status().isNotFound());
    }
}
