package com.iqscaffold.leadservice.note;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.iqscaffold.leadservice.activity.ActivityLogService;
import com.iqscaffold.leadservice.lead.Lead;
import com.iqscaffold.leadservice.lead.LeadRepository;
import com.iqscaffold.leadservice.lead.LeadStatus;
import com.iqscaffold.leadservice.note.dto.LeadNoteDtos;
import com.iqscaffold.leadservice.shared.exception.LeadNotFoundException;
import com.iqscaffold.leadservice.shared.exception.LeadNoteNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeadNoteServiceImpl Unit Tests")
class LeadNoteServiceImplTest {

  @Mock
  private LeadNoteRepository leadNoteRepository;

  @Mock
  private LeadRepository leadRepository;

  @Mock
  private ActivityLogService activityLogService;

  private LeadNoteServiceImpl leadNoteService;

  @BeforeEach
  void setUp() {
    leadNoteService = new LeadNoteServiceImpl(
        leadNoteRepository,
        leadRepository,
        activityLogService
    );
  }

  @Test
  @DisplayName("Should create note successfully")
  void shouldCreateNote() {
    // Arrange
    Long leadId = 1L;
    Lead lead = createTestLead(leadId);
    LeadNoteDtos.CreateLeadNoteRequest request = new LeadNoteDtos.CreateLeadNoteRequest(
        "Test note content",
        false
    );
    LeadNote savedNote = new LeadNote(lead, request.content(), "test-user");
    savedNote.setId(10L);

    when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
    when(leadNoteRepository.save(any(LeadNote.class))).thenReturn(savedNote);

    // Act
    LeadNoteDtos.LeadNoteResponse result = leadNoteService.createNote(leadId, request, "test-user");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.leadId()).isEqualTo(leadId);
    assertThat(result.content()).isEqualTo("Test note content");
    assertThat(result.isPinned()).isFalse();
    verify(leadRepository).findById(leadId);
    verify(leadNoteRepository).save(any(LeadNote.class));
    verify(activityLogService).logNoteAdded(any(Lead.class), any(LeadNote.class));
  }

  @Test
  @DisplayName("Should create pinned note successfully")
  void shouldCreatePinnedNote() {
    // Arrange
    Long leadId = 1L;
    Lead lead = createTestLead(leadId);
    LeadNoteDtos.CreateLeadNoteRequest request = new LeadNoteDtos.CreateLeadNoteRequest(
        "Important pinned note",
        true
    );
    LeadNote savedNote = new LeadNote(lead, request.content(), "test-user");
    savedNote.setId(10L);
    savedNote.setIsPinned(true);

    when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
    when(leadNoteRepository.save(any(LeadNote.class))).thenReturn(savedNote);

    // Act
    LeadNoteDtos.LeadNoteResponse result = leadNoteService.createNote(leadId, request, "test-user");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.isPinned()).isTrue();
    verify(leadRepository).findById(leadId);
    verify(leadNoteRepository).save(any(LeadNote.class));
    verify(activityLogService).logNoteAdded(any(Lead.class), any(LeadNote.class));
  }

  @Test
  @DisplayName("Should throw exception when creating note for non-existent lead")
  void shouldThrowExceptionWhenCreatingNoteForNonExistentLead() {
    // Arrange
    Long leadId = 999L;
    LeadNoteDtos.CreateLeadNoteRequest request = new LeadNoteDtos.CreateLeadNoteRequest(
        "Test note content",
        false
    );

    when(leadRepository.findById(leadId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> leadNoteService.createNote(leadId, request, "test-user"))
        .isInstanceOf(LeadNotFoundException.class)
        .hasMessageContaining("not found");

    verify(leadRepository).findById(leadId);
    verify(leadNoteRepository, never()).save(any(LeadNote.class));
    verify(activityLogService, never()).logNoteAdded(any(Lead.class), any(LeadNote.class));
  }

  @Test
  @DisplayName("Should get notes by lead ID successfully")
  void shouldGetNotesByLeadId() {
    // Arrange
    Long leadId = 1L;
    Lead lead = createTestLead(leadId);
    LeadNote note1 = new LeadNote(lead, "First note", "user1");
    note1.setId(10L);
    LeadNote note2 = new LeadNote(lead, "Second note", "user2");
    note2.setId(11L);
    List<LeadNote> notes = List.of(note2, note1); // Descending order

    when(leadRepository.existsById(leadId)).thenReturn(true);
    when(leadNoteRepository.findByLeadIdOrderByCreatedAtDesc(leadId)).thenReturn(notes);

    // Act
    List<LeadNoteDtos.LeadNoteResponse> result = leadNoteService.getNotesByLeadId(leadId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    assertThat(result.get(0).content()).isEqualTo("Second note");
    assertThat(result.get(1).content()).isEqualTo("First note");
    verify(leadRepository).existsById(leadId);
    verify(leadNoteRepository).findByLeadIdOrderByCreatedAtDesc(leadId);
  }

  @Test
  @DisplayName("Should return empty list when lead has no notes")
  void shouldReturnEmptyListWhenLeadHasNoNotes() {
    // Arrange
    Long leadId = 1L;
    when(leadRepository.existsById(leadId)).thenReturn(true);
    when(leadNoteRepository.findByLeadIdOrderByCreatedAtDesc(leadId)).thenReturn(List.of());

    // Act
    List<LeadNoteDtos.LeadNoteResponse> result = leadNoteService.getNotesByLeadId(leadId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
    verify(leadRepository).existsById(leadId);
    verify(leadNoteRepository).findByLeadIdOrderByCreatedAtDesc(leadId);
  }

  @Test
  @DisplayName("Should throw exception when getting notes for non-existent lead")
  void shouldThrowExceptionWhenGettingNotesForNonExistentLead() {
    // Arrange
    Long leadId = 999L;
    when(leadRepository.existsById(leadId)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> leadNoteService.getNotesByLeadId(leadId))
        .isInstanceOf(LeadNotFoundException.class)
        .hasMessageContaining("not found");

    verify(leadRepository).existsById(leadId);
    verify(leadNoteRepository, never()).findByLeadIdOrderByCreatedAtDesc(any());
  }

  @Test
  @DisplayName("Should update note successfully")
  void shouldUpdateNote() {
    // Arrange
    Long leadId = 1L;
    Long noteId = 10L;
    Lead lead = createTestLead(leadId);
    LeadNote existingNote = new LeadNote(lead, "Original content", "user1");
    existingNote.setId(noteId);

    LeadNoteDtos.UpdateLeadNoteRequest request = new LeadNoteDtos.UpdateLeadNoteRequest(
        "Updated content",
        true
    );

    when(leadRepository.existsById(leadId)).thenReturn(true);
    when(leadNoteRepository.findById(noteId)).thenReturn(Optional.of(existingNote));
    when(leadNoteRepository.save(any(LeadNote.class))).thenReturn(existingNote);

    // Act
    LeadNoteDtos.LeadNoteResponse result = leadNoteService.updateNote(leadId, noteId, request, "admin");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.content()).isEqualTo("Updated content");
    assertThat(result.isPinned()).isTrue();
    verify(leadRepository).existsById(leadId);
    verify(leadNoteRepository).findById(noteId);
    verify(leadNoteRepository).save(existingNote);
  }

  @Test
  @DisplayName("Should update note content only when isPinned is null")
  void shouldUpdateNoteContentOnlyWhenIsPinnedIsNull() {
    // Arrange
    Long leadId = 1L;
    Long noteId = 10L;
    Lead lead = createTestLead(leadId);
    LeadNote existingNote = new LeadNote(lead, "Original content", "user1");
    existingNote.setId(noteId);
    existingNote.setIsPinned(true);

    LeadNoteDtos.UpdateLeadNoteRequest request = new LeadNoteDtos.UpdateLeadNoteRequest(
        "Updated content",
        null
    );

    when(leadRepository.existsById(leadId)).thenReturn(true);
    when(leadNoteRepository.findById(noteId)).thenReturn(Optional.of(existingNote));
    when(leadNoteRepository.save(any(LeadNote.class))).thenReturn(existingNote);

    // Act
    LeadNoteDtos.LeadNoteResponse result = leadNoteService.updateNote(leadId, noteId, request, "admin");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.content()).isEqualTo("Updated content");
    assertThat(result.isPinned()).isTrue(); // Should remain unchanged
    verify(leadNoteRepository).save(existingNote);
  }

  @Test
  @DisplayName("Should throw exception when updating note for non-existent lead")
  void shouldThrowExceptionWhenUpdatingNoteForNonExistentLead() {
    // Arrange
    Long leadId = 999L;
    Long noteId = 10L;
    LeadNoteDtos.UpdateLeadNoteRequest request = new LeadNoteDtos.UpdateLeadNoteRequest(
        "Updated content",
        false
    );

    when(leadRepository.existsById(leadId)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> leadNoteService.updateNote(leadId, noteId, request, "admin"))
        .isInstanceOf(LeadNotFoundException.class)
        .hasMessageContaining("not found");

    verify(leadRepository).existsById(leadId);
    verify(leadNoteRepository, never()).findById(any());
  }

  @Test
  @DisplayName("Should throw exception when updating non-existent note")
  void shouldThrowExceptionWhenUpdatingNonExistentNote() {
    // Arrange
    Long leadId = 1L;
    Long noteId = 999L;
    LeadNoteDtos.UpdateLeadNoteRequest request = new LeadNoteDtos.UpdateLeadNoteRequest(
        "Updated content",
        false
    );

    when(leadRepository.existsById(leadId)).thenReturn(true);
    when(leadNoteRepository.findById(noteId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> leadNoteService.updateNote(leadId, noteId, request, "admin"))
        .isInstanceOf(LeadNoteNotFoundException.class)
        .hasMessageContaining("not found");

    verify(leadRepository).existsById(leadId);
    verify(leadNoteRepository).findById(noteId);
    verify(leadNoteRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw exception when updating note with wrong lead ID")
  void shouldThrowExceptionWhenUpdatingNoteWithWrongLeadId() {
    // Arrange
    Long leadId = 1L;
    Long wrongLeadId = 2L;
    Long noteId = 10L;
    Lead lead = createTestLead(leadId);
    LeadNote note = new LeadNote(lead, "Original content", "user1");
    note.setId(noteId);

    LeadNoteDtos.UpdateLeadNoteRequest request = new LeadNoteDtos.UpdateLeadNoteRequest(
        "Updated content",
        false
    );

    when(leadRepository.existsById(wrongLeadId)).thenReturn(true);
    when(leadNoteRepository.findById(noteId)).thenReturn(Optional.of(note));

    // Act & Assert
    assertThatThrownBy(() -> leadNoteService.updateNote(wrongLeadId, noteId, request, "admin"))
        .isInstanceOf(LeadNoteNotFoundException.class)
        .hasMessageContaining("does not belong to lead");

    verify(leadRepository).existsById(wrongLeadId);
    verify(leadNoteRepository).findById(noteId);
    verify(leadNoteRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should delete note successfully")
  void shouldDeleteNote() {
    // Arrange
    Long leadId = 1L;
    Long noteId = 10L;
    Lead lead = createTestLead(leadId);
    LeadNote note = new LeadNote(lead, "Note to delete", "user1");
    note.setId(noteId);

    when(leadRepository.existsById(leadId)).thenReturn(true);
    when(leadNoteRepository.findById(noteId)).thenReturn(Optional.of(note));

    // Act
    leadNoteService.deleteNote(leadId, noteId);

    // Assert
    verify(leadRepository).existsById(leadId);
    verify(leadNoteRepository).findById(noteId);
    verify(leadNoteRepository).delete(note);
  }

  @Test
  @DisplayName("Should throw exception when deleting note for non-existent lead")
  void shouldThrowExceptionWhenDeletingNoteForNonExistentLead() {
    // Arrange
    Long leadId = 999L;
    Long noteId = 10L;

    when(leadRepository.existsById(leadId)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> leadNoteService.deleteNote(leadId, noteId))
        .isInstanceOf(LeadNotFoundException.class)
        .hasMessageContaining("not found");

    verify(leadRepository).existsById(leadId);
    verify(leadNoteRepository, never()).findById(any());
    verify(leadNoteRepository, never()).delete(any());
  }

  @Test
  @DisplayName("Should throw exception when deleting non-existent note")
  void shouldThrowExceptionWhenDeletingNonExistentNote() {
    // Arrange
    Long leadId = 1L;
    Long noteId = 999L;

    when(leadRepository.existsById(leadId)).thenReturn(true);
    when(leadNoteRepository.findById(noteId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> leadNoteService.deleteNote(leadId, noteId))
        .isInstanceOf(LeadNoteNotFoundException.class)
        .hasMessageContaining("not found");

    verify(leadRepository).existsById(leadId);
    verify(leadNoteRepository).findById(noteId);
    verify(leadNoteRepository, never()).delete(any());
  }

  @Test
  @DisplayName("Should throw exception when deleting note with wrong lead ID")
  void shouldThrowExceptionWhenDeletingNoteWithWrongLeadId() {
    // Arrange
    Long leadId = 1L;
    Long wrongLeadId = 2L;
    Long noteId = 10L;
    Lead lead = createTestLead(leadId);
    LeadNote note = new LeadNote(lead, "Note to delete", "user1");
    note.setId(noteId);

    when(leadRepository.existsById(wrongLeadId)).thenReturn(true);
    when(leadNoteRepository.findById(noteId)).thenReturn(Optional.of(note));

    // Act & Assert
    assertThatThrownBy(() -> leadNoteService.deleteNote(wrongLeadId, noteId))
        .isInstanceOf(LeadNoteNotFoundException.class)
        .hasMessageContaining("does not belong to lead");

    verify(leadRepository).existsById(wrongLeadId);
    verify(leadNoteRepository).findById(noteId);
    verify(leadNoteRepository, never()).delete(any());
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
