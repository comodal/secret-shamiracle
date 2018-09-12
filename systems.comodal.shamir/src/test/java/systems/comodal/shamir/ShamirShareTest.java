package systems.comodal.shamir;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static systems.comodal.shamir.Shamir.reconstructSecret;

final class ShamirShareTest {

  @Test
  void testShareCreationAndReconstruction() {
    final var sharesBuilder = Shamir.buildShares().marsennePrimeExponent(521);
    for (int numShares = 64; numShares >= 63; numShares--) {
      sharesBuilder.numShares(numShares);
      for (int requiredShares = 1; requiredShares <= numShares; requiredShares++) {
        sharesBuilder
            .numRequiredShares(requiredShares)
            .initSecrets();
        validateReconstruction(sharesBuilder);
      }
    }
  }

  private void validateReconstruction(final ShamirSharesBuilder sharesBuilder) {
    final var shares = sharesBuilder.createShares();

    final var shareMap = new HashMap<BigInteger, BigInteger>(sharesBuilder.getNumRequiredShares());
    ThreadLocalRandom.current().ints(0, sharesBuilder.getNumShares())
        .distinct()
        .limit(sharesBuilder.getNumRequiredShares())
        .forEach(i -> shareMap.put(BigInteger.valueOf(i + 1), shares[i]));

    final var reconstructedSecret = reconstructSecret(shareMap, sharesBuilder.getPrime());
    assertEquals(sharesBuilder.getSecret(), reconstructedSecret, sharesBuilder::toString);

    assertNull(sharesBuilder.clearSecrets().getSecret());
  }
}
