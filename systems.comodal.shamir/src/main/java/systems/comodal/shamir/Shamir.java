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

  public static BigInteger[] createShares(final BigInteger prime, final BigInteger[] secrets, final int numShares) {
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

  public static BigInteger reconstructSecret(final Map<BigInteger, BigInteger> shares, final BigInteger prime) {
    final var shareEntries = shares.entrySet();
    var freeCoefficient = BigInteger.ZERO;

    for (final var referenceEntry : shareEntries) {
      var numerator = BigInteger.ONE;
      var denominator = BigInteger.ONE;

      final var referencePosition = referenceEntry.getKey();
      for (final var shareEntry : shareEntries) {
        final var position = shareEntry.getKey();
        if (referencePosition.equals(position)) {
          continue;
        }
        numerator = numerator.multiply(position.negate()).mod(prime);
        denominator = denominator.multiply(referencePosition.subtract(position)).mod(prime);
      }
      final var share = referenceEntry.getValue();
      freeCoefficient = prime.add(freeCoefficient)
          .add(share.multiply(numerator).multiply(denominator.modInverse(prime)))
          .mod(prime);
    }
    return freeCoefficient;
  }

  private static BigInteger reconstructSecret(final Map.Entry<BigInteger, BigInteger>[] shares, final BigInteger prime) {
    var freeCoefficient = BigInteger.ZERO;

    for (final var referenceEntry : shares) {
      var numerator = BigInteger.ONE;
      var denominator = BigInteger.ONE;

      final var referencePosition = referenceEntry.getKey();
      for (final var shareEntry : shares) {
        final var position = shareEntry.getKey();
        if (referencePosition.equals(position)) {
          continue;
        }
        numerator = numerator.multiply(position.negate()).mod(prime);
        denominator = denominator.multiply(referencePosition.subtract(position)).mod(prime);
      }
      final var share = referenceEntry.getValue();
      freeCoefficient = prime.add(freeCoefficient)
          .add(share.multiply(numerator).multiply(denominator.modInverse(prime)))
          .mod(prime);
    }
    return freeCoefficient;
  }

  @SuppressWarnings("unchecked")
  public static int validateShareCombinations(final BigInteger expectedSecret,
                                              final BigInteger prime,
                                              final int numRequiredShares,
                                              final BigInteger[] shares) {
    final var points = IntStream.range(0, shares.length)
        .mapToObj(i -> Map.entry(BigInteger.valueOf(i + 1), shares[i]))
        .toArray(Map.Entry[]::new);
    return Shamir.shareCombinations(points, 0, numRequiredShares, new Map.Entry[numRequiredShares], expectedSecret, prime);
  }

  private static int shareCombinations(final Map.Entry<BigInteger, BigInteger>[] points,
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
    for (int i = startPos; i <= points.length - len; i++) {
      result[result.length - len] = points[i];
      numSubSets += shareCombinations(points, i + 1, len - 1, result, expectedSecret, prime);
    }
    return numSubSets;
  }

  private static void validateReconstruction(final BigInteger expectedSecret,
                                             final BigInteger prime,
                                             final Map.Entry<BigInteger, BigInteger>[] shares) {
    final var reconstructedSecret = reconstructSecret(shares, prime);
    if (!expectedSecret.equals(reconstructedSecret)) {
      throw new IllegalStateException(String.format("Reconstructed secret does not equal expected secret. %nReconstructed: '%s' %nExpected: '%s' %nWith %d shares: %n%s",
          reconstructedSecret, expectedSecret, shares.length, Arrays.toString(shares)));
    }
  }
}
