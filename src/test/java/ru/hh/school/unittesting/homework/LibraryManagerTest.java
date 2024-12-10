package ru.hh.school.unittesting.homework;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LibraryManagerTest {
  @Mock
  private NotificationService notificationService;

  @Mock
  private UserService userService;

  @InjectMocks
  private LibraryManager libraryManager;

  @BeforeEach
  void setUp() {
    libraryManager.addBook("Book1", 10);
    libraryManager.addBook("Book2", 1);
  }

  @ParameterizedTest
  @CsvSource({
      "Book1, 5, 15",
      "Book1, 0, 10",
      "Book2, 1, 2",
      "Book2, 0, 1"
  })
  void testAddExistingBookId(String bookId, int quantity, int expectedValue) {
    libraryManager.addBook(bookId, quantity);
    int totalQuantity = libraryManager.getAvailableCopies(bookId);
    assertEquals(expectedValue, totalQuantity);
  }

  @ParameterizedTest
  @CsvSource({
      "NewBook, 17, 17",
      "NewBook, 0, 0"
  })
  void testAddNewBookId(String bookId, int quantity, int expectedValue) {
    libraryManager.addBook(bookId, quantity);
    int totalQuantity = libraryManager.getAvailableCopies(bookId);
    assertEquals(expectedValue, totalQuantity);
  }

  @Test
  void testFailureWhenNotActiveUserTriesToBorrowBook() {
    when(userService.isUserActive("NotActiveUser")).thenReturn(false);
    boolean result = libraryManager.borrowBook("Book1", "NotActiveUser");
    int totalQuantity = libraryManager.getAvailableCopies("Book1");

    assertFalse(result);
    verify(userService, times(1)).isUserActive("NotActiveUser");
    verify(notificationService, times(1))
        .notifyUser("NotActiveUser", "Your account is not active.");
    assertEquals(10, totalQuantity);
  }

  @Test
  void testFailureToBorrowWhenNoAvailableBook() {
    when(userService.isUserActive("ActiveUser")).thenReturn(true);
    boolean result = libraryManager.borrowBook("NoBook", "ActiveUser");
    int totalQuantity = libraryManager.getAvailableCopies("NoBook");

    assertFalse(result);
    verify(userService, times(1)).isUserActive("ActiveUser");
    assertEquals(0, totalQuantity);
  }

  @Test
  void testSuccessAttemptToBorrowBook() {
    when(userService.isUserActive("ActiveUser")).thenReturn(true);
    boolean result = libraryManager.borrowBook("Book1", "ActiveUser");
    int totalRemainsOfBook = libraryManager.getAvailableCopies("Book1");

    assertTrue(result);
    verify(userService, times(1)).isUserActive("ActiveUser");
    verify(notificationService, times(1))
        .notifyUser("ActiveUser", "You have borrowed the book: Book1");
    assertEquals(9, totalRemainsOfBook);
  }

  @Test
  void testFailureToReturnBookIfNoBookInBorrowed() {
    boolean result = libraryManager.returnBook("Book0", "User1");

    assertFalse(result);
    assertEquals(0, libraryManager.getAvailableCopies("Book0"));
  }

  @Test
  void testFailureToReturnBookIfUserIdIncorrect() {
    String userId = "User1";
    when(userService.isUserActive(userId)).thenReturn(true);
    libraryManager.borrowBook("Book2", userId);
    boolean result = libraryManager.returnBook("Book2", "User100");

    assertFalse(result);
    assertEquals(0, libraryManager.getAvailableCopies("Book2"));
  }

  @Test
  void testSuccessAttemptToReturnBook() {
    String userId = "User1";
    when(userService.isUserActive(userId)).thenReturn(true);
    libraryManager.borrowBook("Book2", userId);
    boolean result = libraryManager.returnBook("Book2", "User1");
    int totalQuantity = libraryManager.getAvailableCopies("Book2");

    assertTrue(result);
    assertEquals(1, totalQuantity);
    verify(notificationService, times(1))
        .notifyUser("User1", "You have returned the book: Book2");
  }

  @Test
  void testThrowsExceptionWhenOverdueDaysIsNegative() {
    assertThrows(IllegalArgumentException.class,
        () -> libraryManager.calculateDynamicLateFee(-1, true, true));
  }

  @ParameterizedTest
  @CsvSource
      ({
          "10, false, false, 5",
          "7, false, false, 3.5",
          "0, false, false, 0",
          "10, true, false, 7.5",
          "7, true, false, 5.25",
          "10, false, true, 4",
          "10, true, true, 6",
      })
  void testSuccessCalculationOfDynamicLateFee(int overdueDays, boolean isBestseller, boolean isPremiumMember,
                                              double expectedFee) {
    double result = libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember);
    assertEquals(expectedFee, result);
  }
}
