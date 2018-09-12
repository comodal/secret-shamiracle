package systems.comodal.shamir;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

import static systems.comodal.shamir.Shamir.createSecret;

public final class ShamirSharesBuilder {

  private SecureRandom secureRandom;
  private BigInteger prime;
  private int numShares;
  private int numRequiredShares;
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
    return numRequiredShares;
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
    // 13th Mersenne Prime = 521
    this.prime = BigInteger.ONE.shiftLeft(mersennePrimeExponent).subtract(BigInteger.ONE);
    return this;
  }

  public ShamirSharesBuilder numShares(final int numShares) {
    this.numShares = numShares;
    return this;
  }

  public ShamirSharesBuilder numRequiredShares(final int numRequiredShares) {
    this.numRequiredShares = numRequiredShares;
    return this;
  }

  public ShamirSharesBuilder initSecrets(final BigInteger secret) {
    if (secret.compareTo(prime) >= 0) {
      throw new IllegalArgumentException("Secret must be less than the prime " + prime);
    }
    if (secureRandom == null) {
      this.secureRandom = new SecureRandom();
    }
    if (secrets == null || secrets.length != numRequiredShares) {
      secrets = new BigInteger[numRequiredShares];
    }
    secrets[0] = secret;
    for (int i = 1; i < numRequiredShares; i++) {
      secrets[i] = createSecret(secureRandom, prime);
    }
    return this;
  }

  public ShamirSharesBuilder initSecrets() {
    if (secureRandom == null) {
      this.secureRandom = new SecureRandom();
    }
    if (secrets == null || secrets.length != numRequiredShares) {
      secrets = new BigInteger[numRequiredShares];
    }
    secrets[0] = createSecret(secureRandom, prime);
    for (int i = 1; i < numRequiredShares; i++) {
      secrets[i] = createSecret(secureRandom, prime);
    }
    return this;
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

  @Override
  public String toString() {
    return "{\"_class\":\"ShamirSharesBuilder\", " +
        "\"prime\":" + (prime == null ? "null" : prime) + ", " +
        "\"numShares\":\"" + numShares + "\"" + ", " +
        "\"numRequiredShares\":\"" + numRequiredShares + "\"" + ", " +
        "\"secrets\":" + Arrays.toString(secrets) + "}";
  }
}
