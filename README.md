# secret-shamiracle [![License](http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat)](LICENSE)

Simple builder and utility functions for creating Shamir secret shares and reconstructing/interpolating the original secret from Shamir shares.

### Context

See the [Shamir secret sharing wiki](https://en.wikipedia.org/wiki/Shamir%27s_Secret_Sharing#Shamir's_secret-sharing_scheme) for background and context.

### Usage

```java
var sharesBuilder = Shamir.buildShares()
  // Set mersenne prime: https://en.wikipedia.org/wiki/Mersenne_prime#List_of_known_Mersenne_primes
  .mersennePrimeExponent(521)
  //.prime((BigInteger) prime) Or bring your own prime.
  .numRequiredShares(3)
  .numShares(5)
  .initSecrets();  // prepare 'requiredShares' coefficients
  //.initSecrets(secret); // Or supply your own secret, must be less than the configured prime.

BigInteger[] shares = sharesBuilder.createShares();
sharesBuilder.clearSecrets();

// ... reconstruct secret
var shareMap = Map.of(BigInteger.valueOf(1), shares[0],
                      BigInteger.valueOf(3), shares[2],
                      BigInteger.valueOf(5), shares[4]);
BigInteger reconstructedSecret = Shamir.reconstructSecret(shareMap, sharesBuilder.getPrime());
```
