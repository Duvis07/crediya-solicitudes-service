package co.com.crediya.solicitudes.api.mapper;

import co.com.crediya.solicitudes.api.dto.ApplicationDetailResponse;
import co.com.crediya.solicitudes.api.service.ApplicationDetailService;
import co.com.crediya.solicitudes.model.application.Application;
import co.com.crediya.solicitudes.model.common.PageRequest;
import co.com.crediya.solicitudes.model.common.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageResponseMapperTest {

    @Mock
    private ApplicationDetailService applicationDetailService;

    private PageResponseMapper pageResponseMapper;

    @BeforeEach
    void setUp() {
        pageResponseMapper = new PageResponseMapper(applicationDetailService);
    }

    @Test
    void buildPageResponseWithDetailsShouldReturnCorrectStructureWhenContentExists() {
        // Arrange
        Application app1 = Application.builder()
                .applicationId(1L)
                .documentId("12345678")
                .email("test1@example.com")
                .amount(new BigDecimal("500000.00"))
                .term(12)
                .loanTypeId(1L)
                .stateId(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Application app2 = Application.builder()
                .applicationId(2L)
                .documentId("87654321")
                .email("test2@example.com")
                .amount(new BigDecimal("1000000.00"))
                .term(24)
                .loanTypeId(2L)
                .stateId(2L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        List<Application> applications = Arrays.asList(app1, app2);
        PageRequest pageRequest = PageRequest.of(0, 10);
        PageResponse<Application> pageResponse = PageResponse.of(applications, pageRequest, 2L);

        ApplicationDetailResponse detail1 = ApplicationDetailResponse.builder()
                .amount(new BigDecimal("500000.00"))
                .term(12)
                .applicationState("Pendiente")
                .documentId("12345678")
                .email("test1@example.com")
                .fullName("Juan Pérez")
                .baseSalary(new BigDecimal("3000000.00"))
                .loanTypeName("Prestamo Personal")
                .interestRate(new BigDecimal("0.1250"))
                .totalMonthlyDebt(new BigDecimal("450000.00"))
                .build();

        ApplicationDetailResponse detail2 = ApplicationDetailResponse.builder()
                .amount(new BigDecimal("1000000.00"))
                .term(24)
                .applicationState("Aprobada")
                .documentId("87654321")
                .email("test2@example.com")
                .fullName("María García")
                .baseSalary(new BigDecimal("5000000.00"))
                .loanTypeName("Prestamo Hipotecario")
                .interestRate(new BigDecimal("0.0890"))
                .totalMonthlyDebt(new BigDecimal("750000.00"))
                .build();

        when(applicationDetailService.buildDetailResponse(app1))
                .thenReturn(Mono.just(detail1));
        when(applicationDetailService.buildDetailResponse(app2))
                .thenReturn(Mono.just(detail2));

        // Act
        Mono<Map<String, Object>> result = pageResponseMapper.buildPageResponseWithDetails(pageResponse);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(6, response.size());

                    // Verify content
                    @SuppressWarnings("unchecked")
                    List<ApplicationDetailResponse> content = (List<ApplicationDetailResponse>) response.get("content");
                    assertNotNull(content);
                    assertEquals(2, content.size());
                    assertEquals("Juan Pérez", content.get(0).getFullName());
                    assertEquals("María García", content.get(1).getFullName());

                    // Verify pagination metadata
                    assertEquals(0, response.get("page"));
                    assertEquals(10, response.get("size"));
                    assertEquals(2L, response.get("totalElements"));
                    assertEquals(true, response.get("first"));
                    assertEquals(true, response.get("last"));
                })
                .verifyComplete();
    }

    @Test
    void buildPageResponseWithDetailsShouldReturnEmptyContentWhenNoApplications() {
        // Arrange
        List<Application> emptyApplications = Collections.emptyList();
        PageRequest pageRequest = PageRequest.of(0, 10);
        PageResponse<Application> pageResponse = PageResponse.of(emptyApplications, pageRequest, 0L);

        // Act
        Mono<Map<String, Object>> result = pageResponseMapper.buildPageResponseWithDetails(pageResponse);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(6, response.size());

                    // Verify empty content
                    @SuppressWarnings("unchecked")
                    List<ApplicationDetailResponse> content = (List<ApplicationDetailResponse>) response.get("content");
                    assertNotNull(content);
                    assertTrue(content.isEmpty());

                    // Verify pagination metadata
                    assertEquals(0, response.get("page"));
                    assertEquals(10, response.get("size"));
                    assertEquals(0L, response.get("totalElements"));
                    assertEquals(true, response.get("first"));
                    assertEquals(true, response.get("last"));
                })
                .verifyComplete();
    }

    @Test
    void buildPageResponseWithDetailsShouldHandleFirstPageCorrectly() {
        // Arrange
        Application app = Application.builder()
                .applicationId(1L)
                .documentId("12345678")
                .email("test@example.com")
                .amount(new BigDecimal("500000.00"))
                .term(12)
                .build();

        List<Application> applications = Arrays.asList(app);
        PageRequest pageRequest = PageRequest.of(0, 5); // First page
        PageResponse<Application> pageResponse = PageResponse.of(applications, pageRequest, 20L); // Total 20 elements

        ApplicationDetailResponse detail = ApplicationDetailResponse.builder()
                .amount(new BigDecimal("500000.00"))
                .term(12)
                .applicationState("Pendiente")
                .documentId("12345678")
                .email("test@example.com")
                .fullName("Test User")
                .baseSalary(new BigDecimal("3000000.00"))
                .loanTypeName("Test Loan")
                .interestRate(new BigDecimal("0.1000"))
                .totalMonthlyDebt(new BigDecimal("400000.00"))
                .build();

        when(applicationDetailService.buildDetailResponse(app))
                .thenReturn(Mono.just(detail));

        // Act
        Mono<Map<String, Object>> result = pageResponseMapper.buildPageResponseWithDetails(pageResponse);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(0, response.get("page"));
                    assertEquals(5, response.get("size"));
                    assertEquals(20L, response.get("totalElements"));
                    assertEquals(true, response.get("first")); // First page
                    assertEquals(false, response.get("last")); // Not last page (20 elements, 5 per page = 4 pages)
                })
                .verifyComplete();
    }

    @Test
    void buildPageResponseWithDetailsShouldHandleMiddlePageCorrectly() {
        // Arrange
        Application app = Application.builder()
                .applicationId(5L)
                .documentId("55555555")
                .email("middle@example.com")
                .amount(new BigDecimal("750000.00"))
                .term(18)
                .build();

        List<Application> applications = Arrays.asList(app);
        PageRequest pageRequest = PageRequest.of(2, 5); // Middle page (page 2, 0-indexed)
        PageResponse<Application> pageResponse = PageResponse.of(applications, pageRequest, 20L);

        ApplicationDetailResponse detail = ApplicationDetailResponse.builder()
                .amount(new BigDecimal("750000.00"))
                .term(18)
                .applicationState("Revision")
                .documentId("55555555")
                .email("middle@example.com")
                .fullName("Middle User")
                .baseSalary(new BigDecimal("4000000.00"))
                .loanTypeName("Middle Loan")
                .interestRate(new BigDecimal("0.1100"))
                .totalMonthlyDebt(new BigDecimal("600000.00"))
                .build();

        when(applicationDetailService.buildDetailResponse(app))
                .thenReturn(Mono.just(detail));

        // Act
        Mono<Map<String, Object>> result = pageResponseMapper.buildPageResponseWithDetails(pageResponse);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(2, response.get("page"));
                    assertEquals(5, response.get("size"));
                    assertEquals(20L, response.get("totalElements"));
                    assertEquals(false, response.get("first")); // Not first page
                    assertEquals(false, response.get("last")); // Not last page
                })
                .verifyComplete();
    }

    @Test
    void buildPageResponseWithDetailsShouldHandleLastPageCorrectly() {
        // Arrange
        Application app = Application.builder()
                .applicationId(10L)
                .documentId("99999999")
                .email("last@example.com")
                .amount(new BigDecimal("200000.00"))
                .term(6)
                .build();

        List<Application> applications = Arrays.asList(app);
        PageRequest pageRequest = PageRequest.of(3, 5); // Last page (page 3, 0-indexed)
        PageResponse<Application> pageResponse = PageResponse.of(applications, pageRequest, 16L); // 16 elements total

        ApplicationDetailResponse detail = ApplicationDetailResponse.builder()
                .amount(new BigDecimal("200000.00"))
                .term(6)
                .applicationState("Rechazada")
                .documentId("99999999")
                .email("last@example.com")
                .fullName("Last User")
                .baseSalary(new BigDecimal("2000000.00"))
                .loanTypeName("Last Loan")
                .interestRate(new BigDecimal("0.1300"))
                .totalMonthlyDebt(new BigDecimal("300000.00"))
                .build();

        when(applicationDetailService.buildDetailResponse(app))
                .thenReturn(Mono.just(detail));

        // Act
        Mono<Map<String, Object>> result = pageResponseMapper.buildPageResponseWithDetails(pageResponse);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(3, response.get("page"));
                    assertEquals(5, response.get("size"));
                    assertEquals(16L, response.get("totalElements"));
                    assertEquals(false, response.get("first")); // Not first page
                    assertEquals(true, response.get("last")); // Last page
                })
                .verifyComplete();
    }

    @Test
    void buildPageResponseWithDetailsShouldHandleSinglePageCorrectly() {
        // Arrange
        Application app = Application.builder()
                .applicationId(1L)
                .documentId("11111111")
                .email("single@example.com")
                .amount(new BigDecimal("300000.00"))
                .term(9)
                .build();

        List<Application> applications = Arrays.asList(app);
        PageRequest pageRequest = PageRequest.of(0, 10);
        PageResponse<Application> pageResponse = PageResponse.of(applications, pageRequest, 1L); // Only 1 element

        ApplicationDetailResponse detail = ApplicationDetailResponse.builder()
                .amount(new BigDecimal("300000.00"))
                .term(9)
                .applicationState("Aprobada")
                .documentId("11111111")
                .email("single@example.com")
                .fullName("Single User")
                .baseSalary(new BigDecimal("2500000.00"))
                .loanTypeName("Single Loan")
                .interestRate(new BigDecimal("0.0950"))
                .totalMonthlyDebt(new BigDecimal("250000.00"))
                .build();

        when(applicationDetailService.buildDetailResponse(app))
                .thenReturn(Mono.just(detail));

        // Act
        Mono<Map<String, Object>> result = pageResponseMapper.buildPageResponseWithDetails(pageResponse);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(0, response.get("page"));
                    assertEquals(10, response.get("size"));
                    assertEquals(1L, response.get("totalElements"));
                    assertEquals(true, response.get("first")); // First and only page
                    assertEquals(true, response.get("last")); // Last and only page

                    @SuppressWarnings("unchecked")
                    List<ApplicationDetailResponse> content = (List<ApplicationDetailResponse>) response.get("content");
                    assertEquals(1, content.size());
                    assertEquals("Single User", content.get(0).getFullName());
                })
                .verifyComplete();
    }

    @Test
    void buildPageResponseWithDetailsShouldHandleServiceErrorCorrectly() {
        // Arrange
        Application app = Application.builder()
                .applicationId(1L)
                .documentId("ERROR123")
                .email("error@example.com")
                .amount(new BigDecimal("100000.00"))
                .term(12)
                .build();

        List<Application> applications = Arrays.asList(app);
        PageRequest pageRequest = PageRequest.of(0, 10);
        PageResponse<Application> pageResponse = PageResponse.of(applications, pageRequest, 1L);

        when(applicationDetailService.buildDetailResponse(app))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        // Act
        Mono<Map<String, Object>> result = pageResponseMapper.buildPageResponseWithDetails(pageResponse);

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void buildPageResponseWithDetailsShouldHandleLargePageSizeCorrectly() {
        // Arrange
        Application app = Application.builder()
                .applicationId(1L)
                .documentId("LARGE123")
                .email("large@example.com")
                .amount(new BigDecimal("1500000.00"))
                .term(36)
                .build();

        List<Application> applications = Arrays.asList(app);
        PageRequest pageRequest = PageRequest.of(0, 1000); // Large page size
        PageResponse<Application> pageResponse = PageResponse.of(applications, pageRequest, 1L);

        ApplicationDetailResponse detail = ApplicationDetailResponse.builder()
                .amount(new BigDecimal("1500000.00"))
                .term(36)
                .applicationState("Pendiente")
                .documentId("LARGE123")
                .email("large@example.com")
                .fullName("Large User")
                .baseSalary(new BigDecimal("6000000.00"))
                .loanTypeName("Large Loan")
                .interestRate(new BigDecimal("0.0800"))
                .totalMonthlyDebt(new BigDecimal("900000.00"))
                .build();

        when(applicationDetailService.buildDetailResponse(app))
                .thenReturn(Mono.just(detail));

        // Act
        Mono<Map<String, Object>> result = pageResponseMapper.buildPageResponseWithDetails(pageResponse);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(0, response.get("page"));
                    assertEquals(1000, response.get("size")); // Large page size
                    assertEquals(1L, response.get("totalElements"));
                    assertEquals(true, response.get("first"));
                    assertEquals(true, response.get("last"));

                    @SuppressWarnings("unchecked")
                    List<ApplicationDetailResponse> content = (List<ApplicationDetailResponse>) response.get("content");
                    assertEquals(1, content.size());
                })
                .verifyComplete();
    }

    @Test
    void buildPageResponseWithDetailsShouldMaintainOrderOfApplications() {
        // Arrange
        Application app1 = Application.builder()
                .applicationId(1L)
                .documentId("FIRST")
                .email("first@example.com")
                .amount(new BigDecimal("100000.00"))
                .term(6)
                .build();

        Application app2 = Application.builder()
                .applicationId(2L)
                .documentId("SECOND")
                .email("second@example.com")
                .amount(new BigDecimal("200000.00"))
                .term(12)
                .build();

        Application app3 = Application.builder()
                .applicationId(3L)
                .documentId("THIRD")
                .email("third@example.com")
                .amount(new BigDecimal("300000.00"))
                .term(18)
                .build();

        List<Application> applications = Arrays.asList(app1, app2, app3);
        PageRequest pageRequest = PageRequest.of(0, 10);
        PageResponse<Application> pageResponse = PageResponse.of(applications, pageRequest, 3L);

        ApplicationDetailResponse detail1 = ApplicationDetailResponse.builder()
                .documentId("FIRST")
                .fullName("First User")
                .amount(new BigDecimal("100000.00"))
                .build();

        ApplicationDetailResponse detail2 = ApplicationDetailResponse.builder()
                .documentId("SECOND")
                .fullName("Second User")
                .amount(new BigDecimal("200000.00"))
                .build();

        ApplicationDetailResponse detail3 = ApplicationDetailResponse.builder()
                .documentId("THIRD")
                .fullName("Third User")
                .amount(new BigDecimal("300000.00"))
                .build();

        when(applicationDetailService.buildDetailResponse(app1))
                .thenReturn(Mono.just(detail1));
        when(applicationDetailService.buildDetailResponse(app2))
                .thenReturn(Mono.just(detail2));
        when(applicationDetailService.buildDetailResponse(app3))
                .thenReturn(Mono.just(detail3));

        // Act
        Mono<Map<String, Object>> result = pageResponseMapper.buildPageResponseWithDetails(pageResponse);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    @SuppressWarnings("unchecked")
                    List<ApplicationDetailResponse> content = (List<ApplicationDetailResponse>) response.get("content");
                    assertEquals(3, content.size());

                    // Verify order is maintained
                    assertEquals("FIRST", content.get(0).getDocumentId());
                    assertEquals("SECOND", content.get(1).getDocumentId());
                    assertEquals("THIRD", content.get(2).getDocumentId());
                })
                .verifyComplete();
    }
}
