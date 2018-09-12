package systems.comodal.shamir;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class ShamirShareTest {

  @Test
  void testShareCreationAndReconstruction() {
    final var sharesBuilder = Shamir.buildShares()
        .mersennePrimeExponent(521);
    sharesBuilder.validatePrime();

    int numShares = 64;
    final var sharePositions = IntStream.rangeClosed(1, numShares).mapToObj(BigInteger::valueOf).toArray(BigInteger[]::new);
    final var shareMap = new HashMap<BigInteger, BigInteger>(numShares);

    for (; numShares >= 63; numShares--) {
      sharesBuilder.numShares(numShares);
      for (int requiredShares = 1; requiredShares <= numShares; requiredShares++) {
        sharesBuilder
            .numRequiredShares(requiredShares)
            .initSecrets();

        final var shares = sharesBuilder.createShares();

        ThreadLocalRandom.current().ints(0, sharesBuilder.getNumShares())
            .distinct()
            .limit(sharesBuilder.getNumRequiredShares())
            .forEach(i -> shareMap.put(sharePositions[i], shares[i]));

        final var reconstructedSecret = Shamir.reconstructSecret(shareMap, sharesBuilder.getPrime());
        assertEquals(sharesBuilder.getSecret(), reconstructedSecret, sharesBuilder::toString);
        shareMap.clear();

        assertNull(sharesBuilder.clearSecrets().getSecret());
      }
    }
  }

  @Test
  void testShareCombinations() {
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
