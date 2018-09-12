package systems.comodal.shamir;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class ShamirShareTest {

  @Test
  void testShareCreationAndReconstruction() {
    final var sharesBuilder = Shamir.buildShares()
        .mersennePrimeExponent(521);
    sharesBuilder.validatePrime();

    for (int numShares = 64; numShares >= 63; numShares--) {
      sharesBuilder.numShares(numShares);
      for (int requiredShares = 1; requiredShares <= numShares; requiredShares++) {
        sharesBuilder
            .numRequiredShares(requiredShares)
            .initSecrets();

        final var shares = sharesBuilder.createShares();

        final var shareMap = new HashMap<BigInteger, BigInteger>(sharesBuilder.getNumRequiredShares());
        ThreadLocalRandom.current().ints(0, sharesBuilder.getNumShares())
            .distinct()
            .limit(sharesBuilder.getNumRequiredShares())
            .forEach(i -> shareMap.put(BigInteger.valueOf(i + 1), shares[i]));

        final var reconstructedSecret = Shamir.reconstructSecret(shareMap, sharesBuilder.getPrime());
        assertEquals(sharesBuilder.getSecret(), reconstructedSecret, sharesBuilder::toString);

        assertNull(sharesBuilder.clearSecrets().getSecret());
      }
    }
  }

  @Test
  void testShareSuperSet() {
    final var sharesBuilder = Shamir.buildShares()
        .mersennePrimeExponent(521)
        .numRequiredShares(5)
        .numShares(10)
        .initSecrets();

    final var shares = sharesBuilder.createShares();
    // n!  / (r! * (n  - r)!)
    // 10! / (5! * (10 - 5)!)
    assertEquals(252, sharesBuilder.validateShareCombinations(shares));
  }
}
