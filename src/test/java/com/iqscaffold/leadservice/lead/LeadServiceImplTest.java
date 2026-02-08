package com.iqscaffold.leadservice.lead;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.iqscaffold.leadservice.event.LeadEventPublisher;
import com.iqscaffold.leadservice.infrastructure.client.ContactServiceClient;
import com.iqscaffold.leadservice.lead.dto.LeadDtos;
import com.iqscaffold.leadservice.shared.exception.DuplicateResourceException;
import com.iqscaffold.leadservice.shared.exception.LeadNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeadServiceImpl Unit Tests")
class LeadServiceImplTest {

  @Mock
  private LeadRepository leadRepository;

  @Mock
  private ContactServiceClient contactServiceClient;

  @Mock
  private LeadEventPublisher leadEventPublisher;

  @Mock
  private ConversionOrchestrator conversionOrchestrator;

  private LeadServiceImpl leadService;

  @BeforeEach
  void setUp() {
    leadService = new LeadServiceImpl(
        leadRepository,
        contactServiceClient,
        leadEventPublisher,
        conversionOrchestrator
    );
  }

  @Test
  @DisplayName("Should create lead entity successfully")
  void shouldCreateLeadEntity() {
    // Arrange
    Lead lead = createTestLead();
    when(leadRepository.save(any(Lead.class))).thenReturn(lead);

    // Act
    Lead result = leadService.createLead(lead);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
    verify(leadRepository).save(lead);
    verify(leadEventPublisher).publishLeadCreated(lead);
  }

