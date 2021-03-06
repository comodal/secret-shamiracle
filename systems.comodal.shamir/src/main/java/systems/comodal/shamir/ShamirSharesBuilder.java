package systems.comodal.shamir;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

import static systems.comodal.shamir.Shamir.createMersennePrimeFromExponent;
import static systems.comodal.shamir.Shamir.createSecret;

public final class ShamirSharesBuilder {

  private Random secureRandom;
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
    return secrets == null || secrets.length == 0 ? null : secrets[0];
  }

  public ShamirSharesBuilder secureRandom(final Random secureRandom) {
    this.secureRandom = secureRandom;
    return this;
  }

  public ShamirSharesBuilder prime(final BigInteger prime) {
    this.prime = prime;
    return this;
  }

  public ShamirSharesBuilder mersennePrimeExponent(final int mersennePrimeExponent) {
    this.prime = createMersennePrimeFromExponent(mersennePrimeExponent);
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
      throw new IllegalStateException("This is not a prime number: " + prime);
    }
  }

  public ShamirSharesBuilder numShares(final int numShares) {
    this.numShares = numShares;
    return this;
  }

  public ShamirSharesBuilder numRequiredShares(final int numRequiredShares) {
    if (secrets == null) {
      this.secrets = new BigInteger[numRequiredShares];
      return this;
    }
    if (secrets.length != numRequiredShares) {
      final var newSecretsArray = new BigInteger[numRequiredShares];
      System.arraycopy(secrets, 0, newSecretsArray, 0, Math.min(secrets.length, numRequiredShares));
      this.secrets = newSecretsArray;
    }
    return this;
  }

  public ShamirSharesBuilder initSecrets(final byte[] secretBytes) {
    return initSecrets(new BigInteger(1, secretBytes));
  }

  public ShamirSharesBuilder initSecrets(final BigInteger secret) {
    Objects.requireNonNull(prime, "Prime must be set.");
    if (secret.compareTo(BigInteger.ZERO) <= 0 || secret.compareTo(prime) >= 0) {
      throw new IllegalArgumentException("Secret must be greater than 0 and less than the prime " + prime);
    }
    validateNumRequiredShares();
    initSecureRandom();
    initSecretsUnchecked(secret);
    return this;
  }

  public ShamirSharesBuilder initSecrets() {
    Objects.requireNonNull(prime, "Prime must be set.");
    validateNumRequiredShares();
    initSecureRandom();
    initSecretsUnchecked(createSecret(secureRandom, prime));
    return this;
  }

  private void validateNumRequiredShares() {
    if (secrets == null || secrets.length == 0) {
      throw new IllegalStateException("Num required shares must be set and greater than 0.");
    }
  }

  private void initSecureRandom() {
    if (secureRandom == null) {
      this.secureRandom = new SecureRandom();
    }
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
    return Shamir.createShares(prime, secrets, validateNumShares());
  }

  private int validateNumShares() {
    if (numShares > 0) {
      return numShares;
    }
    throw new IllegalStateException("Num shares must be set and greater than 0.");
  }

  @SuppressWarnings("unchecked")
  public void validateShareCombinations(final BigInteger[] shares) {
    Shamir.validateShareCombinations(secrets[0], prime, secrets.length, shares);
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
