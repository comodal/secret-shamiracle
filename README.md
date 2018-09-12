# secret-shamiracle [![Build Status](https://travis-ci.org/comodal/secret-shamiracle.svg?branch=master)](https://travis-ci.org/comodal/secret-shamiracle)  [![License](http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat)](LICENSE)

Simple builder and utility functions for creating Shamir secret shares and reconstructing/interpolating the original secret from Shamir shares.

### Context

See the [Shamir secret sharing wiki](https://en.wikipedia.org/wiki/Shamir%27s_Secret_Sharing#Shamir's_secret-sharing_scheme) for background and context.

### Usage

#### Required Fields

* Prime:
  * Defines the finite field used.
  * Must be larger than all secrets used.
  * Can be supplied directly or indirectly as a [Mersenne Prime](https://en.wikipedia.org/wiki/Mersenne_prime#List_of_known_Mersenne_primes) exponent.
* Required Shares: The minimum number of shares needed to reconstruct the free coefficent secret.
* Total Shares: The total number of shares to generate.

#### Optional Fields

* SecureRandom: Defaults to `new SecureRandom()`
* Secret:
  * The underlying secret may be provided as a BigInteger to `initSecrets(secret)`.
  * Or may be generated with a call to `initSecrets()`.

```java
var sharesBuilder = Shamir.buildShares()
  .mersennePrimeExponent(521)
  .numRequiredShares(3)
  .numShares(5)
  .initSecrets();

// Convenience method that calls prime.isProbablePrime(Integer.MAX_VALUE).
// Throws an IllegalStateException if false.
sharesBuilder.validatePrime();

// Shares are provided as an Array of BigIntegers to the user.
// Each array value and its index is a coordinate in the system.
// The array index correspondes to the x-axis position minus one.  Each value is the y-axis value.
var shares = sharesBuilder.createShares();

// Validate secret reconstruction for all combinations of shares.
sharesBuilder.validateShareCombinations(shares);

// Free references to BigInteger secrets... Data may continue to exist in system memory.
sharesBuilder.clearSecrets();

// ... reconstruct secret
var shareMap = Map.of(BigInteger.valueOf(1), shares[0],
                      BigInteger.valueOf(3), shares[2],
                      BigInteger.valueOf(5), shares[4]);
BigInteger secret = Shamir.reconstructSecret(shareMap, sharesBuilder.getPrime());
```
