# secret-shamiracle [![License](http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat)](LICENSE)

## Usage

```java
var sharesBuilder = Shamir.buildShares()
  // Set marsenne prime: https://en.wikipedia.org/wiki/Mersenne_prime#List_of_known_Mersenne_primes
  .marsennePrimeExponent(521)
  //.prime(prime) Or bring your own prime.
  .numShares(4)
  .numRequiredShares(2)
  .initSecrets();  // prepare 'requiredShares' coefficients
  //.initSecrets(secret); // Or supply your own secret, must be less than the configured prime.

BigInteger[] shares = sharesBuilder.createShares();
sharesBuilder.clearSecrets();

// ... reconstruct secret
var shareMap = Map.of(BigInteger.ONE, shares[0],
                      BigInteger.valueOf(3), shares[2]);
BigInteger reconstructedSecret = reconstructSecret(shareMap, sharesBuilder.getPrime());
```
