package systems.comodal.shamir;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.IntStream;

import static systems.comodal.shamir.Shamir.createSecret;
import static systems.comodal.shamir.Shamir.reconstructSecret;

public final class ShamirSharesBuilder {

  private SecureRandom secureRandom;
  private BigInteger prime;
  private int numShares;
  private BigInteger[] secrets;

  ShamirSharesBuilder() {
  }

  public BigInteger getPrime() {
    return prime;
  }

  public int getNumShares() {
    return numShares;
  }

  public int getNumRequiredShares() {
    return secrets == null ? 0 : secrets.length;
  }

  public BigInteger getSecret() {
    return secrets == null ? null : secrets[0];
  }

  public ShamirSharesBuilder secureRandom(final SecureRandom secureRandom) {
    this.secureRandom = secureRandom;
    return this;
  }

  public ShamirSharesBuilder prime(final BigInteger prime) {
    this.prime = prime;
    return this;
  }

  public ShamirSharesBuilder mersennePrimeExponent(final int mersennePrimeExponent) {
    // https://en.wikipedia.org/wiki/Mersenne_prime#List_of_known_Mersenne_primes
    // e.g. 13th Mersenne Prime has an exponent of 521.
    this.prime = BigInteger.ONE.shiftLeft(mersennePrimeExponent).subtract(BigInteger.ONE);
    return this;
  }

  public ShamirSharesBuilder validatePrime() {
    validatePrime(prime);
    return this;
  }

  public ShamirSharesBuilder validateAndSetPrime(final BigInteger prime) {
    validatePrime(prime);
    this.prime = prime;
    return this;
  }

  private static void validatePrime(final BigInteger prime) {
    if (!prime.isProbablePrime(Integer.MAX_VALUE)) {
      throw new IllegalStateException("This is probably not a prime number using a certainty of Integer.MAX_VALUE: " + prime);
    }
  }

  public ShamirSharesBuilder numShares(final int numShares) {
    this.numShares = numShares;
    return this;
  }

  public ShamirSharesBuilder numRequiredShares(final int numRequiredShares) {
    if (secrets == null || secrets.length != numRequiredShares) {
      secrets = new BigInteger[numRequiredShares];
    }
    return this;
  }

  public ShamirSharesBuilder initSecrets(final BigInteger secret) {
    if (secret.compareTo(BigInteger.ZERO) <= 0 || secret.compareTo(prime) >= 0) {
      throw new IllegalArgumentException("Secret must be greater than 0 and less than the prime " + prime);
    }
    initSecureRandom();
    initSecretsUnchecked(secret);
    return this;
  }

  private void initSecureRandom() {
    if (secureRandom == null) {
      this.secureRandom = new SecureRandom();
    }
  }

  public ShamirSharesBuilder initSecrets() {
    initSecureRandom();
    initSecretsUnchecked(createSecret(secureRandom, prime));
    return this;
  }

  private void initSecretsUnchecked(final BigInteger secret) {
    secrets[0] = secret;
    for (int i = 1; i < secrets.length; i++) {
      secrets[i] = createSecret(secureRandom, prime);
    }
  }

  public ShamirSharesBuilder clearSecret(final int index) {
    secrets[index] = null;
    return this;
  }

  public ShamirSharesBuilder clearSecrets() {
    Arrays.fill(secrets, null);
    return this;
  }

  public BigInteger[] createShares() {
    return Shamir.createShares(prime, secrets, numShares);
  }

  private void validateReconstruction(final BigInteger expectedSecret,
                                      final Map<BigInteger, BigInteger> shareMap) {
    if (shareMap.size() != secrets.length) {
      throw new IllegalStateException(String.format("Share map should have exactly %d shares, but found %d.%n%s", secrets.length, shareMap.size(), shareMap));
    }
    final var reconstructedSecret = reconstructSecret(shareMap, prime);
    if (!expectedSecret.equals(reconstructedSecret)) {
      throw new IllegalStateException(String.format("Reconstructed secret does not equal expected secret. %nReconstructed: '%s' %nExpected: '%s' %nWith %d shares: %n%s",
          reconstructedSecret, expectedSecret, shareMap.size(), shareMap));
    }
  }

  @SuppressWarnings("unchecked")
  public int validateShareCombinations(final BigInteger[] shares) {
    final var positions = IntStream.rangeClosed(1, numShares).mapToObj(BigInteger::valueOf).toArray(BigInteger[]::new);
    return shareCombinations(shares, 0, secrets.length, new Map.Entry[secrets.length], secrets[0], positions);
  }

  private int shareCombinations(final BigInteger[] shares,
                                final int startPos,
                                final int len,
                                final Map.Entry<BigInteger, BigInteger>[] result,
                                final BigInteger expectedSecret,
                                final BigInteger[] cachedPositions) {
    if (len == 0) {
      validateReconstruction(expectedSecret, Map.ofEntries(result));
      return 1;
    }
    int numSubSets = 0;
    for (int i = startPos; i <= shares.length - len; i++) {
      final int r = result.length - len;
      result[r] = Map.entry(cachedPositions[i], shares[i]);
      numSubSets += shareCombinations(shares, i + 1, len - 1, result, expectedSecret, cachedPositions);
    }
    return numSubSets;
  }

  @Override
  public String toString() {
    return "{\"_class\":\"ShamirSharesBuilder\", " +
        "\"prime\":" + (prime == null ? "null" : prime) + ", " +
        "\"numShares\":\"" + numShares + "\"" + ", " +
        "\"numRequiredShares\":\"" + getNumRequiredShares() + "\"" + ", " +
        "\"secrets\":" + Arrays.toString(secrets) + "}";
  }
}
