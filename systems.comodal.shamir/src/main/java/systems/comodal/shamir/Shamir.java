package systems.comodal.shamir;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

public final class Shamir {

  private Shamir() {
  }

  public static ShamirSharesBuilder buildShares() {
    return new ShamirSharesBuilder();
  }

  public static BigInteger createMersennePrimeFromExponent(final int exponent) {
    // https://en.wikipedia.org/wiki/Mersenne_prime#List_of_known_Mersenne_primes
    // e.g. 13th Mersenne Prime has an exponent of 521.
    return BigInteger.ONE.shiftLeft(exponent).subtract(BigInteger.ONE);
  }

  public static BigInteger[] createSecrets(final Random secureRandom, final BigInteger prime, final int requiredShares) {
    final var secrets = new BigInteger[requiredShares];
    createSecrets(secureRandom, prime, secrets);
    return secrets;
  }

  public static void createSecrets(final Random secureRandom, final BigInteger prime, final BigInteger[] secrets) {
    for (int i = 0; i < secrets.length; i++) {
      secrets[i] = createSecret(secureRandom, prime);
    }
  }

  public static BigInteger createSecret(final Random secureRandom, final BigInteger prime) {
    for (BigInteger secret; ; ) {
      secret = new BigInteger(prime.bitLength(), secureRandom);
      if (secret.compareTo(BigInteger.ZERO) > 0 && secret.compareTo(prime) < 0) {
        return secret;
      }
    }
  }

  public static BigInteger[] createShares(final Random secureRandom,
                                          final BigInteger prime,
                                          final BigInteger secret,
                                          final int requiredShares,
                                          final int numShares) {
    final var secrets = new BigInteger[requiredShares];
    secrets[0] = secret;
    for (int i = 1; i < requiredShares; i++) {
      secrets[i] = createSecret(secureRandom, prime);
    }
    return createShares(prime, secrets, numShares);
  }

  public static BigInteger[] createShares(final BigInteger prime,
                                          final BigInteger[] secrets,
                                          final int numShares) {
    final var shares = new BigInteger[numShares];
    for (int shareIndex = 0; shareIndex < numShares; shareIndex++) {
      var result = secrets[0];
      final var sharePosition = BigInteger.valueOf(shareIndex + 1);
      for (int exp = 1; exp < secrets.length; exp++) {
        result = result.add(secrets[exp]
            .multiply(sharePosition.pow(exp).mod(prime)))
            .mod(prime);
      }
      shares[shareIndex] = result;
    }
    return shares;
  }

  public static BigInteger reconstructSecret(final Map<BigInteger, BigInteger> coordinates,
                                             final BigInteger prime) {
    return reconstructSecret(coordinates.entrySet(), prime);
  }

  public static BigInteger reconstructSecret(final Iterable<Map.Entry<BigInteger, BigInteger>> coordinateEntries,
                                             final BigInteger prime) {
    var freeCoefficient = BigInteger.ZERO;
    BigInteger referencePosition, position;
    BigInteger numerator, denominator;

    for (final var referencePoint : coordinateEntries) {
      numerator = denominator = BigInteger.ONE;
      referencePosition = referencePoint.getKey();
      for (final var point : coordinateEntries) {
        position = point.getKey();
        if (referencePosition.equals(position)) {
          continue;
        }
        numerator = numerator.multiply(position.negate()).mod(prime);
        denominator = denominator.multiply(referencePosition.subtract(position)).mod(prime);
      }
      freeCoefficient = prime.add(freeCoefficient)
          .add(referencePoint.getValue().multiply(numerator).multiply(denominator.modInverse(prime)))
          .mod(prime);
    }
    return freeCoefficient;
  }

  private static BigInteger reconstructSecret(final Map.Entry<BigInteger, BigInteger>[] coordinates,
                                              final BigInteger prime) {
    var freeCoefficient = BigInteger.ZERO;
    Map.Entry<BigInteger, BigInteger> referencePoint;
    BigInteger position;
    BigInteger numerator, denominator;

    final int numPoints = coordinates.length;
    for (int i = 0; i < numPoints; i++) {
      numerator = denominator = BigInteger.ONE;
      referencePoint = coordinates[i];
      for (int j = 0; j < numPoints; j++) {
        if (i == j) {
          continue;
        }
        position = coordinates[j].getKey();
        numerator = numerator.multiply(position.negate()).mod(prime);
        denominator = denominator.multiply(referencePoint.getKey().subtract(position)).mod(prime);
      }
      freeCoefficient = prime.add(freeCoefficient)
          .add(referencePoint.getValue().multiply(numerator).multiply(denominator.modInverse(prime)))
          .mod(prime);
    }
    return freeCoefficient;
  }

  @SuppressWarnings("unchecked")
  public static Map.Entry<BigInteger, BigInteger>[] createCoordinates(final BigInteger[] shares) {
    return IntStream.range(0, shares.length)
        .mapToObj(i -> Map.entry(BigInteger.valueOf(i + 1), shares[i]))
        .toArray(Map.Entry[]::new);
  }

  @SuppressWarnings("unchecked")
  public static int validateShareCombinations(final BigInteger expectedSecret,
                                              final BigInteger prime,
                                              final int numRequiredShares,
                                              final BigInteger[] shares) {
    final var coordinates = createCoordinates(shares);
    return Shamir.shareCombinations(coordinates, 0, numRequiredShares, new Map.Entry[numRequiredShares], expectedSecret, prime);
  }

  private static int shareCombinations(final Map.Entry<BigInteger, BigInteger>[] coordinates,
                                       final int startPos,
                                       final int len,
                                       final Map.Entry<BigInteger, BigInteger>[] result,
                                       final BigInteger expectedSecret,
                                       final BigInteger prime) {
    if (len == 0) {
      validateReconstruction(expectedSecret, prime, result);
      return 1;
    }
    int numSubSets = 0;
    for (int i = startPos; i <= coordinates.length - len; i++) {
      result[result.length - len] = coordinates[i];
      numSubSets += shareCombinations(coordinates, i + 1, len - 1, result, expectedSecret, prime);
    }
    return numSubSets;
  }

  private static void validateReconstruction(final BigInteger expectedSecret,
                                             final BigInteger prime,
                                             final Map.Entry<BigInteger, BigInteger>[] coordinates) {
    final var reconstructedSecret = reconstructSecret(coordinates, prime);
    if (!expectedSecret.equals(reconstructedSecret)) {
      throw new IllegalStateException(String.format("Reconstructed secret does not equal expected secret. %nReconstructed: '%s' %nExpected: '%s' %nWith %d shares: %n%s",
          reconstructedSecret, expectedSecret, coordinates.length, Arrays.toString(coordinates)));
    }
  }
}
