/**
 * Experimental Dancing Links (DLX) solver.
 *
 * <p>POC package — not wired to production. See {@code README.md} in this
 * directory for the exact-cover modelling, go/no-go criteria, and the
 * step-by-step implementation plan.</p>
 *
 * <p>Classes in this package are kept self-contained: they depend only on
 * {@link solver.Solver}, {@link model.Board}, {@link model.Piece}, and
 * the JDK — never on the production solver internals. That lets the
 * experiment be deleted cleanly if benchmarks rule it out.</p>
 */
package solver.experimental.dlx;