  @Test
  @DisplayName("Should create lead from request successfully")
  void shouldCreateLeadFromRequest() {
    // Arrange
    LeadDtos.CreateLeadRequest request = new LeadDtos.CreateLeadRequest(
        "John", "Doe", "john.doe@example.com", "+1234567890",
        "Test Company", "Developer", "Website", "Test notes", null
    );
    Lead savedLead = createTestLead();
    when(leadRepository.existsByEmail(request.email())).thenReturn(false);
    when(leadRepository.save(any(Lead.class))).thenReturn(savedLead);

    // Act
    LeadDtos.LeadResponse result = leadService.createLead(request, "test-user");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.email()).isEqualTo("john.doe@example.com");
    verify(leadRepository).existsByEmail(request.email());
    verify(leadRepository).save(any(Lead.class));
    verify(leadEventPublisher).publishLeadCreated(any(Lead.class));
  }

  @Test
  @DisplayName("Should throw exception when creating lead with duplicate email")
  void shouldThrowExceptionWhenCreatingLeadWithDuplicateEmail() {
    // Arrange
    LeadDtos.CreateLeadRequest request = new LeadDtos.CreateLeadRequest(
        "John", "Doe", "john.doe@example.com", "+1234567890",
        "Test Company", "Developer", "Website", "Test notes", null
    );
    when(leadRepository.existsByEmail(request.email())).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> leadService.createLead(request, "test-user"))
        .isInstanceOf(DuplicateResourceException.class)
        .hasMessageContaining("already exists");

    verify(leadRepository).existsByEmail(request.email());
    verify(leadRepository, never()).save(any(Lead.class));
    verify(leadEventPublisher, never()).publishLeadCreated(any(Lead.class));
  }

  @Test
  @DisplayName("Should get lead by ID successfully")
  void shouldGetLeadById() {
    // Arrange
    Long leadId = 1L;
    Lead lead = createTestLead();
    when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

    // Act
    Optional<Lead> result = leadService.getLeadById(leadId);

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get().getEmail()).isEqualTo("john.doe@example.com");
    verify(leadRepository).findById(leadId);
  }

  @Test
  @DisplayName("Should return empty when lead not found by ID")
  void shouldReturnEmptyWhenLeadNotFoundById() {
    // Arrange
    Long leadId = 999L;
    when(leadRepository.findById(leadId)).thenReturn(Optional.empty());

    // Act
    Optional<Lead> result = leadService.getLeadById(leadId);

    // Assert
    assertThat(result).isEmpty();
    verify(leadRepository).findById(leadId);
  }

  @Test
  @DisplayName("Should get lead response by ID successfully")
  void shouldGetLeadResponseById() {
    // Arrange
    Long leadId = 1L;
    Lead lead = createTestLead();
    when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

    // Act
    LeadDtos.LeadResponse result = leadService.getLeadResponseById(leadId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.email()).isEqualTo("john.doe@example.com");
    verify(leadRepository).findById(leadId);
  }

  @Test
  @DisplayName("Should throw exception when getting lead response for non-existent ID")
  void shouldThrowExceptionWhenGettingLeadResponseForNonExistentId() {
    // Arrange
    Long leadId = 999L;
    when(leadRepository.findById(leadId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> leadService.getLeadResponseById(leadId))
        .isInstanceOf(LeadNotFoundException.class)
        .hasMessageContaining("not found");

    verify(leadRepository).findById(leadId);
  }

  @Test
  @DisplayName("Should get lead by email successfully")
  void shouldGetLeadByEmail() {
    // Arrange
    String email = "john.doe@example.com";
    Lead lead = createTestLead();
    when(leadRepository.findByEmail(email)).thenReturn(Optional.of(lead));

    // Act
    Optional<Lead> result = leadService.getLeadByEmail(email);

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get().getEmail()).isEqualTo(email);
    verify(leadRepository).findByEmail(email);
  }

  @Test
  @DisplayName("Should get all leads with pagination")
  void shouldGetAllLeadsWithPagination() {
    // Arrange
    Pageable pageable = PageRequest.of(0, 10);
    List<Lead> leads = List.of(createTestLead());
    Page<Lead> page = new PageImpl<>(leads, pageable, leads.size());
    when(leadRepository.findAll(pageable)).thenReturn(page);

    // Act
    Page<Lead> result = leadService.getAllLeads(pageable);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    verify(leadRepository).findAll(pageable);
  }

  @Test
  @DisplayName("Should get leads by status")
  void shouldGetLeadsByStatus() {
    // Arrange
    LeadStatus status = LeadStatus.NEW;
    Pageable pageable = PageRequest.of(0, 10);
    List<Lead> leads = List.of(createTestLead());
    Page<Lead> page = new PageImpl<>(leads, pageable, leads.size());
    when(leadRepository.findByStatus(status, pageable)).thenReturn(page);

    // Act
    Page<Lead> result = leadService.getLeadsByStatus(status, pageable);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    verify(leadRepository).findByStatus(status, pageable);
  }

  @Test
  @DisplayName("Should get leads by assigned user")
  void shouldGetLeadsByAssignedTo() {
    // Arrange
    String assignedTo = "user123";
    Pageable pageable = PageRequest.of(0, 10);
    List<Lead> leads = List.of(createTestLead());
    Page<Lead> page = new PageImpl<>(leads, pageable, leads.size());
    when(leadRepository.findByAssignedTo(assignedTo, pageable)).thenReturn(page);

    // Act
    Page<Lead> result = leadService.getLeadsByAssignedTo(assignedTo, pageable);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    verify(leadRepository).findByAssignedTo(assignedTo, pageable);
  }

  @Test
  @DisplayName("Should search leads")
  void shouldSearchLeads() {
    // Arrange
    String searchTerm = "john";
    Pageable pageable = PageRequest.of(0, 10);
    List<Lead> leads = List.of(createTestLead());
    Page<Lead> page = new PageImpl<>(leads, pageable, leads.size());
    when(leadRepository.searchLeads(searchTerm, pageable)).thenReturn(page);

    // Act
    Page<Lead> result = leadService.searchLeads(searchTerm, pageable);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    verify(leadRepository).searchLeads(searchTerm, pageable);
  }

  @Test
  @DisplayName("Should update lead entity successfully")
  void shouldUpdateLeadEntity() {
    // Arrange
    Long leadId = 1L;
    Lead existingLead = createTestLead();
    Lead updateData = new Lead();
    updateData.setFirstName("Jane");
    updateData.setLastName("Smith");
    updateData.setEmail("jane.smith@example.com");
    updateData.setPhone("+9876543210");
    updateData.setCompany("New Company");
    updateData.setJobTitle("Manager");
    updateData.setSource("Referral");
    updateData.setNotes("Updated notes");
    updateData.setUpdatedBy("admin");

    when(leadRepository.findById(leadId)).thenReturn(Optional.of(existingLead));
    when(leadRepository.save(any(Lead.class))).thenReturn(existingLead);

    // Act
    Lead result = leadService.updateLead(leadId, updateData);

    // Assert
    assertThat(result).isNotNull();
    verify(leadRepository).findById(leadId);
    verify(leadRepository).save(existingLead);
    verify(leadEventPublisher).publishLeadUpdated(existingLead);
  }

  @Test
  @DisplayName("Should throw exception when updating non-existent lead")
  void shouldThrowExceptionWhenUpdatingNonExistentLead() {
    // Arrange
    Long leadId = 999L;
    Lead updateData = new Lead();
    when(leadRepository.findById(leadId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> leadService.updateLead(leadId, updateData))
        .isInstanceOf(LeadNotFoundException.class)
        .hasMessageContaining("not found");

    verify(leadRepository).findById(leadId);
    verify(leadRepository, never()).save(any(Lead.class));
  }

  @Test
  @DisplayName("Should update lead from request successfully")
  void shouldUpdateLeadFromRequest() {
    // Arrange
    Long leadId = 1L;
    Lead existingLead = createTestLead();
    LeadDtos.UpdateLeadRequest request = new LeadDtos.UpdateLeadRequest(
        "Jane", "Smith", "jane.smith@example.com", "+9876543210",
        "New Company", "Manager", "Referral", "Updated notes", null
    );

    when(leadRepository.findById(leadId)).thenReturn(Optional.of(existingLead));
    when(leadRepository.existsByEmail(request.email())).thenReturn(false);
    when(leadRepository.save(any(Lead.class))).thenReturn(existingLead);

    // Act
    LeadDtos.LeadResponse result = leadService.updateLead(leadId, request, "admin");

    // Assert
    assertThat(result).isNotNull();
    verify(leadRepository).findById(leadId);
    verify(leadRepository).save(existingLead);
    verify(leadEventPublisher).publishLeadUpdated(existingLead);
  }

  @Test
  @DisplayName("Should throw exception when updating lead with duplicate email")
  void shouldThrowExceptionWhenUpdatingLeadWithDuplicateEmail() {
    // Arrange
    Long leadId = 1L;
    Lead existingLead = createTestLead();
    existingLead.setEmail("old.email@example.com");
    LeadDtos.UpdateLeadRequest request = new LeadDtos.UpdateLeadRequest(
        "Jane", "Smith", "duplicate@example.com", "+9876543210",
        "New Company", "Manager", "Referral", "Updated notes", null
    );

    when(leadRepository.findById(leadId)).thenReturn(Optional.of(existingLead));
    when(leadRepository.existsByEmail(request.email())).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> leadService.updateLead(leadId, request, "admin"))
        .isInstanceOf(DuplicateResourceException.class)
        .hasMessageContaining("already exists");

    verify(leadRepository).findById(leadId);
    verify(leadRepository, never()).save(any(Lead.class));
  }

  @Test
  @DisplayName("Should update lead status successfully")
  void shouldUpdateLeadStatus() {
    // Arrange
    Long leadId = 1L;
    Lead lead = createTestLead();
    LeadStatus newStatus = LeadStatus.CONTACTED;
    when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
    when(leadRepository.save(any(Lead.class))).thenReturn(lead);

    // Act
    Lead result = leadService.updateLeadStatus(leadId, newStatus);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo(newStatus);
    verify(leadRepository).findById(leadId);
    verify(leadRepository).save(lead);
  }

  @Test
  @DisplayName("Should update lead score successfully")
  void shouldUpdateLeadScore() {
    // Arrange
    Long leadId = 1L;
    Lead lead = createTestLead();
    Integer newScore = 95;
    when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
    when(leadRepository.save(any(Lead.class))).thenReturn(lead);

    // Act
    Lead result = leadService.updateLeadScore(leadId, newScore);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getScore()).isEqualTo(newScore);
    verify(leadRepository).findById(leadId);
    verify(leadRepository).save(lead);
  }

  @Test
  @DisplayName("Should qualify lead successfully")
  void shouldQualifyLead() {
    // Arrange
    Long leadId = 1L;
    Lead lead = createTestLead();
    lead.setQualified(false);
    when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
    when(leadRepository.save(any(Lead.class))).thenReturn(lead);

    // Act
    Lead result = leadService.qualifyLead(leadId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getQualified()).isTrue();
    assertThat(result.getStatus()).isEqualTo(LeadStatus.QUALIFIED);
    verify(leadRepository).findById(leadId);
    verify(leadRepository).save(lead);
  }

  @Test
  @DisplayName("Should disqualify lead successfully")
  void shouldDisqualifyLead() {
    // Arrange
    Long leadId = 1L;
    Lead lead = createTestLead();
    lead.setQualified(true);
    when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
    when(leadRepository.save(any(Lead.class))).thenReturn(lead);

    // Act
    Lead result = leadService.disqualifyLead(leadId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getQualified()).isFalse();
    assertThat(result.getStatus()).isEqualTo(LeadStatus.UNQUALIFIED);
    verify(leadRepository).findById(leadId);
    verify(leadRepository).save(lead);
  }

  @Test
  @DisplayName("Should assign lead successfully")
  void shouldAssignLead() {
    // Arrange
    Long leadId = 1L;
    String assignedTo = "user123";
    Lead lead = createTestLead();
    when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
    when(leadRepository.save(any(Lead.class))).thenReturn(lead);

    // Act
    Lead result = leadService.assignLead(leadId, assignedTo);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getAssignedTo()).isEqualTo(assignedTo);
    verify(leadRepository).findById(leadId);
    verify(leadRepository).save(lead);
  }

  @Test
  @DisplayName("Should convert lead successfully")
  void shouldConvertLead() {
    // Arrange
    Long leadId = 1L;
    Long contactId = 100L;
    Lead lead = createTestLead();
    when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
    when(leadRepository.save(any(Lead.class))).thenReturn(lead);

    // Act
    Lead result = leadService.convertLead(leadId, contactId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo(LeadStatus.CONVERTED);
    assertThat(result.getConvertedToContactId()).isEqualTo(contactId);
    assertThat(result.getConvertedAt()).isNotNull();
    verify(leadRepository).findById(leadId);
    verify(leadRepository).save(lead);
  }

  @Test
  @DisplayName("Should convert lead to contact using orchestrator")
  void shouldConvertLeadToContactUsingOrchestrator() {
    // Arrange
    Long leadId = 1L;
    LeadDtos.ConvertLeadRequest request = new LeadDtos.ConvertLeadRequest(null, "Test notes");
    String bearerToken = "Bearer test-token";
    LeadDtos.ConvertLeadResponse expectedResponse = new LeadDtos.ConvertLeadResponse(
        leadId, 100L, LocalDateTime.now(), "Lead converted successfully"
    );

    when(conversionOrchestrator.convertLead(eq(leadId), eq(request), eq(bearerToken), anyString()))
        .thenReturn(expectedResponse);

    // Act
    LeadDtos.ConvertLeadResponse result = leadService.convertLeadToContact(leadId, request, bearerToken);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.leadId()).isEqualTo(leadId);
    assertThat(result.contactId()).isEqualTo(100L);
    verify(conversionOrchestrator).convertLead(eq(leadId), eq(request), eq(bearerToken), anyString());
  }

  @Test
  @DisplayName("Should delete lead successfully")
  void shouldDeleteLead() {
    // Arrange
    Long leadId = 1L;
    Lead lead = createTestLead();
    when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

    // Act
    leadService.deleteLead(leadId);

    // Assert
    verify(leadRepository).findById(leadId);
    verify(leadRepository).deleteById(leadId);
    verify(leadEventPublisher).publishLeadDeleted(leadId, lead.getEmail());
  }

  @Test
  @DisplayName("Should throw exception when deleting non-existent lead")
  void shouldThrowExceptionWhenDeletingNonExistentLead() {
    // Arrange
    Long leadId = 999L;
    when(leadRepository.findById(leadId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> leadService.deleteLead(leadId))
        .isInstanceOf(LeadNotFoundException.class)
        .hasMessageContaining("not found");

    verify(leadRepository).findById(leadId);
    verify(leadRepository, never()).deleteById(any());
  }

  @Test
  @DisplayName("Should check if lead exists by email")
  void shouldCheckIfLeadExistsByEmail() {
    // Arrange
    String email = "john.doe@example.com";
    when(leadRepository.existsByEmail(email)).thenReturn(true);

    // Act
    boolean result = leadService.existsByEmail(email);

    // Assert
    assertThat(result).isTrue();
    verify(leadRepository).existsByEmail(email);
  }

  @Test
  @DisplayName("Should get lead count by status")
  void shouldGetLeadCountByStatus() {
    // Arrange
    LeadStatus status = LeadStatus.NEW;
    when(leadRepository.countByStatus(status)).thenReturn(5L);

    // Act
    long result = leadService.getLeadCountByStatus(status);

    // Assert
    assertThat(result).isEqualTo(5L);
    verify(leadRepository).countByStatus(status);
  }

  @Test
  @DisplayName("Should get lead count by source")
  void shouldGetLeadCountBySource() {
    // Arrange
    String source = "Website";
    when(leadRepository.countBySource(source)).thenReturn(10L);

    // Act
    long result = leadService.getLeadCountBySource(source);

    // Assert
    assertThat(result).isEqualTo(10L);
    verify(leadRepository).countBySource(source);
  }

  @Test
  @DisplayName("Should get qualified lead count")
  void shouldGetQualifiedLeadCount() {
    // Arrange
    when(leadRepository.countByQualified(true)).thenReturn(15L);

    // Act
    long result = leadService.getQualifiedLeadCount();

    // Assert
    assertThat(result).isEqualTo(15L);
    verify(leadRepository).countByQualified(true);
  }

  @Test
  @DisplayName("Should get leads created since date")
  void shouldGetLeadsCreatedSince() {
    // Arrange
    LocalDateTime date = LocalDateTime.now().minusDays(7);
    when(leadRepository.countLeadsCreatedSince(date)).thenReturn(20L);

    // Act
    long result = leadService.getLeadsCreatedSince(date);

    // Assert
    assertThat(result).isEqualTo(20L);
    verify(leadRepository).countLeadsCreatedSince(date);
  }

  @Test
  @DisplayName("Should get lead with notes using entity graph")
  void shouldGetLeadWithNotes() {
    // Arrange
    Long leadId = 1L;
    Lead lead = createTestLead();
    when(leadRepository.findWithNotesById(leadId)).thenReturn(Optional.of(lead));

    // Act
    Optional<Lead> result = leadService.getLeadWithNotes(leadId);

    // Assert
    assertThat(result).isPresent();
    verify(leadRepository).findWithNotesById(leadId);
  }

  @Test
  @DisplayName("Should get lead with activities using entity graph")
  void shouldGetLeadWithActivities() {
    // Arrange
    Long leadId = 1L;
    Lead lead = createTestLead();
    when(leadRepository.findWithActivitiesById(leadId)).thenReturn(Optional.of(lead));

    // Act
    Optional<Lead> result = leadService.getLeadWithActivities(leadId);

    // Assert
    assertThat(result).isPresent();
    verify(leadRepository).findWithActivitiesById(leadId);
  }

  @Test
  @DisplayName("Should get lead with complete history using entity graph")
  void shouldGetLeadWithCompleteHistory() {
    // Arrange
    Long leadId = 1L;
    Lead lead = createTestLead();
    when(leadRepository.findWithNotesAndActivitiesById(leadId)).thenReturn(Optional.of(lead));

    // Act
    Optional<Lead> result = leadService.getLeadWithCompleteHistory(leadId);

    // Assert
    assertThat(result).isPresent();
    verify(leadRepository).findWithNotesAndActivitiesById(leadId);
  }

  private Lead createTestLead() {
    Lead lead = new Lead();
    lead.setId(1L);
    lead.setFirstName("John");
    lead.setLastName("Doe");
    lead.setEmail("john.doe@example.com");
    lead.setPhone("+1234567890");
    lead.setCompany("Test Company");
    lead.setJobTitle("Developer");
    lead.setSource("Website");
    lead.setStatus(LeadStatus.NEW);
    lead.setScore(75);
    lead.setQualified(false);
    lead.setNotes("Test notes");
    lead.setCreatedBy("system");
    lead.setUpdatedBy("system");
    lead.setCreatedAt(LocalDateTime.now());
    lead.setUpdatedAt(LocalDateTime.now());
    return lead;
  }
}
