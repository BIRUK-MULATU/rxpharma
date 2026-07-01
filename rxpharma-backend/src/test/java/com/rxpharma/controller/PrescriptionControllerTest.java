package com.rxpharma.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rxpharma.dto.request.PrescriptionDrugRequest;
import com.rxpharma.dto.request.PrescriptionRequest;
import com.rxpharma.dto.response.PrescriptionDrugResponse;
import com.rxpharma.entity.Prescription;
import com.rxpharma.entity.User;
import com.rxpharma.repository.UserRepository;
import com.rxpharma.security.CustomUserDetailsService;
import com.rxpharma.security.JwtUtil;
import com.rxpharma.service.PrescriptionService;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = PrescriptionController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class PrescriptionControllerTest {



    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PrescriptionService prescriptionService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private Prescription createTestPrescription() {
        return Prescription.builder().id(1L).patientName("Abebe Girma")
                .doctorName("Dr. Lemma").issuedDate(LocalDate.now())
                .status(Prescription.Status.PENDING).notes("Test notes").build();
    }

    @Test
    void getAllPrescriptions_returnsPage() throws Exception {
        Page<Prescription> page = new PageImpl<>(List.of(createTestPrescription()));
        when(prescriptionService.getAllPrescriptions(any())).thenReturn(page);

        mockMvc.perform(get("/api/prescriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].patientName").value("Abebe Girma"));
    }

    @Test
    void getPrescriptionById_found() throws Exception {
        when(prescriptionService.getPrescriptionById(1L)).thenReturn(createTestPrescription());

        mockMvc.perform(get("/api/prescriptions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientName").value("Abebe Girma"));
    }

    @Test
    void createPrescription_success() throws Exception {
        PrescriptionRequest request = new PrescriptionRequest();
        request.setPatientName("Sara");
        request.setDoctorName("Dr. Kebede");
        request.setIssuedDate(LocalDate.now());

        when(prescriptionService.createPrescription(anyString(), anyString(), any(), any()))
                .thenReturn(createTestPrescription());

        mockMvc.perform(post("/api/prescriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientName").value("Abebe Girma"));
    }

    @Test
    void dispensePrescription_success() throws Exception {
        User dispenser = User.builder().id(1L).fullName("Pharmacist").email("pharmacist@rxpharma.com").build();
        when(userRepository.findByEmail("pharmacist@rxpharma.com")).thenReturn(Optional.of(dispenser));

        Prescription dispensed = createTestPrescription();
        dispensed.setStatus(Prescription.Status.DISPENSED);
        dispensed.setDispensedBy(dispenser);
        when(prescriptionService.dispensePrescription(1L, 1L)).thenReturn(dispensed);

        mockMvc.perform(patch("/api/prescriptions/1/dispense")
                        .with(request -> {
                            request.setUserPrincipal(new UsernamePasswordAuthenticationToken(
                                    "pharmacist@rxpharma.com", null,
                                    List.of(new SimpleGrantedAuthority("ROLE_PHARMACIST"))));
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISPENSED"));
    }

    @Test
    void cancelPrescription_success() throws Exception {
        Prescription cancelled = createTestPrescription();
        cancelled.setStatus(Prescription.Status.CANCELLED);
        when(prescriptionService.cancelPrescription(1L)).thenReturn(cancelled);

        mockMvc.perform(patch("/api/prescriptions/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void addDrugToPrescription_success() throws Exception {
        PrescriptionDrugRequest request = new PrescriptionDrugRequest();
        request.setDrugId(1L);
        request.setQuantity(10);
        request.setDosageInstructions("Take twice daily");

        PrescriptionDrugResponse response = PrescriptionDrugResponse.builder()
                .drugId(1L).drugName("Amoxicillin").quantity(10)
                .dosageInstructions("Take twice daily").build();

        when(prescriptionService.addDrugToPrescription(1L, 1L, 10, "Take twice daily"))
                .thenReturn(response);

        mockMvc.perform(post("/api/prescriptions/1/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.drugName").value("Amoxicillin"));
    }

    @Test
    void getPrescriptionDrugs_returnsList() throws Exception {
        PrescriptionDrugResponse response = PrescriptionDrugResponse.builder()
                .drugId(1L).drugName("Amoxicillin").quantity(10).build();
        when(prescriptionService.getPrescriptionDrugs(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/prescriptions/1/drugs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].drugName").value("Amoxicillin"));
    }

    @Test
    void searchByPatientName_returnsPage() throws Exception {
        Page<Prescription> page = new PageImpl<>(List.of(createTestPrescription()));
        when(prescriptionService.searchByPatientName(eq("Abebe"), any())).thenReturn(page);

        mockMvc.perform(get("/api/prescriptions/search").param("patientName", "Abebe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].patientName").value("Abebe Girma"));
    }
}
