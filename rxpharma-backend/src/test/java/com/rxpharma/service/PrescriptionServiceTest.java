package com.rxpharma.service;

import com.rxpharma.dto.response.PrescriptionDrugResponse;
import com.rxpharma.entity.*;
import com.rxpharma.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrescriptionService Tests")
class PrescriptionServiceTest {

    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private PrescriptionDrugRepository prescriptionDrugRepository;
    @Mock private DrugRepository drugRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private PrescriptionService prescriptionService;

    private Prescription pendingPrescription;
    private Prescription dispensedPrescription;
    private Drug testDrug;
    private User testPharmacist;
    private PrescriptionDrug testPrescriptionDrug;

    @BeforeEach
    void setUp() {
        testDrug = Drug.builder()
                .id(1L)
                .name("Amoxicillin 500mg")
                .sku("AMX-500")
                .stockQty(100)
                .build();

        testPharmacist = User.builder()
                .id(1L)
                .fullName("Biruk Pharmacist")
                .email("biruk@rxpharma.com")
                .role(User.Role.PHARMACIST)
                .build();

        testPrescriptionDrug = PrescriptionDrug.builder()
                .id(1L)
                .drug(testDrug)
                .quantity(10)
                .dosageInstructions("Take twice daily")
                .build();

        pendingPrescription = Prescription.builder()
                .id(1L)
                .patientName("Abebe Girma")
                .doctorName("Dr. Lemma")
                .issuedDate(LocalDate.now())
                .status(Prescription.Status.PENDING)
                .notes("Test notes")
                .prescriptionDrugs(new ArrayList<>(List.of(testPrescriptionDrug)))
                .build();

        testPrescriptionDrug.setPrescription(pendingPrescription);

        dispensedPrescription = Prescription.builder()
                .id(2L)
                .patientName("Sara Tesfaye")
                .doctorName("Dr. Haile")
                .status(Prescription.Status.DISPENSED)
                .prescriptionDrugs(new ArrayList<>())
                .build();
    }

    // ─── getPrescriptionById ──────────────────────────────────────────────────

    @Test
    @DisplayName("getPrescriptionById - found returns prescription")
    void getPrescriptionById_found() {
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(pendingPrescription));

