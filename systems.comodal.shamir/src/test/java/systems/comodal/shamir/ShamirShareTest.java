package systems.comodal.shamir;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

final class ShamirShareTest {

  @Test
  void testShareCreationAndReconstruction() {
    final var sharesBuilder = Shamir.buildShares().mersennePrimeExponent(521);

    sharesBuilder.validatePrime();
    assertNull(sharesBuilder.getSecret());
    assertDoesNotThrow(sharesBuilder::toString, "toString failed");

    int numShares = 64;
    final var sharePositions = IntStream.rangeClosed(1, numShares).mapToObj(BigInteger::valueOf).toArray(BigInteger[]::new);
    final var shareMap = new HashMap<BigInteger, BigInteger>(numShares);

    for (; numShares >= 63; numShares--) {
      sharesBuilder.numShares(numShares);
      assertEquals(numShares, sharesBuilder.getNumShares());

      for (int requiredShares = 1; requiredShares <= numShares; requiredShares++) {
        sharesBuilder
            .numRequiredShares(requiredShares)
            .initSecrets();
        assertEquals(requiredShares, sharesBuilder.getNumRequiredShares());
        assertNotNull(sharesBuilder.getSecret());

        final var shares = sharesBuilder.createShares();
        ThreadLocalRandom.current().ints(0, sharesBuilder.getNumShares())
            .distinct()
            .limit(sharesBuilder.getNumRequiredShares())
            .forEach(i -> shareMap.put(sharePositions[i], shares[i]));
        final var reconstructedSecret = Shamir.reconstructSecret(shareMap, sharesBuilder.getPrime());
        assertEquals(sharesBuilder.getSecret(), reconstructedSecret, sharesBuilder::toString);
        shareMap.clear();

        assertNull(sharesBuilder.clearSecrets().getSecret());
      }
    }
  }

  @Test
  void testShareCombinations() {
    final var prime = new BigInteger("2305843009213693951");
    final var sharesBuilder = Shamir.buildShares()
        .validateAndSetPrime(prime)
        .numRequiredShares(5)
        .numShares(10)
        .initSecrets();

    assertEquals(prime, sharesBuilder.getPrime());
    sharesBuilder.validatePrime();
    assertNotNull(sharesBuilder.getSecret());
    assertEquals(5, sharesBuilder.getNumRequiredShares());
    assertEquals(10, sharesBuilder.getNumShares());
    assertDoesNotThrow(sharesBuilder::toString, "toString failed");

    final var shares = sharesBuilder.createShares();
    // n!  / (r! * (n  - r)!)
    // 10! / (5! * (10 - 5)!)
    assertEquals(252, sharesBuilder.validateShareCombinations(shares));

    final var swap = shares[0];
    shares[0] = shares[1];
    shares[1] = swap;
    assertThrows(IllegalStateException.class, () -> sharesBuilder.validateShareCombinations(shares));

    assertNull(sharesBuilder.clearSecret(0).getSecret());
    assertDoesNotThrow(sharesBuilder::toString, "toString failed");
  }

  @Test
  void testInvalidSecret() {
    final var prime = BigInteger.valueOf(2147483647L);
    final var sharesBuilder = Shamir.buildShares()
        .prime(prime)
        .numRequiredShares(3)
        .numShares(7)
        .secureRandom(new SecureRandom());

    assertEquals(prime, sharesBuilder.getPrime());
    sharesBuilder.validatePrime();
    assertNull(sharesBuilder.getSecret());
    assertEquals(3, sharesBuilder.getNumRequiredShares());
    assertEquals(7, sharesBuilder.getNumShares());
    assertDoesNotThrow(sharesBuilder::toString, "toString failed");

    assertThrows(IllegalArgumentException.class, () -> sharesBuilder.initSecrets(sharesBuilder.getPrime()), "Should have failed with a secret equal to prime.");
    assertThrows(IllegalArgumentException.class, () -> sharesBuilder.initSecrets(sharesBuilder.getPrime().add(BigInteger.TWO)), "Should have failed with a secret > prime.");
    assertThrows(IllegalArgumentException.class, () -> sharesBuilder.initSecrets(BigInteger.ZERO), "Should have failed with a secret equal to 0.");
    assertThrows(IllegalArgumentException.class, () -> sharesBuilder.initSecrets(BigInteger.ONE.negate()), "Should have failed with a secret < 0.");
    assertDoesNotThrow(() -> sharesBuilder.initSecrets(sharesBuilder.getPrime().subtract(BigInteger.ONE)), "Should have succeeded with a secret equal to prime - 1.");
    assertDoesNotThrow(() -> sharesBuilder.initSecrets(BigInteger.ONE), "Should have succeeded with a secret equal to 1.");
  }

