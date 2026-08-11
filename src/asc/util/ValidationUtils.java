package asc.util;

import java.util.regex.Pattern;

/**
 * Utility class for centralized data validation using regular expressions.
 */
public class ValidationUtils {

  private static final Pattern EMAIL_PATTERN = 
      Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
  
  private static final Pattern PHONE_PATTERN = 
      Pattern.compile("^\\d{10,11}$");
  
  private static final Pattern DATE_PATTERN = 
      Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

  /**
   * Validates if the given email is in a correct format.
   */
  public static boolean isValidEmail(String email) {
    return email != null && EMAIL_PATTERN.matcher(email).matches();
  }

  /**
   * Validates if the given phone number is in a correct format (10-11 digits).
   */
  public static boolean isValidPhone(String phone) {
    return phone != null && PHONE_PATTERN.matcher(phone).matches();
  }

  /**
   * Validates if the given date is in yyyy-MM-dd format.
   */
  public static boolean isValidDate(String date) {
    if (date == null || !DATE_PATTERN.matcher(date).matches()) {
      return false;
    }
    try {
      java.time.LocalDate.parse(date);
      return true;
    } catch (java.time.format.DateTimeParseException e) {
      return false;
    }
  }

  /**
   * Validates if the given string is a positive double.
   */
  public static boolean isPositiveDouble(String value) {
    try {
      return Double.parseDouble(value) > 0;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
