package com.dragomitch.ipl.pae.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import com.dragomitch.ipl.pae.utils.DataValidationUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

public class TestDataValidationUtils {

  @Test
  public void testIsAValidStringTC1() {
    String str = null;
    assertFalse(DataValidationUtils.isAValidString(str));
  }

  @Test
  public void testIsAValidStringTC2() {
    String str = "";
    assertFalse(DataValidationUtils.isAValidString(str));
  }

  @Test
  public void testIsAValidStringTC3() {
    String str = "hello";
    assertTrue(DataValidationUtils.isAValidString(str));
  }

  @Test
  public void testIsShortherOrEqualsThanTC1() {
    String str = "hello";
    int maximalLength = 1;
    assertFalse(DataValidationUtils.isShortherOrEqualsThan(str, maximalLength));
  }

  @Test
  public void testIsShortherOrEqualsThanTC2() {
    String str = "hello";
    int maximalLength = 10;
    assertTrue(DataValidationUtils.isShortherOrEqualsThan(str, maximalLength));
  }

  @Test
  public void testIsShortherOrEqualsThanTC3() {
    String str = "hello";
    int maximalLength = 5;
    assertTrue(DataValidationUtils.isShortherOrEqualsThan(str, maximalLength));
  }

  @Test
  public void testIsShortherOrEqualsThanTC4() {
    String str = null;
    int maximalLength = 10;
    assertFalse(DataValidationUtils.isShortherOrEqualsThan(str, maximalLength));
  }

  @Test
  public void testIsShortherOrEqualsThanTC5() {
    String str = "hello";
    int maximalLength = -5;
    assertFalse(DataValidationUtils.isShortherOrEqualsThan(str, maximalLength));
  }

  @Test
  public void testIsAValidIbanTC1() {
    String iban = "";
    assertFalse(DataValidationUtils.isAValidIban(iban));
  }

  @Test
  public void testIsAValidIbanTC2() {
    String iban = null;
    assertFalse(DataValidationUtils.isAValidIban(iban));
  }

  @Test
  public void testIsAValidIbanTC3() {
    String iban = "hello";
    assertFalse(DataValidationUtils.isAValidIban(iban));
  }

  @Test
  public void testIsAValidIbanTC4() {
    String iban = "BE96001244289402";
    assertTrue(DataValidationUtils.isAValidIban(iban));
  }

  @Test
  public void testIsAValidIbanTC5() {
    String iban = "BE9?001244289402";
    assertFalse(DataValidationUtils.isAValidIban(iban));
  }

  @Test
  public void testIsAValidBicTC1() {
    String bic = "";
    assertFalse(DataValidationUtils.isAValidBic(bic));
  }

  @Test
  public void testIsAValidBicTC2() {
    String bic = null;
    assertFalse(DataValidationUtils.isAValidBic(bic));
  }

  @Test
  public void testIsAValidBicTC3() {
    String bic = "GEBABEBB";
    assertTrue(DataValidationUtils.isAValidBic(bic));
  }

  @Test
  public void testIsAValidBicTC4() {
    String bic = "123hello";
    assertFalse(DataValidationUtils.isAValidBic(bic));
  }

  @Test
  public void testIsAValidEmailTC1() {
    String email = "";
    assertFalse(DataValidationUtils.isAValidEmail(email));
  }

  @Test
  public void testIsAValidEmailTC2() {
    String email = null;
    assertFalse(DataValidationUtils.isAValidEmail(email));
  }

  @Test
  public void testIsAValidEmailTC3() {
    String email = "hello";
    assertFalse(DataValidationUtils.isAValidEmail(email));
  }

  @Test
  public void testIsAValidEmailTC4() {
    String email = "hello_21@gmail.com";
    assertTrue(DataValidationUtils.isAValidEmail(email));
  }

  @Test
  public void testIsAValidPhoneNumberTC1() {
    String phoneNumber = null;
    assertFalse(DataValidationUtils.isAValidPhoneNumber(phoneNumber));
  }

  @Test
  public void testIsAValidPhoneNumberTC2() {
    String phoneNumber = "";
    assertFalse(DataValidationUtils.isAValidPhoneNumber(phoneNumber));
  }

  @Test
  public void testIsAValidPhoneNumberTC3() {
    String phoneNumber = "0472/12.34.56";
    assertTrue(DataValidationUtils.isAValidPhoneNumber(phoneNumber));
  }

  @Test
  public void testIsAValidPhoneNumberTC4() {
    String phoneNumber = "0472 12 34 56";
    assertFalse(DataValidationUtils.isAValidPhoneNumber(phoneNumber));
  }

  @Test
  public void testIsAValidObjectTC1() {
    Object object = null;
    assertFalse(DataValidationUtils.isAValidObject(object));
  }

  @Test
  public void testIsAValidObjectTC2() {
    String object = "";
    assertTrue(DataValidationUtils.isAValidObject(object));
  }

  @Test
  public void testIsPositiveTC1() {
    int number = -1;
    assertFalse(DataValidationUtils.isPositive(number));
  }

  @Test
  public void testIsPositiveTC2() {
    int number = 0;
    assertFalse(DataValidationUtils.isPositive(number));
  }

  @Test
  public void testIsPositiveTC3() {
    int number = 1;
    assertTrue(DataValidationUtils.isPositive(number));
  }

  @Test
  public void testIsPositiveOrZeroTC1() {
    int number = -1;
    assertFalse(DataValidationUtils.isPositiveOrZero(number));
  }

  @Test
  public void testIsPositiveOrZeroTC2() {
    int number = 0;
    assertTrue(DataValidationUtils.isPositiveOrZero(number));
  }

  @Test
  public void testIsPositiveOrZeroTC3() {
    int number = 1;
    assertTrue(DataValidationUtils.isPositiveOrZero(number));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCheckStringTC1() {
    String str = null;
    DataValidationUtils.checkString(str);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCheckStringTC2() {
    String str = "";
    DataValidationUtils.checkString(str);
  }

  @Test
  public void testCheckStringTC3() {
    String str = "hello";
    DataValidationUtils.checkString(str);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCheckObjectTC1() {
    Object object = null;
    DataValidationUtils.checkObject(object);
  }

  @Test
  public void testCheckObjectTC2() {
    String object = "";
    DataValidationUtils.checkObject(object);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCheckPositiveTC1() {
    int number = -1;
    DataValidationUtils.checkPositive(number);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCheckPositiveTC2() {
    int number = 0;
    DataValidationUtils.checkPositive(number);
  }

  @Test
  public void testCheckPositiveTC3() {
    int number = 1;
    DataValidationUtils.checkPositive(number);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCheckPositiveOrZeroTC1() {
    int number = -1;
    DataValidationUtils.checkPositiveOrZero(number);
  }

  @Test
  public void testCheckPositiveOrZeroTC2() {
    int number = 0;
    DataValidationUtils.checkPositiveOrZero(number);
  }

  @Test
  public void testCheckPositiveOrZeroTC3() {
    int number = 1;
    DataValidationUtils.checkPositiveOrZero(number);
  }

  @Test
  public void testInstantiationTC1() {
    Constructor<?>[] constructors = DataValidationUtils.class.getDeclaredConstructors();
    for (Constructor<?> constructor : constructors) {
      assertTrue(Modifier.isPrivate(constructor.getModifiers()));
    }
  }

  @Test(expected = InvocationTargetException.class)
  public void testInstantiationTC2() throws NoSuchMethodException, IllegalAccessException,
      InstantiationException, InvocationTargetException {
    Constructor<?> constructor = DataValidationUtils.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

}
