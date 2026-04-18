import model.Board;
import model.Piece;
import solver.EternitySolver;
import util.PuzzleGenerator;
import java.util.HashMap;
import java.util.Map;

public class ParallelBench {
  public static void main(String[] args) {
    for (long seed : new long[]{1L, 17L}) {
      Map<Integer, Piece> pieces = PuzzleGenerator.generate(6, 5, seed);

      // Sequential
      Board b1 = new Board(6, 6);
      EternitySolver s1 = new EternitySolver();
      s1.setVerbose(false); s1.setMaxExecutionTime(90_000);
      s1.setSymmetryBreakingFlags(false, false);
      long t = System.currentTimeMillis();
      boolean ok1 = s1.solve(b1, new HashMap<>(pieces));
      System.out.println("seed=" + seed + " SEQ: " + ok1 + " in " + (System.currentTimeMillis()-t) + "ms");

      // Parallel 4 threads
      Board b2 = new Board(6, 6);
      EternitySolver s2 = new EternitySolver();
      s2.setVerbose(false); s2.setMaxExecutionTime(90_000);
      s2.setSymmetryBreakingFlags(false, false);
      t = System.currentTimeMillis();
      boolean ok2 = s2.solveParallel(b2, new HashMap<>(pieces), new HashMap<>(pieces), 4);
      System.out.println("seed=" + seed + " PAR4: " + ok2 + " in " + (System.currentTimeMillis()-t) + "ms");
    }
  }
}
