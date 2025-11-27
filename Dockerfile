# Multi-stage build for Eternity Solver
FROM eclipse-temurin:11-jdk-alpine AS builder

# Set working directory
WORKDIR /build

# Copy source and dependencies
COPY src/ src/
COPY lib/ lib/
COPY compile.sh .
COPY build_jar.sh .
COPY VERSION .

# Make scripts executable
RUN chmod +x compile.sh build_jar.sh

# Build JAR
RUN ./build_jar.sh

# Runtime stage - minimal image
FROM eclipse-temurin:11-jre-alpine

# Metadata
LABEL maintainer="Laurent Zamofing"
LABEL description="Eternity Solver - High-performance edge-matching puzzle solver"
LABEL version="1.0.0"

# Set working directory
WORKDIR /app

# Copy JAR from builder
COPY --from=builder /build/eternity-solver-1.0.0.jar ./eternity-solver.jar

# Copy puzzle data
COPY data/ data/

# Create volume for saves
VOLUME /app/saves

# Set entry point
ENTRYPOINT ["java", "-jar", "eternity-solver.jar"]

# Default command (show help)
CMD ["--help"]

# Usage examples:
# Build:   docker build -t eternity-solver:1.0.0 .
# Help:    docker run --rm eternity-solver:1.0.0 --help
# Solve:   docker run --rm eternity-solver:1.0.0 example_3x3
# Verbose: docker run --rm eternity-solver:1.0.0 -v -p example_4x4
# Save:    docker run --rm -v $(pwd)/saves:/app/saves eternity-solver:1.0.0 example_3x3
