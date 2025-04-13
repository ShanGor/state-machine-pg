package io.github.shangor.statemachine.pojo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UseCaseEventKeyTest {

    @Test
    public void equals_Should_ReturnTrue_When_ObjectsAreEqual() {
        // Given
        UseCaseEventKey key1 = new UseCaseEventKey("1", "A");
        UseCaseEventKey key2 = new UseCaseEventKey("1", "A");

        // When
        boolean result = key1.equals(key2);

        // Then
        Assertions.assertTrue(result);
    }

    @Test
    public void equals_Should_ReturnFalse_When_ObjectsAreNotEqual() {
        // Given
        UseCaseEventKey key1 = new UseCaseEventKey("1", "A");
        UseCaseEventKey key2 = new UseCaseEventKey("2", "B");

        // When
        boolean result = key1.equals(key2);

        // Then
        Assertions.assertFalse(result);
    }

    @Test
    public void equals_Should_ReturnFalse_When_ObjectIsNull() {
        // Given
        UseCaseEventKey key1 = new UseCaseEventKey("1", "A");
        UseCaseEventKey key2 = null;

        // When
        boolean result = key1.equals(key2);

        // Then
        Assertions.assertFalse(result);
    }

    @Test
    public void equals_Should_ReturnFalse_When_ObjectIsOfDifferentType() {
        // Given
        UseCaseEventKey key1 = new UseCaseEventKey("1", "A");
        Object key2 = new Object();

        // When
        boolean result = key1.equals(key2);

        // Then
        Assertions.assertFalse(result);
    }

    @Test
    public void equals_Should_ReturnFalse_When_FromStateIsDifferent() {
        // Given
        UseCaseEventKey key1 = new UseCaseEventKey("1", "A");
        UseCaseEventKey key2 = new UseCaseEventKey("1", "B");

        // When
        boolean result = key1.equals(key2);

        // Then
        Assertions.assertFalse(result);
    }
}