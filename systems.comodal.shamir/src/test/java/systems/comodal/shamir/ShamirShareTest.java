package systems.comodal.shamir;

import org.apache.commons.numbers.combinatorics.BinomialCoefficient;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

final class ShamirShareTest {

  @Test
  void testShareCreationAndReconstruction() {
    final var sharesBuilder = Shamir.buildShares().mersennePrimeExponent(521);

    sharesBuilder.validatePrime();
    assertNull(sharesBuilder.getSecret());
    validateToString(sharesBuilder);

    int numShares = 64;
    final var sharePositions = IntStream.rangeClosed(1, numShares).mapToObj(BigInteger::valueOf).toArray(BigInteger[]::new);
    final var coordinates = new HashMap<BigInteger, BigInteger>(numShares);

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
            .forEach(i -> coordinates.put(sharePositions[i], shares[i]));
        final var reconstructedSecret = Shamir.reconstructSecret(coordinates, sharesBuilder.getPrime());
        assertEquals(sharesBuilder.getSecret(), reconstructedSecret, sharesBuilder::toString);
        coordinates.clear();

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
    validateToString(sharesBuilder);

    final var shares = sharesBuilder.createShares();
    assertEquals(binomialCoefficient(sharesBuilder), sharesBuilder.validateShareCombinations(shares));

    final var swap = shares[0];
    shares[0] = shares[1];
    shares[1] = swap;
    assertThrows(IllegalStateException.class, () -> sharesBuilder.validateShareCombinations(shares));

    assertNull(sharesBuilder.clearSecret(0).getSecret());
    validateToString(sharesBuilder);
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
    validateToString(sharesBuilder);

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
    validateToString(sharesBuilder);

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
    validateToString(sharesBuilder);
  }

  @Test
  void testDefaultState() {
    final var sharesBuilder = Shamir.buildShares();
    assertNull(sharesBuilder.getSecret());
    assertNull(sharesBuilder.getPrime());
    assertEquals(0, sharesBuilder.getNumRequiredShares());
    assertEquals(0, sharesBuilder.getNumShares());
    validateToString(sharesBuilder);
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
    final int numShares = 5;
    final var secrets = Shamir.createSecrets(secureRandom, prime, numRequired);
    assertEquals(numRequired, secrets.length);
    for (final var secret : secrets) {
      assertNotNull(secret);
      assertTrue(secret.compareTo(prime) < 0, secret::toString);
      assertTrue(secret.compareTo(BigInteger.ZERO) > 0, secret::toString);
    }

    var shares = Shamir.createShares(prime, secrets, numShares);
    final long binomialCoefficient = BinomialCoefficient.value(5, 3);
    assertEquals(binomialCoefficient, Shamir.validateShareCombinations(secrets[0], prime, secrets.length, shares));

    shares = Shamir.createShares(secureRandom, prime, secrets[0], numRequired, numShares);
    assertEquals(binomialCoefficient, Shamir.validateShareCombinations(secrets[0], prime, secrets.length, shares));
  }

  @Test
  void testSuppliedSecret() {
    final var secretString = "Shamir's Secret";
    final var secretBytes = secretString.getBytes(UTF_8);
    final var secret = new BigInteger(secretBytes);

    final var sharesBuilder = Shamir.buildShares()
        .mersennePrimeExponent(1_279)
        .numRequiredShares(4)
        .numShares(8)
        .initSecrets(secret);

    sharesBuilder.validatePrime();

    final var coordinates = new HashMap<BigInteger, BigInteger>(sharesBuilder.getNumRequiredShares());
    validateShares(sharesBuilder, coordinates, secret, secretBytes, secretString);

    sharesBuilder.initSecrets(secretBytes);
    coordinates.clear();
    validateShares(sharesBuilder, coordinates, secret, secretBytes, secretString);

    sharesBuilder.prime(null);
    assertThrows(NullPointerException.class, () -> sharesBuilder.initSecrets(secretBytes), "Should have failed with a null prime.");
    assertThrows(NullPointerException.class, sharesBuilder::initSecrets, "Should have failed with a null prime.");
  }

  private static long binomialCoefficient(final ShamirSharesBuilder sharesBuilder) {
    return BinomialCoefficient.value(sharesBuilder.getNumShares(), sharesBuilder.getNumRequiredShares());
  }

  private void validateShares(final ShamirSharesBuilder sharesBuilder,
                              final Map<BigInteger, BigInteger> coordinates,
                              final BigInteger expectedSecret,
                              final byte[] expectedSecretBytes,
                              final String expectedSecretString) {
    final var shares = sharesBuilder.createShares();
    assertEquals(binomialCoefficient(sharesBuilder), sharesBuilder.validateShareCombinations(shares));

    IntStream.range(0, sharesBuilder.getNumRequiredShares())
        .forEach(i -> coordinates.put(BigInteger.valueOf(i + 1), shares[i]));
    final var reconstructedSecret = Shamir.reconstructSecret(coordinates, sharesBuilder.getPrime());

    assertEquals(expectedSecret, reconstructedSecret);
    final var reconstructSecretBytes = reconstructedSecret.toByteArray();
    assertArrayEquals(expectedSecretBytes, reconstructSecretBytes);
    assertEquals(expectedSecretString, new String(reconstructSecretBytes, UTF_8));
  }

  @Test
  void testCreateSecretBoundsCheck() {
    final var secureRandom = new SecureRandom();
    final var smallestPrime = BigInteger.TWO;
    IntStream.range(0, 100).forEach(i -> Shamir.createSecret(secureRandom, smallestPrime));
  }

  @Test
  void testReadmeExample() {
    var sharesBuilder = Shamir.buildShares()
        .numRequiredShares(3)
        .numShares(5)
        .mersennePrimeExponent(521)
        .initSecrets("Shamir's Secret".getBytes(UTF_8));

    var shares = sharesBuilder.createShares();

    sharesBuilder.validateShareCombinations(shares);

    var coordinates = Map.of(BigInteger.valueOf(1), shares[0],
        BigInteger.valueOf(3), shares[2],
        BigInteger.valueOf(5), shares[4]);
    var secret = Shamir.reconstructSecret(coordinates, sharesBuilder.getPrime());
    var secretString = new String(secret.toByteArray(), UTF_8);
    assertEquals("Shamir's Secret", secretString);
  }

  private void validateToString(final Object object) {
    assertDoesNotThrow(object::toString, () -> object.getClass().getSimpleName() + "#toString failed");
  }
}
