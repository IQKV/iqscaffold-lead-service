package com.iqscaffold.leadservice.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqscaffold.leadservice.lead.Lead;
import com.iqscaffold.leadservice.lead.LeadRepository;
import com.iqscaffold.leadservice.lead.LeadStatus;
import com.iqscaffold.leadservice.note.LeadNote;
import com.iqscaffold.leadservice.shared.exception.LeadNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityLogServiceImpl Unit Tests")
class ActivityLogServiceImplTest {

  @Mock
  private LeadActivityRepository activityRepository;

  @Mock
  private LeadRepository leadRepository;

  private ActivityLogServiceImpl activityLogService;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    activityLogService = new ActivityLogServiceImpl(
        activityRepository,
        leadRepository,
        objectMapper
    );
  }

  @Test
  @DisplayName("Should log lead created activity")
  void shouldLogLeadCreated() {
    // Arrange
    Lead lead = createTestLead(1L);
    ArgumentCaptor<LeadActivity> activityCaptor = ArgumentCaptor.forClass(LeadActivity.class);

    // Act
    activityLogService.logLeadCreated(lead);

    // Assert
    verify(activityRepository).save(activityCaptor.capture());
    LeadActivity savedActivity = activityCaptor.getValue();
    assertThat(savedActivity.getLead()).isEqualTo(lead);
    assertThat(savedActivity.getType()).isEqualTo(ActivityType.LEAD_CREATED);
    assertThat(savedActivity.getDescription()).contains("Lead created");
    assertThat(savedActivity.getDescription()).contains(lead.getFullName());
    assertThat(savedActivity.getCreatedBy()).isEqualTo(lead.getCreatedBy());
  }

  @Test
  @DisplayName("Should log lead updated activity")
  void shouldLogLeadUpdated() {
    // Arrange
    Lead lead = createTestLead(1L);
    ArgumentCaptor<LeadActivity> activityCaptor = ArgumentCaptor.forClass(LeadActivity.class);

    // Act
    activityLogService.logLeadUpdated(lead);

    // Assert
    verify(activityRepository).save(activityCaptor.capture());
    LeadActivity savedActivity = activityCaptor.getValue();
    assertThat(savedActivity.getLead()).isEqualTo(lead);
    assertThat(savedActivity.getType()).isEqualTo(ActivityType.LEAD_UPDATED);
    assertThat(savedActivity.getDescription()).contains("Lead updated");
    assertThat(savedActivity.getCreatedBy()).isEqualTo(lead.getUpdatedBy());
  }

  @Test
  @DisplayName("Should log lead deleted activity")
  void shouldLogLeadDeleted() {
    // Arrange
    Long leadId = 1L;
    Lead lead = createTestLead(leadId);
    String deletedBy = "admin";
    ArgumentCaptor<LeadActivity> activityCaptor = ArgumentCaptor.forClass(LeadActivity.class);

    when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

    // Act
    activityLogService.logLeadDeleted(leadId, deletedBy);

    // Assert
    verify(leadRepository).findById(leadId);
    verify(activityRepository).save(activityCaptor.capture());
    LeadActivity savedActivity = activityCaptor.getValue();
    assertThat(savedActivity.getLead()).isEqualTo(lead);
    assertThat(savedActivity.getType()).isEqualTo(ActivityType.LEAD_DELETED);
    assertThat(savedActivity.getDescription()).contains("Lead deleted");
    assertThat(savedActivity.getCreatedBy()).isEqualTo(deletedBy);
  }

  @Test
  @DisplayName("Should throw exception when logging deleted activity for non-existent lead")
  void shouldThrowExceptionWhenLoggingDeletedActivityForNonExistentLead() {
    // Arrange
    Long leadId = 999L;
    String deletedBy = "admin";

    when(leadRepository.findById(leadId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> activityLogService.logLeadDeleted(leadId, deletedBy))
        .isInstanceOf(LeadNotFoundException.class)
        .hasMessageContaining("not found");

    verify(leadRepository).findById(leadId);
    verify(activityRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should log note added activity")
  void shouldLogNoteAdded() {
    // Arrange
    Lead lead = createTestLead(1L);
    LeadNote note = new LeadNote(lead, "Test note content", "user1");
    note.setId(10L);
    ArgumentCaptor<LeadActivity> activityCaptor = ArgumentCaptor.forClass(LeadActivity.class);

    // Act
    activityLogService.logNoteAdded(lead, note);

    // Assert
    verify(activityRepository).save(activityCaptor.capture());
    LeadActivity savedActivity = activityCaptor.getValue();
    assertThat(savedActivity.getLead()).isEqualTo(lead);
    assertThat(savedActivity.getType()).isEqualTo(ActivityType.NOTE_ADDED);
    assertThat(savedActivity.getDescription()).contains("Note added");
    assertThat(savedActivity.getCreatedBy()).isEqualTo(note.getCreatedBy());
  }

  @Test
  @DisplayName("Should log note updated activity")
  void shouldLogNoteUpdated() {
    // Arrange
    Lead lead = createTestLead(1L);
    LeadNote note = new LeadNote(lead, "Updated note content", "user1");
    note.setId(10L);
    note.setUpdatedBy("admin");
    ArgumentCaptor<LeadActivity> activityCaptor = ArgumentCaptor.forClass(LeadActivity.class);

    // Act
    activityLogService.logNoteUpdated(lead, note);

    // Assert
    verify(activityRepository).save(activityCaptor.capture());
    LeadActivity savedActivity = activityCaptor.getValue();
    assertThat(savedActivity.getLead()).isEqualTo(lead);
    assertThat(savedActivity.getType()).isEqualTo(ActivityType.NOTE_UPDATED);
    assertThat(savedActivity.getDescription()).contains("Note updated");
    assertThat(savedActivity.getCreatedBy()).isEqualTo(note.getUpdatedBy());
  }

  @Test
  @DisplayName("Should log note deleted activity")
  void shouldLogNoteDeleted() {
    // Arrange
    Lead lead = createTestLead(1L);
    Long noteId = 10L;
    String deletedBy = "admin";
    ArgumentCaptor<LeadActivity> activityCaptor = ArgumentCaptor.forClass(LeadActivity.class);

    // Act
    activityLogService.logNoteDeleted(lead, noteId, deletedBy);

    // Assert
    verify(activityRepository).save(activityCaptor.capture());
    LeadActivity savedActivity = activityCaptor.getValue();
    assertThat(savedActivity.getLead()).isEqualTo(lead);
    assertThat(savedActivity.getType()).isEqualTo(ActivityType.NOTE_DELETED);
    assertThat(savedActivity.getDescription()).contains("Note deleted");
    assertThat(savedActivity.getCreatedBy()).isEqualTo(deletedBy);
  }

  @Test
  @DisplayName("Should log stage change activity")
  void shouldLogStageChange() {
    // Arrange
    Long leadId = 1L;
    Lead lead = createTestLead(leadId);
    Long oldStageId = 1L;
    Long newStageId = 2L;
    String changedBy = "user1";
    ArgumentCaptor<LeadActivity> activityCaptor = ArgumentCaptor.forClass(LeadActivity.class);

    when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

    // Act
    activityLogService.logStageChange(leadId, oldStageId, newStageId, changedBy);

    // Assert
    verify(leadRepository).findById(leadId);
    verify(activityRepository).save(activityCaptor.capture());
    LeadActivity savedActivity = activityCaptor.getValue();
    assertThat(savedActivity.getLead()).isEqualTo(lead);
    assertThat(savedActivity.getType()).isEqualTo(ActivityType.STAGE_CHANGED);
    assertThat(savedActivity.getDescription()).contains("moved from stage");
    assertThat(savedActivity.getCreatedBy()).isEqualTo(changedBy);
  }

  @Test
  @DisplayName("Should throw exception when logging stage change for non-existent lead")
  void shouldThrowExceptionWhenLoggingStageChangeForNonExistentLead() {
    // Arrange
    Long leadId = 999L;
    Long oldStageId = 1L;
    Long newStageId = 2L;
    String changedBy = "user1";

    when(leadRepository.findById(leadId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> activityLogService.logStageChange(leadId, oldStageId, newStageId, changedBy))
        .isInstanceOf(LeadNotFoundException.class)
        .hasMessageContaining("not found");

    verify(leadRepository).findById(leadId);
    verify(activityRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should log follow-up scheduled activity")
  void shouldLogFollowUpScheduled() {
    // Arrange
    Long leadId = 1L;
    Lead lead = createTestLead(leadId);
    Long followUpId = 100L;
    String scheduledBy = "user1";
    ArgumentCaptor<LeadActivity> activityCaptor = ArgumentCaptor.forClass(LeadActivity.class);

    when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

    // Act
    activityLogService.logFollowUpScheduled(leadId, followUpId, scheduledBy);

    // Assert
    verify(leadRepository).findById(leadId);
    verify(activityRepository).save(activityCaptor.capture());
    LeadActivity savedActivity = activityCaptor.getValue();
    assertThat(savedActivity.getLead()).isEqualTo(lead);
    assertThat(savedActivity.getType()).isEqualTo(ActivityType.FOLLOWUP_SCHEDULED);
    assertThat(savedActivity.getDescription()).contains("Follow-up scheduled");
    assertThat(savedActivity.getCreatedBy()).isEqualTo(scheduledBy);
  }

  @Test
  @DisplayName("Should log follow-up completed activity")
  void shouldLogFollowUpCompleted() {
    // Arrange
    Long leadId = 1L;
    Lead lead = createTestLead(leadId);
    Long followUpId = 100L;
    String completedBy = "user1";
    ArgumentCaptor<LeadActivity> activityCaptor = ArgumentCaptor.forClass(LeadActivity.class);

    when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

    // Act
    activityLogService.logFollowUpCompleted(leadId, followUpId, completedBy);

    // Assert
    verify(leadRepository).findById(leadId);
    verify(activityRepository).save(activityCaptor.capture());
    LeadActivity savedActivity = activityCaptor.getValue();
    assertThat(savedActivity.getLead()).isEqualTo(lead);
    assertThat(savedActivity.getType()).isEqualTo(ActivityType.FOLLOWUP_COMPLETED);
    assertThat(savedActivity.getDescription()).contains("Follow-up completed");
    assertThat(savedActivity.getCreatedBy()).isEqualTo(completedBy);
  }

  @Test
  @DisplayName("Should log lead converted activity")
  void shouldLogLeadConverted() {
    // Arrange
    Long leadId = 1L;
    Lead lead = createTestLead(leadId);
    lead.setConvertedToContactId(200L);
    lead.setConvertedAt(LocalDateTime.now());
    String description = "Lead converted to contact";
    ArgumentCaptor<LeadActivity> activityCaptor = ArgumentCaptor.forClass(LeadActivity.class);

    when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

    // Act
    activityLogService.logLeadConverted(leadId, description);

    // Assert
    verify(leadRepository).findById(leadId);
    verify(activityRepository).save(activityCaptor.capture());
    LeadActivity savedActivity = activityCaptor.getValue();
    assertThat(savedActivity.getLead()).isEqualTo(lead);
    assertThat(savedActivity.getType()).isEqualTo(ActivityType.LEAD_CONVERTED);
    assertThat(savedActivity.getDescription()).isEqualTo(description);
    assertThat(savedActivity.getCreatedBy()).isEqualTo("system");
  }

  @Test
  @DisplayName("Should throw exception when logging converted activity for non-existent lead")
  void shouldThrowExceptionWhenLoggingConvertedActivityForNonExistentLead() {
    // Arrange
    Long leadId = 999L;
    String description = "Lead converted";

    when(leadRepository.findById(leadId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> activityLogService.logLeadConverted(leadId, description))
        .isInstanceOf(LeadNotFoundException.class)
        .hasMessageContaining("not found");

    verify(leadRepository).findById(leadId);
    verify(activityRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should get lead activity timeline")
  void shouldGetLeadActivityTimeline() {
    // Arrange
    Long leadId = 1L;
    Lead lead = createTestLead(leadId);
    LeadActivity activity1 = new LeadActivity(lead, ActivityType.LEAD_CREATED, "Lead created", "system");
    LeadActivity activity2 = new LeadActivity(lead, ActivityType.NOTE_ADDED, "Note added", "user1");
    List<LeadActivity> activities = List.of(activity2, activity1); // Descending order

    when(leadRepository.existsById(leadId)).thenReturn(true);
    when(activityRepository.findByLeadIdOrderByCreatedAtDesc(leadId)).thenReturn(activities);

    // Act
    List<LeadActivity> result = activityLogService.getLeadActivityTimeline(leadId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getType()).isEqualTo(ActivityType.NOTE_ADDED);
    assertThat(result.get(1).getType()).isEqualTo(ActivityType.LEAD_CREATED);
    verify(leadRepository).existsById(leadId);
    verify(activityRepository).findByLeadIdOrderByCreatedAtDesc(leadId);
  }

  @Test
  @DisplayName("Should return empty list when lead has no activities")
  void shouldReturnEmptyListWhenLeadHasNoActivities() {
    // Arrange
    Long leadId = 1L;
    when(leadRepository.existsById(leadId)).thenReturn(true);
    when(activityRepository.findByLeadIdOrderByCreatedAtDesc(leadId)).thenReturn(List.of());

    // Act
    List<LeadActivity> result = activityLogService.getLeadActivityTimeline(leadId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
    verify(leadRepository).existsById(leadId);
    verify(activityRepository).findByLeadIdOrderByCreatedAtDesc(leadId);
  }

  @Test
  @DisplayName("Should throw exception when getting timeline for non-existent lead")
  void shouldThrowExceptionWhenGettingTimelineForNonExistentLead() {
    // Arrange
    Long leadId = 999L;
    when(leadRepository.existsById(leadId)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> activityLogService.getLeadActivityTimeline(leadId))
        .isInstanceOf(LeadNotFoundException.class)
        .hasMessageContaining("not found");

    verify(leadRepository).existsById(leadId);
    verify(activityRepository, never()).findByLeadIdOrderByCreatedAtDesc(any());
  }

  private Lead createTestLead(Long id) {
    Lead lead = new Lead();
    lead.setId(id);
    lead.setFirstName("John");
    lead.setLastName("Doe");
    lead.setEmail("john.doe@example.com");
    lead.setPhone("+1234567890");
    lead.setCompany("Test Company");
    lead.setJobTitle("Developer");
    lead.setSource("Website");
    lead.setStatus(LeadStatus.NEW);
    lead.setCreatedBy("system");
    lead.setUpdatedBy("system");
    lead.setCreatedAt(LocalDateTime.now());
    lead.setUpdatedAt(LocalDateTime.now());
    return lead;
  }
}
