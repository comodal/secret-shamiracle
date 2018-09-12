package systems.comodal.shamir;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Map;

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
}
