package systems.comodal.shamir;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Map;
import java.util.stream.IntStream;

public final class Shamir {

  private Shamir() {
  }

  public static ShamirSharesBuilder buildShares() {
    return new ShamirSharesBuilder();
  }

  public static BigInteger[] createSecrets(final SecureRandom secureRandom, final BigInteger prime, final int requiredShares) {
    final var secrets = new BigInteger[requiredShares];
    createSecrets(secureRandom, prime, secrets);
    return secrets;
  }

  public static void createSecrets(final SecureRandom secureRandom, final BigInteger prime, final BigInteger[] secrets) {
    for (int i = 0; i < secrets.length; i++) {
      secrets[i] = createSecret(secureRandom, prime);
    }
  }

  public static BigInteger createSecret(final SecureRandom secureRandom, final BigInteger prime) {
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
    var freeCoefficient = BigInteger.ZERO;

    for (final var referenceEntry : shares.entrySet()) {
      var numerator = BigInteger.ONE;
      var denominator = BigInteger.ONE;

      final var referencePosition = referenceEntry.getKey();
      for (final var shareEntry : shares.entrySet()) {
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
    final var positions = IntStream.rangeClosed(1, shares.length).mapToObj(BigInteger::valueOf).toArray(BigInteger[]::new);
    return Shamir.shareCombinations(shares, 0, numRequiredShares, new Map.Entry[numRequiredShares], expectedSecret, prime, positions);
  }

  private static int shareCombinations(final BigInteger[] shares,
                                       final int startPos,
                                       final int len,
                                       final Map.Entry<BigInteger, BigInteger>[] result,
                                       final BigInteger expectedSecret,
                                       final BigInteger prime,
                                       final BigInteger[] cachedPositions) {
    if (len == 0) {
      validateReconstruction(expectedSecret, prime, Map.ofEntries(result));
      return 1;
    }
    int numSubSets = 0;
    for (int i = startPos; i <= shares.length - len; i++) {
      final int r = result.length - len;
      result[r] = Map.entry(cachedPositions[i], shares[i]);
      numSubSets += shareCombinations(shares, i + 1, len - 1, result, expectedSecret, prime, cachedPositions);
    }
    return numSubSets;
  }

  private static void validateReconstruction(final BigInteger expectedSecret,
                                             final BigInteger prime,
                                             final Map<BigInteger, BigInteger> shareMap) {
    final var reconstructedSecret = reconstructSecret(shareMap, prime);
    if (!expectedSecret.equals(reconstructedSecret)) {
      throw new IllegalStateException(String.format("Reconstructed secret does not equal expected secret. %nReconstructed: '%s' %nExpected: '%s' %nWith %d shares: %n%s",
          reconstructedSecret, expectedSecret, shareMap.size(), shareMap));
    }
  }
}