        Prescription result = prescriptionService.getPrescriptionById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getPatientName()).isEqualTo("Abebe Girma");
    }

    @Test
    @DisplayName("getPrescriptionById - not found throws RuntimeException")
    void getPrescriptionById_notFound() {
        when(prescriptionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> prescriptionService.getPrescriptionById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Prescription not found with id: 99");
    }

    // ─── createPrescription ───────────────────────────────────────────────────

    @Test
    @DisplayName("createPrescription - success saves with PENDING status")
    void createPrescription_success() {
        when(prescriptionRepository.save(any(Prescription.class)))
                .thenAnswer(i -> i.getArgument(0));

        Prescription result = prescriptionService.createPrescription(
                "Abebe Girma", "Dr. Lemma", LocalDate.now(), "Some notes");

        assertThat(result.getPatientName()).isEqualTo("Abebe Girma");
        assertThat(result.getDoctorName()).isEqualTo("Dr. Lemma");
        assertThat(result.getStatus()).isEqualTo(Prescription.Status.PENDING);
        assertThat(result.getNotes()).isEqualTo("Some notes");
        verify(prescriptionRepository).save(any(Prescription.class));
    }

    @Test
    @DisplayName("createPrescription - null notes is accepted")
    void createPrescription_nullNotes() {
        when(prescriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Prescription result = prescriptionService.createPrescription(
                "Patient", "Doctor", LocalDate.now(), null);

        assertThat(result.getNotes()).isNull();
        assertThat(result.getStatus()).isEqualTo(Prescription.Status.PENDING);
    }

    // ─── dispensePrescription ─────────────────────────────────────────────────

    @Test
    @DisplayName("dispensePrescription - success deducts stock and sets DISPENSED status")
    void dispensePrescription_success() {
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(pendingPrescription));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testPharmacist));
        when(drugRepository.findById(1L)).thenReturn(Optional.of(testDrug));
        when(drugRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(prescriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Prescription result = prescriptionService.dispensePrescription(1L, 1L);

        assertThat(result.getStatus()).isEqualTo(Prescription.Status.DISPENSED);
        assertThat(result.getDispensedBy()).isEqualTo(testPharmacist);
        assertThat(testDrug.getStockQty()).isEqualTo(90); // 100 - 10
        verify(drugRepository).save(testDrug);
    }

    @Test
    @DisplayName("dispensePrescription - already dispensed throws RuntimeException")
    void dispensePrescription_alreadyDispensed() {
        when(prescriptionRepository.findById(2L)).thenReturn(Optional.of(dispensedPrescription));

        assertThatThrownBy(() -> prescriptionService.dispensePrescription(2L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only PENDING prescriptions can be dispensed");
    }

    @Test
    @DisplayName("dispensePrescription - dispenser not found throws RuntimeException")
    void dispensePrescription_dispenserNotFound() {
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(pendingPrescription));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> prescriptionService.dispensePrescription(1L, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("dispensePrescription - insufficient stock throws RuntimeException")
    void dispensePrescription_insufficientStock() {
        testDrug.setStockQty(5); // less than required quantity of 10
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(pendingPrescription));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testPharmacist));
        when(drugRepository.findById(1L)).thenReturn(Optional.of(testDrug));

        assertThatThrownBy(() -> prescriptionService.dispensePrescription(1L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("dispensePrescription - drug not found in repository throws RuntimeException")
    void dispensePrescription_drugNotFound() {
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(pendingPrescription));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testPharmacist));
        when(drugRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> prescriptionService.dispensePrescription(1L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Drug not found");
    }

    // ─── cancelPrescription ───────────────────────────────────────────────────

    @Test
    @DisplayName("cancelPrescription - success sets CANCELLED status")
    void cancelPrescription_success() {
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(pendingPrescription));
        when(prescriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Prescription result = prescriptionService.cancelPrescription(1L);

        assertThat(result.getStatus()).isEqualTo(Prescription.Status.CANCELLED);
        verify(prescriptionRepository).save(pendingPrescription);
    }

    @Test
    @DisplayName("cancelPrescription - cannot cancel already dispensed prescription")
    void cancelPrescription_alreadyDispensed() {
        when(prescriptionRepository.findById(2L)).thenReturn(Optional.of(dispensedPrescription));

        assertThatThrownBy(() -> prescriptionService.cancelPrescription(2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only PENDING prescriptions can be cancelled");
    }

    @Test
    @DisplayName("cancelPrescription - prescription not found throws RuntimeException")
    void cancelPrescription_notFound() {
        when(prescriptionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> prescriptionService.cancelPrescription(99L))
                .isInstanceOf(RuntimeException.class);
    }

    // ─── addDrugToPrescription ────────────────────────────────────────────────

    @Test
    @DisplayName("addDrugToPrescription - success adds drug and returns response")
    void addDrugToPrescription_success() {
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(pendingPrescription));
        when(drugRepository.findById(1L)).thenReturn(Optional.of(testDrug));
        when(prescriptionDrugRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PrescriptionDrugResponse result = prescriptionService.addDrugToPrescription(
                1L, 1L, 5, "Take once daily");

        assertThat(result.getDrugId()).isEqualTo(1L);
        assertThat(result.getDrugName()).isEqualTo("Amoxicillin 500mg");
        assertThat(result.getQuantity()).isEqualTo(5);
        assertThat(result.getDosageInstructions()).isEqualTo("Take once daily");
        verify(prescriptionDrugRepository).save(any(PrescriptionDrug.class));
    }

    @Test
    @DisplayName("addDrugToPrescription - cannot add to non-PENDING prescription")
    void addDrugToPrescription_notPending() {
        when(prescriptionRepository.findById(2L)).thenReturn(Optional.of(dispensedPrescription));

        assertThatThrownBy(() -> prescriptionService.addDrugToPrescription(
                2L, 1L, 5, "Instructions"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("non-PENDING");
    }

    @Test
    @DisplayName("addDrugToPrescription - drug not found throws RuntimeException")
    void addDrugToPrescription_drugNotFound() {
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(pendingPrescription));
        when(drugRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> prescriptionService.addDrugToPrescription(
                1L, 99L, 5, "Instructions"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Drug not found");
    }

    // ─── getPrescriptionDrugs ─────────────────────────────────────────────────

    @Test
    @DisplayName("getPrescriptionDrugs - returns drugs for prescription")
    void getPrescriptionDrugs_success() {
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(pendingPrescription));
        when(prescriptionDrugRepository.findByPrescriptionId(1L))
                .thenReturn(List.of(testPrescriptionDrug));

        List<PrescriptionDrugResponse> result =
                prescriptionService.getPrescriptionDrugs(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDrugName()).isEqualTo("Amoxicillin 500mg");
        assertThat(result.get(0).getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("getPrescriptionDrugs - empty list when no drugs added yet")
    void getPrescriptionDrugs_empty() {
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(pendingPrescription));
        when(prescriptionDrugRepository.findByPrescriptionId(1L)).thenReturn(List.of());

        List<PrescriptionDrugResponse> result =
                prescriptionService.getPrescriptionDrugs(1L);

        assertThat(result).isEmpty();
    }
}