  @Test
  void testInvalidPrimes() {
    final var invalidPrime = BigInteger.valueOf(2147483647L - 1);
    final var sharesBuilder = Shamir.buildShares()
        .prime(invalidPrime)
        .numRequiredShares(2)
        .numShares(8)
        .secureRandom(new SecureRandom());

    assertEquals(invalidPrime, sharesBuilder.getPrime());
    assertNull(sharesBuilder.getSecret());
    assertEquals(2, sharesBuilder.getNumRequiredShares());
    assertEquals(8, sharesBuilder.getNumShares());
    assertDoesNotThrow(sharesBuilder::toString, "toString failed");

    assertThrows(IllegalStateException.class, sharesBuilder::validatePrime, () -> "Should have failed, supplied an invalid prime " + sharesBuilder.getPrime());
    assertThrows(IllegalStateException.class, () -> sharesBuilder.validateAndSetPrime(sharesBuilder.getPrime()), () -> "Should have failed, supplied an invalid prime " + sharesBuilder.getPrime());
    final var validPrime = sharesBuilder.getPrime().add(BigInteger.ONE);
    assertDoesNotThrow(() -> sharesBuilder.validateAndSetPrime(validPrime), () -> "Should have succeeded, supplied a valid prime " + validPrime);
  }

  @Test
  void testMersennePrimes() {
    final var sharesBuilder = Shamir.buildShares();
    final int[] exponents = new int[]{2, 3, 5, 7, 13, 17, 19, 31, 61, 89, 107, 127, 521, 607, 1_279, 2_203, 2_281, 3_217}; // 4_253, 4_423, 9_689, 9_941, 11_213, 19_937, 21_701, 23_209, 44_497, 86_243, 110_503, 132_049, 216_091, 756_839, 859_433, 1_257_787, 1_398_269, 2_976_221, 3_021_377, 6_972_593, 13_466_917, 20_996_011, 24_036_583, 25_964_951, 30_402_457, 32_582_657, 37_156_667, 42_643_801, 43_112_609, 57_885_161, 74_207_281, 77_232_917};
    for (int exponent : exponents) {
      sharesBuilder.mersennePrimeExponent(exponent).validatePrime();
    }
    assertDoesNotThrow(sharesBuilder::toString, "toString failed");
  }

  @Test
  void testDefaultState() {
    final var sharesBuilder = Shamir.buildShares();
    assertNull(sharesBuilder.getSecret());
    assertNull(sharesBuilder.getPrime());
    assertEquals(0, sharesBuilder.getNumRequiredShares());
    assertEquals(0, sharesBuilder.getNumShares());
    assertDoesNotThrow(sharesBuilder::toString, "toString failed");
  }

  @Test
  void testChangeNumRequiredShares() {
    final var sharesBuilder = Shamir.buildShares().mersennePrimeExponent(4_253);

    assertEquals(0, sharesBuilder.getNumRequiredShares());
    assertEquals(1, sharesBuilder.numRequiredShares(1).getNumRequiredShares());

    assertNull(sharesBuilder.getSecret());
    sharesBuilder.initSecrets();
    assertNotNull(sharesBuilder.getSecret());

    final var expectedSecret = sharesBuilder.getSecret();

    assertEquals(1, sharesBuilder.numRequiredShares(1).getNumRequiredShares());
    assertEquals(expectedSecret, sharesBuilder.getSecret());

    assertEquals(2, sharesBuilder.numRequiredShares(2).getNumRequiredShares());
    assertEquals(expectedSecret, sharesBuilder.getSecret());

    assertEquals(0, sharesBuilder.numRequiredShares(0).getNumRequiredShares());
    assertNull(sharesBuilder.getSecret());
  }

  @Test
  void testStaticShamirMethods() {
    final var secureRandom = new SecureRandom();

    final var prime = BigInteger.valueOf(73_939_133);
    assertTrue(prime.isProbablePrime(Integer.MAX_VALUE));

    final int numRequired = 3;
    final var secrets = Shamir.createSecrets(secureRandom, prime, numRequired);
    assertEquals(numRequired, secrets.length);
    for (final var secret : secrets) {
      assertNotNull(secret);
      assertTrue(secret.compareTo(prime) < 0, secret::toString);
      assertTrue(secret.compareTo(BigInteger.ZERO) > 0, secret::toString);
    }

    final var shares = Shamir.createShares(prime, secrets, 5);
    assertEquals(10, Shamir.validateShareCombinations(secrets[0], prime, secrets.length, shares));
  }
}
