# secret-shamiracle [![Build Status](https://travis-ci.org/comodal/secret-shamiracle.svg?branch=master)](https://travis-ci.org/comodal/secret-shamiracle)  [![Download](https://api.bintray.com/packages/comodal/libraries/shamir/images/download.svg)](https://bintray.com/comodal/libraries/shamir/_latestVersion) [![codecov](https://codecov.io/gh/comodal/secret-shamiracle/branch/master/graph/badge.svg)](https://codecov.io/gh/comodal/secret-shamiracle) [![License](http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat)](LICENSE)

> Simple builder and utility functions for creating Shamir secret shares and secret reconstruction.

### Credits
* [Shamir secret sharing wiki](https://en.wikipedia.org/wiki/Shamir%27s_Secret_Sharing#Shamir's_secret-sharing_scheme)
* Credit to Stackoverflow user [JerzySkalski](https://stackoverflow.com/users/4513021/jerzyskalski) for providing a [working example](https://stackoverflow.com/a/34365904/3754157) which limits the finite field with prime modulus division.

### Components
* [Shamir.java](./systems.comodal.shamir/src/main/java/systems/comodal/shamir/Shamir.java#L1): Minimal static methods to facilitate the creation of shares and the reconstruction of a secret.
* [ShamirSharesBuilder.java](./systems.comodal.shamir/src/main/java/systems/comodal/shamir/ShamirSharesBuilder.java#L1): A mutable builder to help coordinate the state needed to create and validate shares.

### Shares Builder Usage

* Required Shares: The minimum number of shares needed to reconstruct the free coefficient secret.
* Total Shares: The total number of shares to generate.
* Prime:
  * Must be supplied directly, or indirectly as a [Mersenne Prime](https://en.wikipedia.org/wiki/Mersenne_prime#List_of_known_Mersenne_primes) exponent.
  * Must be larger than all secrets used.
* Random: Defaults to `new SecureRandom()`
* Secret:
  * May be provided as a byte[] or a BigInteger to `initSecrets(secret)`.
  * Defaults to a random value in the range (0, prime) with a call to `initSecrets()`.

```java
var sharesBuilder = Shamir.buildShares()
  .numRequiredShares(3)
  .numShares(5)
  .mersennePrimeExponent(521)
  .initSecrets("Shamir's Secret".getBytes(UTF_8));

BigInteger[] shares = sharesBuilder.createShares();

// Validate secret reconstruction for all share combinations of size 'numRequiredShares'.
// Throws an IllegalStateException if any reconstructed secret does not equal the original.
sharesBuilder.validateShareCombinations(shares);

// Reconstruct secret.
var coordinates = Map.of(BigInteger.valueOf(1), shares[0],
                         BigInteger.valueOf(3), shares[2],
                         BigInteger.valueOf(5), shares[4]);
var secret = Shamir.reconstructSecret(coordinates, sharesBuilder.getPrime());
var secretString = new String(secret.toByteArray(), UTF_8);
```
